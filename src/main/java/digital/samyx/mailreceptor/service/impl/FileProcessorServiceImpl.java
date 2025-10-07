package digital.samyx.mailreceptor.service.impl;

import digital.samyx.mailreceptor.dto.EmailAttachmentDTO;
import digital.samyx.mailreceptor.service.IFileProcessorService;
import digital.samyx.mailreceptor.util.UnzipFiles;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Slf4j
public class FileProcessorServiceImpl implements IFileProcessorService {

    private static final Set<String> VALID_EXTENSIONS = new HashSet<>(Arrays.asList("xml", "pdf", "zip"));
    private static final Set<String> IGNORED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "doc", "docx", "xls", "xlsx", "txt", "csv"
    ));

    @Override
    public Map<String, List<EmailAttachmentDTO>> processEmailAttachments(Message message, String saveDirectory) throws Exception {
        Map<String, List<EmailAttachmentDTO>> attachmentsByType = new HashMap<>();
        attachmentsByType.put("xml", new ArrayList<>());
        attachmentsByType.put("pdf", new ArrayList<>());
        attachmentsByType.put("zip", new ArrayList<>());

        String prefix = System.currentTimeMillis() + "-";

        // Procesar el contenido del mensaje recursivamente
        processMessageContent(message.getContent(), saveDirectory, prefix, attachmentsByType);

        log.info("Resumen antes de procesar ZIPs - XMLs: {}, PDFs: {}, ZIPs: {}",
                attachmentsByType.get("xml").size(),
                attachmentsByType.get("pdf").size(),
                attachmentsByType.get("zip").size());

        // Procesar archivos ZIP
        List<EmailAttachmentDTO> zips = new ArrayList<>(attachmentsByType.get("zip"));
        for (EmailAttachmentDTO zipAttachment : zips) {
            processZipFile(zipAttachment, saveDirectory, attachmentsByType);
        }

        // Asociar PDFs con XMLs
        asociarPDFsConXMLs(attachmentsByType.get("xml"), attachmentsByType.get("pdf"));

        return attachmentsByType;
    }

    private void processMessageContent(Object content, String saveDirectory, String prefix,
                                       Map<String, List<EmailAttachmentDTO>> attachmentsByType) throws Exception {
        if (content instanceof Multipart) {
            Multipart multiPart = (Multipart) content;
            int numberOfParts = multiPart.getCount();
            log.debug("Procesando Multipart con {} partes", numberOfParts);

            for (int i = 0; i < numberOfParts; i++) {
                Part part = multiPart.getBodyPart(i);
                processPart(part, saveDirectory, prefix, attachmentsByType);
            }
        } else if (content instanceof Part) {
            processPart((Part) content, saveDirectory, prefix, attachmentsByType);
        }
    }

    private void processPart(Part part, String saveDirectory, String prefix,
                             Map<String, List<EmailAttachmentDTO>> attachmentsByType) throws Exception {
        String disposition = part.getDisposition();
        String fileName = part.getFileName();
        String contentType = part.getContentType();

        log.debug("Procesando parte - Disposition: {}, FileName: {}, ContentType: {}",
                disposition, fileName, contentType);

        // Si esta parte es multipart, procesarla recursivamente
        if (contentType.toLowerCase().contains("multipart")) {
            log.debug("Parte es multipart, procesando recursivamente...");
            processMessageContent(part.getContent(), saveDirectory, prefix, attachmentsByType);
            return;
        }

        // Verificar si es un archivo adjunto
        boolean isAttachment =
                "attachment".equalsIgnoreCase(disposition) ||
                        ("inline".equalsIgnoreCase(disposition) && fileName != null) ||
                        (fileName != null && !fileName.isEmpty()) ||
                        (contentType != null &&
                                (contentType.toLowerCase().contains("application/xml") ||
                                        contentType.toLowerCase().contains("application/pdf") ||
                                        contentType.toLowerCase().contains("application/zip") ||
                                        contentType.toLowerCase().contains("application/octet-stream")));

        if (isAttachment && part instanceof MimeBodyPart) {
            log.debug("Parte identificada como archivo adjunto");
            processAttachment((MimeBodyPart) part, saveDirectory, prefix, attachmentsByType);
        }
    }

    private void processAttachment(MimeBodyPart part, String saveDirectory, String prefix,
                                   Map<String, List<EmailAttachmentDTO>> attachmentsByType) throws Exception {
        String fileName = getFileName(part);

        log.debug("Procesando adjunto - FileName: {}", fileName);

        if (fileName == null || fileName.isEmpty()) {
            // Si no hay nombre de archivo, intentar generarlo basado en el content-type
            String contentType = part.getContentType().toLowerCase();
            if (contentType.contains("xml")) {
                fileName = "document.xml";
            } else if (contentType.contains("pdf")) {
                fileName = "document.pdf";
            } else {
                log.debug("No se pudo determinar el nombre del archivo, saltando...");
                return;
            }
        }

        String extension = getFileExtension(fileName).toLowerCase();
        log.debug("Archivo: {} - Extensión detectada: {}", fileName, extension);

        // Ignorar archivos no relevantes
        if (IGNORED_EXTENSIONS.contains(extension)) {
            log.info("Ignorando archivo con extensión no válida: {}", fileName);
            return;
        }

        if (!VALID_EXTENSIONS.contains(extension)) {
            log.warn("Archivo con extensión desconocida: {}", fileName);
            return;
        }

        // Generar nombre único
        String uniqueFileName = prefix + UUID.randomUUID().toString() + "." + extension;
        String filePath = saveDirectory + File.separator + uniqueFileName;

        try {
            // Crear directorio si no existe
            File dir = new File(saveDirectory);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            part.saveFile(filePath);

            EmailAttachmentDTO attachment = new EmailAttachmentDTO();
            attachment.setFileName(uniqueFileName);
            attachment.setFilePath(filePath);
            attachment.setExtension(extension);

            attachmentsByType.get(extension).add(attachment);
            log.info("Archivo guardado: {} como {}", fileName, uniqueFileName);
        } catch (Exception e) {
            log.error("Error guardando archivo {}: ", fileName, e);
            throw e;
        }
    }

    private void processZipFile(EmailAttachmentDTO zipAttachment, String saveDirectory,
                                Map<String, List<EmailAttachmentDTO>> attachmentsByType) throws Exception {
        log.info("Procesando archivo ZIP: {}", zipAttachment.getFileName());

        List<File> extractedFiles = unzipFile(zipAttachment.getFilePath(), saveDirectory);

        for (File file : extractedFiles) {
            String extension = getFileExtension(file.getName()).toLowerCase();

            if (IGNORED_EXTENSIONS.contains(extension)) {
                log.info("Ignorando archivo extraído con extensión no válida: {}", file.getName());
                file.delete(); // Limpiar archivo no necesario
                continue;
            }

            if (VALID_EXTENSIONS.contains(extension) && !extension.equals("zip")) {
                EmailAttachmentDTO attachment = new EmailAttachmentDTO();
                attachment.setFileName(file.getName());
                attachment.setFilePath(file.getAbsolutePath());
                attachment.setExtension(extension);

                attachmentsByType.get(extension).add(attachment);
                log.info("Archivo extraído del ZIP: {}", file.getName());
            }
        }
    }

    @Override
    public void asociarPDFsConXMLs(List<EmailAttachmentDTO> xmlFiles, List<EmailAttachmentDTO> pdfFiles) {
        // Por ahora asociamos por orden, pero podríamos mejorar esto
        // comparando nombres de archivo o extrayendo la clave del PDF si es posible

        if (xmlFiles.size() == 1 && pdfFiles.size() == 1) {
            // Caso simple: un XML y un PDF
            log.info("Asociación simple: 1 XML con 1 PDF");
        } else if (xmlFiles.size() > 0 && pdfFiles.size() > 0) {
            // Casos múltiples: intentar asociar por nombres similares
            log.info("Múltiples archivos detectados - XMLs: {}, PDFs: {}", xmlFiles.size(), pdfFiles.size());
            // TODO: Implementar lógica de asociación más inteligente
        }
    }

    @Override
    public String saveAttachment(Part part, String saveDirectory, String prefix) throws Exception {
        String fileName = getFileName(part);
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        String extension = getFileExtension(fileName);
        String newFileName = prefix + "sinmata" + "." + extension;
        String filePath = saveDirectory + File.separator + newFileName;

        if (part instanceof MimeBodyPart) {
            ((MimeBodyPart) part).saveFile(filePath);
        }

        return filePath;
    }

    @Override
    public List<File> unzipFile(String zipFilePath, String destDirectory) throws Exception {
        List<File> extractedFiles = new ArrayList<>();

        UnzipFiles unzipper = new UnzipFiles();
        unzipper.unzip(zipFilePath, destDirectory);

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    extractedFiles.add(new File(destDirectory + entry.getName()));
                }
            }
        }

        return extractedFiles;
    }

    @Override
    public String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        return (lastIndex > 0) ? fileName.substring(lastIndex + 1).toLowerCase() : "";
    }

    @Override
    public boolean isValidFileType(String extension) {
        return VALID_EXTENSIONS.contains(extension.toLowerCase());
    }

    private String getFileName(Part part) throws Exception {
        if (!(part instanceof MimeBodyPart)) {
            return part.getFileName();
        }

        String contentType = part.getContentType();
        String fileName = null;

        try {
            fileName = MimeUtility.decodeText(part.getFileName());
            if (fileName == null) {
                fileName = getDefaultFileName(contentType);
            }
        } catch (NullPointerException ex) {
            fileName = getDefaultFileName(contentType);
        }

        return (fileName == null) ? "" : fileName;
    }

    private String getDefaultFileName(String contentType) {
        if (contentType.contains("application/xml") || contentType.contains("APPLICATION/XML")) {
            return "sinmata.xml";
        }
        if (contentType.contains("application/pdf") || contentType.contains("APPLICATION/PDF")) {
            return "sinmata.pdf";
        }
        return null;
    }
}