package digital.samyx.mailreceptor.component;

import digital.samyx.mailreceptor.entity.SucursalReceptorSmtp;
import digital.samyx.mailreceptor.service.*;
import jakarta.mail.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

@Component
@Slf4j
public class MensajeReceptorAutomatico {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${nathbit.api.url}")
    private String nathbitApiUrl;

    @Value("${nathbit.api.api-key}")
    private String nathbitApiKey;

    @Autowired
    private SucursalSmtp emisoresSMTPService;

    @Autowired
    private IEmailService emailService;

    @Scheduled(fixedDelay = 600000L) // 10 minutos
    public void procesarCorreos() {
        List<SucursalReceptorSmtp> emisores = emisoresSMTPService.findAllActivos();

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

        List<Message> messages = emailService.getUnreadMessages(email, password, host);
        log.info("Encontrados {} mensajes no leídos para {}", messages.size(), email);

        for (Message message : messages) {
            try {
                processMessage(message, emisor);
                emailService.markMessageAsRead(message);
            } catch (Exception e) {
                log.error("Error procesando mensaje: ", e);
            }
        }
    }

    private void processMessage(Message message, SucursalReceptorSmtp emisor) throws Exception {
        String emailFrom = extractEmailAddress(message);
        log.info("Procesando mensaje de: {}", emailFrom);

        // Extraer adjuntos EN MEMORIA
        Map<String, byte[]> xmlFiles = new HashMap<>();
        Map<String, byte[]> pdfFiles = new HashMap<>();

        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    String fileName = bodyPart.getFileName();

                    if (fileName != null) {
                        fileName = fileName.toLowerCase();
                        byte[] fileBytes = leerBytes(bodyPart.getInputStream());

                        if (fileName.endsWith(".xml")) {
                            xmlFiles.put(fileName, fileBytes);
                        } else if (fileName.endsWith(".pdf")) {
                            pdfFiles.put(fileName, fileBytes);
                        } else if (fileName.endsWith(".zip")) {
                            procesarZipEnMemoria(fileBytes, xmlFiles, pdfFiles);
                        }
                    }
                }
            }
        }

        // Procesar XMLs
        for (Map.Entry<String, byte[]> entry : xmlFiles.entrySet()) {
            String xmlFileName = entry.getKey();
            byte[] xmlBytes = entry.getValue();

            try {
                String clave = extraerClave(xmlBytes);

                // ✅ Si es null (MensajeHacienda), skipear
                if (clave == null) {
                    log.info("⏭️ Skipeando archivo: {}", xmlFileName);
                    continue;
                }

                byte[] pdfBytes = buscarPdfPorClave(clave, pdfFiles);

                enviarANathBit(xmlBytes, pdfBytes, clave, emisor);

            } catch (Exception e) {
                log.error("Error procesando XML {}: {}", xmlFileName, e.getMessage());
            }
        }
    }

    private void procesarZipEnMemoria(byte[] zipBytes,
                                      Map<String, byte[]> xmlFiles,
                                      Map<String, byte[]> pdfFiles) throws Exception {

        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bais)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                byte[] fileBytes = leerBytes(zis);

                if (name.endsWith(".xml")) {
                    xmlFiles.put(name, fileBytes);
                } else if (name.endsWith(".pdf")) {
                    pdfFiles.put(name, fileBytes);
                }

                zis.closeEntry();
            }
        }
    }

    private void enviarANathBit(byte[] xmlBytes, byte[] pdfBytes,
                                String clave, SucursalReceptorSmtp config) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            body.add("xmlFile", new ByteArrayResource(xmlBytes) {
                @Override
                public String getFilename() {
                    return clave + ".xml";
                }
            });

            if (pdfBytes != null) {
                body.add("pdfFile", new ByteArrayResource(pdfBytes) {
                    @Override
                    public String getFilename() {
                        return clave + ".pdf";
                    }
                });
            }

            body.add("empresaId", config.getEmpresaId());
            body.add("sucursalId", config.getSucursalId());
            body.add("crearProveedorSiNoExiste", true);
            body.add("aceptarAutomaticamente", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-API-Key", nathbitApiKey);  // ← AGREGAR API KEY

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            String url = nathbitApiUrl + "/api/facturas-recepcion/procesar-email";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Factura {} procesada por NathBit", clave);
            }

        } catch (Exception e) {
            log.error("❌ Error enviando factura {}: {}", clave, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private byte[] leerBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private String extraerClave(byte[] xmlBytes) {
        String xml = new String(xmlBytes, StandardCharsets.UTF_8);

        // ✅ DETECTAR tipos de documentos NO VÁLIDOS - Skipear
        if (xml.contains("<MensajeHacienda") ||
                xml.contains("<MensajeReceptor") ||
                xml.contains("<ConfirmacionComprobante")) {
            log.info("⏭️ Documento de sistema detectado - skipeando");
            return null;
        }

        // ✅ VALIDAR que sea un documento de venta válido
        boolean esDocumentoValido = xml.contains("<FacturaElectronica") ||
                xml.contains("<NotaCreditoElectronica") ||
                xml.contains("<NotaDebitoElectronica") ||
                xml.contains("<TiqueteElectronico") ||
                xml.contains("<FacturaElectronicaCompra") ||
                xml.contains("<FacturaElectronicaExportacion");

        if (!esDocumentoValido) {
            log.info("⏭️ Tipo de documento no válido - skipeando");
            return null;
        }

        // Extraer clave
        int start = xml.indexOf("<Clave>") + 7;
        int end = xml.indexOf("</Clave>");
        if (start > 6 && end > start) {
            return xml.substring(start, end);
        }

        log.warn("⚠️ No se encontró clave en el XML");
        return null;
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

    private byte[] buscarPdfPorClave(String clave, Map<String, byte[]> pdfFiles) {
        if (pdfFiles.isEmpty()) {
            return null;
        }

        // Extraer consecutivo de la clave (últimos 20 dígitos)
        String consecutivo = clave.length() >= 20 ? clave.substring(21, 41) : "";

        // Buscar PDF que contenga la clave o el consecutivo
        for (Map.Entry<String, byte[]> pdfEntry : pdfFiles.entrySet()) {
            String pdfName = pdfEntry.getKey().toLowerCase();

            // Buscar por clave completa o consecutivo
            if (pdfName.contains(clave.toLowerCase()) ||
                    (!consecutivo.isEmpty() && pdfName.contains(consecutivo))) {
                log.info("📄 PDF encontrado: {} para clave {}", pdfEntry.getKey(), clave);
                return pdfEntry.getValue();
            }
        }

        // Si no encuentra específico, tomar el primero (por si viene solo 1 XML + 1 PDF)
        if (pdfFiles.size() == 1) {
            log.info("📄 Usando único PDF disponible para clave {}", clave);
            return pdfFiles.values().stream().findFirst().orElse(null);
        }

        return null;
    }
}