package digital.samyx.mailreceptor.component;

import digital.samyx.mailreceptor.dto.DocumentoElectronicoDTO;
import digital.samyx.mailreceptor.dto.EmailAttachmentDTO;
import digital.samyx.mailreceptor.entity.SucursalReceptorSmtp;
import digital.samyx.mailreceptor.service.*;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MensajeReceptorAutomatico {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${nathbit.api.url}")
    private String nathbitApiUrl;

    @Value("${nathbit.api.api-key}")
    private String nathbitApiKey;

    @Value("${path.upload.files.api}")
    private String pathUploadFilesApi;

    @Autowired
    private SucursalSmtp emisoresSMTPService;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IFileProcessorService fileProcessorService;

    @Autowired
    private IFacturaParserService facturaParserService;

    @Autowired
    private INotificationService notificationService;

    @Scheduled(fixedDelay = 600000L) // 10 minutos
    public void procesarCorreos() {
        List<SucursalReceptorSmtp> emisores = emisoresSMTPService.findAll();

        for (SucursalReceptorSmtp emisor : emisores) {
            try {
                log.info("Procesando correos para: {}", emisor.getEmail());
                downloadEmailAttachments(emisor.getEmail(), emisor.getSmtpPassword(),
                        emisor.getEmailDomain(), emisor);
            } catch (Exception e) {
                log.error("Error procesando emisor {}: ", emisor.getEmail(), e);
            }
        }
    }

    public void downloadEmailAttachments(String email, String password, String host,
                                         SucursalReceptorSmtp emisor) {
        String saveDirectory = pathUploadFilesApi + "/mr-automatico";

        List<Message> messages = emailService.getUnreadMessages(email, password, host);
        log.info("Encontrados {} mensajes no leídos para {}", messages.size(), email);

        for (Message message : messages) {
            try {
                processMessage(message, saveDirectory, emisor);
                emailService.markMessageAsRead(message);
            } catch (Exception e) {
                log.error("Error procesando mensaje: ", e);
            }
        }
    }

    private void processMessage(Message message, String saveDirectory,
                                SucursalReceptorSmtp emisor) throws Exception {
        String emailFrom = extractEmailAddress(message);
        log.info("Procesando mensaje de: {}", emailFrom);

        // Procesar todos los archivos adjuntos
        Map<String, List<EmailAttachmentDTO>> attachmentsByType =
                fileProcessorService.processEmailAttachments(message, saveDirectory);

        List<EmailAttachmentDTO> xmlFiles = attachmentsByType.get("xml");
        List<EmailAttachmentDTO> pdfFiles = attachmentsByType.get("pdf");

        log.info("Archivos encontrados - XMLs: {}, PDFs: {}, ZIPs: {}",
                xmlFiles.size(), pdfFiles.size(), attachmentsByType.get("zip").size());

        if (xmlFiles.isEmpty()) {
            log.info("No se encontraron archivos XML válidos en el mensaje");
            return;
        }

        // Convertir EmailAttachmentDTO a rutas de archivo
        List<String> xmlPaths = xmlFiles.stream()
                .map(EmailAttachmentDTO::getFilePath)
                .collect(Collectors.toList());

        // Procesar todos los XMLs
        List<DocumentoElectronicoDTO> documentos = facturaParserService.procesarArchivosXml(xmlPaths);

        log.info("Documentos electrónicos válidos encontrados: {}", documentos.size());

        // Guardar cada documento
        for (DocumentoElectronicoDTO documento : documentos) {
            try {
                // Buscar PDF asociado
                String pdfFileName = "";
                if (!pdfFiles.isEmpty()) {
                    pdfFileName = pdfFiles.get(0).getFileName();
                }

                guardarDocumento(documento, emisor, pdfFileName);

            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                    log.info("Documento duplicado detectado: {}", documento.getClaveDocumento());
                    try {
                        notificationService.sendDuplicateNotification(
                                documento.getClaveDocumento(),
                                documento.getDatosFactura().getEmisorFactura(),
                                documento.getDatosFactura().getFechaEmision(),
                                documento.getDatosFactura().getTotalComprobante(),
                                emailFrom
                        );
                    } catch (Exception ex) {
                        log.error("Error enviando notificación: ", ex);
                    }
                } else {
                    log.error("Error guardando documento: ", e);
                }
            }
        }
    }

    private void guardarDocumento(DocumentoElectronicoDTO documento,
                                  SucursalReceptorSmtp config,
                                  String pdfFileName) {
        try {
            // Leer bytes del XML
            byte[] xmlBytes = Files.readAllBytes(Paths.get(documento.getXmlFileName()));

            // Leer bytes del PDF si existe
            byte[] pdfBytes = null;
            if (pdfFileName != null && !pdfFileName.isEmpty()) {
                String pdfPath = pathUploadFilesApi + "/mr-automatico/" + pdfFileName;
                File pdfFile = new File(pdfPath);
                if (pdfFile.exists()) {
                    pdfBytes = Files.readAllBytes(pdfFile.toPath());
                }
            }

            String clave = documento.getClaveDocumento();

            // Preparar request multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // XML
            body.add("xmlFile", new ByteArrayResource(xmlBytes) {
                @Override
                public String getFilename() {
                    return clave + ".xml";
                }
            });

            // PDF opcional
            if (pdfBytes != null) {
                body.add("pdfFile", new ByteArrayResource(pdfBytes) {
                    @Override
                    public String getFilename() {
                        return clave + ".pdf";
                    }
                });
            }

            // Parámetros desde config
            body.add("empresaId", config.getEmpresa().getId());
            body.add("sucursalId", config.getSucursal().getId());
            body.add("crearProveedorSiNoExiste", true);
            body.add("aceptarAutomaticamente", true);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-API-Key", nathbitApiKey);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            // Llamar a NathBit
            String url = nathbitApiUrl + "/api/facturas-recepcion/subir-xml";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Factura {} procesada por NathBit", clave);
            } else {
                throw new RuntimeException("Error: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error enviando factura a NathBit: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String extractEmailAddress(Message message) throws Exception {
        Address[] fromAddress = message.getFrom();
        String from = fromAddress[0].toString();

        Pattern pattern = Pattern.compile("<(.*?)>");
        Matcher matcher = pattern.matcher(from);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return from;
    }
}