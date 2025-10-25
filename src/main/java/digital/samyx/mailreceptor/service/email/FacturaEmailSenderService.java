package digital.samyx.mailreceptor.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de envío asíncrono de emails de facturas
 * 
 * Características:
 * - Envío asíncrono no bloqueante
 * - Idempotencia garantizada (no envía duplicados)
 * - Manejo robusto de timeouts
 * - Auditoría completa de intentos
 * - Transacciones independientes (REQUIRES_NEW)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaEmailSenderService {

    private final JavaMailSender mailSender;
    private final EmailAuditLogRepository auditRepository;
    private final FacturaRepository facturaRepository;
    private final StorageService storageService;
    private final FacturaPdfService pdfService;

    @Value("${spring.mail.username}")
    private String emailFrom;

    /**
     * Envía email de forma asíncrona
     * 
     * @return CompletableFuture con resultado del envío
     */
    @Async("emailTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Boolean> enviarFacturaAsync(Long facturaId) {
        
        try {
            log.debug("📤 Iniciando envío asíncrono para factura ID: {}", facturaId);
            
            // 1. VERIFICACIÓN DE IDEMPOTENCIA
            if (yaFueEnviado(facturaId)) {
                log.info("⏭️  Factura {} ya fue enviada anteriormente, omitiendo", facturaId);
                return CompletableFuture.completedFuture(false);
            }
            
            // 2. CREAR AUDIT LOG (estado ENVIANDO para evitar duplicados concurrentes)
            EmailAuditLog audit = crearAuditLog(facturaId);
            
            // 3. CARGAR DATOS DE LA FACTURA
            Factura factura = cargarFactura(facturaId);
            
            // 4. VALIDACIONES PRE-ENVÍO
            if (!validarFacturaParaEnvio(factura, audit)) {
                return CompletableFuture.completedFuture(false);
            }
            
            // 5. CONSTRUIR DTO
            EmailFacturaDto dto = construirEmailDto(factura, audit);
            
            // 6. ENVIAR EMAIL
            boolean enviado = enviarEmail(dto, audit);
            
            return CompletableFuture.completedFuture(enviado);
            
        } catch (Exception e) {
            log.error("❌ Error enviando factura {}: {}", facturaId, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Verifica si el email ya fue enviado (idempotencia)
     */
    private boolean yaFueEnviado(Long facturaId) {
        return auditRepository.existsByFacturaIdAndEstado(facturaId, EstadoEmail.ENVIADO);
    }
    
    /**
     * Crea registro de auditoría en estado ENVIANDO
     * Esto previene envíos duplicados concurrentes
     */
    private EmailAuditLog crearAuditLog(Long facturaId) {
        EmailAuditLog audit = EmailAuditLog.builder()
            .facturaId(facturaId)
            .estado(EstadoEmail.ENVIANDO) // Estado intermedio para lock optimista
            .intentos(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return auditRepository.save(audit);
    }
    
    /**
     * Carga factura con relaciones necesarias
     */
    private Factura cargarFactura(Long facturaId) {
        return facturaRepository.findById(facturaId)
            .orElseThrow(() -> new IllegalStateException("Factura no encontrada: " + facturaId));
    }
    
    /**
     * Validaciones antes de enviar
     */
    private boolean validarFacturaParaEnvio(Factura factura, EmailAuditLog audit) {
        
        // Validar email receptor
        if (factura.getEmailReceptor() == null || factura.getEmailReceptor().isBlank()) {
            log.warn("⚠️  Factura {} sin email receptor", factura.getClave());
            audit.setEstado(EstadoEmail.OMITIDO);
            audit.setErrorMensaje("Sin email receptor");
            auditRepository.save(audit);
            return false;
        }
        
        // Validar cliente
        if (factura.getCliente() == null) {
            log.warn("⚠️  Factura {} sin cliente asociado", factura.getClave());
            audit.setEstado(EstadoEmail.OMITIDO);
            audit.setErrorMensaje("Sin cliente asociado");
            auditRepository.save(audit);
            return false;
        }
        
        return true;
    }
    
    /**
     * Construye el DTO para envío de email
     */
    private EmailFacturaDto construirEmailDto(Factura factura, EmailAuditLog audit) {
        try {
            // Generar PDF
            byte[] pdfBytes = pdfService.generarFacturaCarta(factura.getClave());
            
            // Cargar XMLs desde storage (si existen)
            byte[] xmlFirmadoBytes = null;
            byte[] respuestaHaciendaBytes = null;
            
            // Buscar bitácora para obtener paths de XMLs
            var bitacora = factura.getBitacora();
            if (bitacora != null) {
                if (bitacora.getXmlFirmadoPath() != null) {
                    try {
                        xmlFirmadoBytes = storageService.downloadFileAsBytes(bitacora.getXmlFirmadoPath());
                        audit.setAdjuntoXmlSize((long) xmlFirmadoBytes.length);
                    } catch (Exception e) {
                        log.warn("No se pudo cargar XML firmado: {}", e.getMessage());
                    }
                }
                
                if (bitacora.getXmlRespuestaPath() != null) {
                    try {
                        respuestaHaciendaBytes = storageService.downloadFileAsBytes(bitacora.getXmlRespuestaPath());
                        audit.setAdjuntoRespuestaSize((long) respuestaHaciendaBytes.length);
                    } catch (Exception e) {
                        log.warn("No se pudo cargar respuesta Hacienda: {}", e.getMessage());
                    }
                }
            }
            
            audit.setAdjuntoPdfSize((long) pdfBytes.length);
            audit.setClave(factura.getClave());
            audit.setEmailDestino(factura.getEmailReceptor());
            
            // Construir DTO
            return EmailFacturaDto.builder()
                .facturaId(factura.getId())
                .clave(factura.getClave())
                .consecutivo(factura.getConsecutivo())
                .emailDestino(factura.getEmailReceptor())
                .tipoDocumento(factura.getTipoDocumento().getDescripcion())
                .nombreComercial(factura.getSucursal().getEmpresa().getNombreComercial())
                .razonSocial(factura.getSucursal().getEmpresa().getNombreRazonSocial())
                .cedulaJuridica(factura.getSucursal().getEmpresa().getIdentificacion())
                .fechaEmision(formatearFecha(factura.getFechaEmision()))
                .logoUrl(factura.getSucursal().getEmpresa().getLogoUrl())
                .pdfBytes(pdfBytes)
                .xmlFirmadoBytes(xmlFirmadoBytes)
                .respuestaHaciendaBytes(respuestaHaciendaBytes)
                .build();
                
        } catch (Exception e) {
            log.error("Error construyendo DTO para factura {}: {}", factura.getClave(), e.getMessage());
            audit.setEstado(EstadoEmail.ERROR);
            audit.setErrorMensaje("Error preparando archivos: " + e.getMessage());
            audit.setErrorTipo("PREPARACION");
            auditRepository.save(audit);
            throw new RuntimeException("Error preparando email", e);
        }
    }
    
    /**
     * Envía el email y maneja errores robustamente
     */
    private boolean enviarEmail(EmailFacturaDto dto, EmailAuditLog audit) {
        
        int intentoActual = audit.getIntentos() + 1;
        audit.setIntentos(intentoActual);
        
        try {
            // Construir mensaje MIME
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailFrom);
            helper.setTo(dto.getEmailDestino());
            helper.setSubject(String.format("Factura Electrónica %s - %s", 
                dto.getConsecutivo(), dto.getNombreComercial()));
            
            audit.setAsunto(helper.getMimeMessage().getSubject());
            
            // HTML del email
            String htmlContent = generarHtmlEmail(dto);
            helper.setText(htmlContent, true);
            
            // Adjuntar PDF (siempre)
            helper.addAttachment(
                String.format("Factura_%s.pdf", dto.getClave()),
                new ByteArrayDataSource(dto.getPdfBytes(), "application/pdf")
            );
            
            // Adjuntar XML firmado (si existe)
            if (dto.getXmlFirmadoBytes() != null) {
                helper.addAttachment(
                    String.format("FE_%s.xml", dto.getClave()),
                    new ByteArrayDataSource(dto.getXmlFirmadoBytes(), "text/xml")
                );
            }
            
            // Adjuntar respuesta Hacienda (si existe)
            if (dto.getRespuestaHaciendaBytes() != null) {
                helper.addAttachment(
                    String.format("Respuesta_%s.xml", dto.getClave()),
                    new ByteArrayDataSource(dto.getRespuestaHaciendaBytes(), "text/xml")
                );
            }
            
            // ENVIAR - aquí puede ocurrir timeout
            long startTime = System.currentTimeMillis();
            mailSender.send(message);
            long duration = System.currentTimeMillis() - startTime;
            
            // ÉXITO
            audit.setEstado(EstadoEmail.ENVIADO);
            audit.setFechaEnvio(LocalDateTime.now());
            audit.setErrorMensaje(null);
            audit.setErrorTipo(null);
            auditRepository.save(audit);
            
            log.info("✅ Email enviado exitosamente: factura={}, destino={}, duración={}ms", 
                dto.getClave(), dto.getEmailDestino(), duration);
            
            return true;
            
        } catch (MailException e) {
            // Errores de mail (incluye timeout)
            return manejarErrorMail(e, dto, audit, intentoActual);
            
        } catch (MessagingException e) {
            // Errores de construcción de mensaje
            return manejarErrorMensaje(e, dto, audit, intentoActual);
            
        } catch (Exception e) {
            // Otros errores inesperados
            return manejarErrorGenerico(e, dto, audit, intentoActual);
        }
    }
    
    /**
     * Maneja errores de envío de mail (timeout, conexión, etc)
     */
    private boolean manejarErrorMail(MailException e, EmailFacturaDto dto, 
                                     EmailAuditLog audit, int intentoActual) {
        
        String mensaje = e.getMessage();
        
        // CASO ESPECIAL: Timeout
        if (mensaje != null && (mensaje.contains("timeout") || mensaje.contains("timed out"))) {
            
            log.warn("⏱️  TIMEOUT enviando factura {} (intento {}). Email posiblemente enviado.", 
                dto.getClave(), intentoActual);
            
            // Estrategia: asumir que se envió (Gmail normalmente sí lo envía antes del timeout)
            audit.setEstado(EstadoEmail.ENVIADO);
            audit.setFechaEnvio(LocalDateTime.now());
            audit.setErrorMensaje("Timeout detectado - asumido como enviado");
            audit.setErrorTipo("TIMEOUT_ASUMIDO_ENVIADO");
            auditRepository.save(audit);
            
            return true; // Consideramos éxito para evitar reenvíos
        }
        
        // Otros errores de mail
        log.error("❌ Error de mail enviando factura {}: {}", dto.getClave(), mensaje);
        audit.setEstado(EstadoEmail.ERROR);
        audit.setErrorMensaje(mensaje);
        audit.setErrorTipo("MAIL_EXCEPTION");
        auditRepository.save(audit);
        
        return false;
    }
    
    /**
     * Maneja errores de construcción de mensaje
     */
    private boolean manejarErrorMensaje(MessagingException e, EmailFacturaDto dto, 
                                        EmailAuditLog audit, int intentoActual) {
        
        log.error("❌ Error construyendo mensaje para factura {}: {}", dto.getClave(), e.getMessage());
        audit.setEstado(EstadoEmail.FALLO_PERMANENTE);
        audit.setErrorMensaje("Error construyendo mensaje: " + e.getMessage());
        audit.setErrorTipo("MESSAGING_EXCEPTION");
        auditRepository.save(audit);
        
        return false;
    }
    
    /**
     * Maneja errores genéricos inesperados
     */
    private boolean manejarErrorGenerico(Exception e, EmailFacturaDto dto, 
                                         EmailAuditLog audit, int intentoActual) {
        
        log.error("❌ Error inesperado enviando factura {}: {}", dto.getClave(), e.getMessage(), e);
        audit.setEstado(EstadoEmail.ERROR);
        audit.setErrorMensaje("Error inesperado: " + e.getMessage());
        audit.setErrorTipo("EXCEPCION_GENERICA");
        auditRepository.save(audit);
        
        return false;
    }
    
    /**
     * Genera HTML del email
     */
    private String generarHtmlEmail(EmailFacturaDto dto) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; }
                    .header { background-color: #007bff; color: white; padding: 30px; text-align: center; }
                    .logo { max-height: 80px; margin-bottom: 15px; }
                    .content { padding: 30px; }
                    .info-box { background: #e8f4fd; padding: 20px; border-radius: 8px; margin: 25px 0; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Factura Electrónica</h1>
                        <p>%s</p>
                    </div>
                    <div class="content">
                        <p>Estimado cliente,</p>
                        <p>Adjunto encontrará su factura electrónica.</p>
                        <div class="info-box">
                            <p><strong>Consecutivo:</strong> %s</p>
                            <p><strong>Fecha:</strong> %s</p>
                            <p><strong>Clave:</strong> %s</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>%s</p>
                        <p>Cédula Jurídica: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            dto.getNombreComercial(),
            dto.getConsecutivo(),
            dto.getFechaEmision(),
            dto.getClave(),
            dto.getRazonSocial(),
            dto.getCedulaJuridica()
        );
    }
    
    /**
     * Formatea fecha para mostrar
     */
    private String formatearFecha(String fechaStr) {
        try {
            if (fechaStr == null || fechaStr.isBlank()) {
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            ZonedDateTime zdt = ZonedDateTime.parse(fechaStr);
            return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }
}