package digital.samyx.mailreceptor.service.impl;

import digital.samyx.mailreceptor.service.INotificationService;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Service
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String correoDistribucion;

    private final SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final SimpleDateFormat formato1 = new SimpleDateFormat("dd/MM/yyyy HH:mm a");
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));

    @Override
    public void sendDuplicateNotification(String clave, String emisorFactura,
                                          String fechaEmision, String totalComprobante,
                                          String emailTo) throws Exception {

        Date fechaEmisionDate = formato.parse(fechaEmision);
        String fechaFormateada = formato1.format(fechaEmisionDate);

        String consecutivo = clave.substring(21, 41);
        String asunto = "⚠️ Factura Duplicada - " + emisorFactura;

        String htmlContent = buildDuplicateEmailHtml(consecutivo, emisorFactura,
                fechaFormateada, totalComprobante);

        enviarEmail(emailTo, asunto, htmlContent);
    }

    private String buildDuplicateEmailHtml(String consecutivo, String emisor,
                                           String fecha, String monto) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; background: #f5f5f5; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                              color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: white; padding: 30px; border-radius: 0 0 10px 10px; }
                    .warning-box { background: #fff3cd; border-left: 4px solid #ffc107; 
                                   padding: 15px; margin: 20px 0; border-radius: 4px; }
                    .info-table { width: 100%; margin: 20px 0; }
                    .info-table td { padding: 10px; border-bottom: 1px solid #eee; }
                    .info-table td:first-child { font-weight: 600; color: #555; width: 40%; }
                    .footer { text-align: center; color: #888; font-size: 12px; margin-top: 20px; }
                    .logo { font-size: 28px; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🧾 NathBit POS</div>
                        <p style="margin: 10px 0 0 0; opacity: 0.9;">Sistema de Recepción Automática</p>
                    </div>
                    
                    <div class="content">
                        <h2 style="color: #333; margin-top: 0;">⚠️ Factura Duplicada Detectada</h2>
                        
                        <div class="warning-box">
                            <strong>Atención:</strong> Esta factura ya fue procesada anteriormente en el sistema.
                        </div>
                        
                        <table class="info-table">
                            <tr>
                                <td>📄 Consecutivo:</td>
                                <td><strong>%s</strong></td>
                            </tr>
                            <tr>
                                <td>🏢 Proveedor:</td>
                                <td><strong>%s</strong></td>
                            </tr>
                            <tr>
                                <td>📅 Fecha Emisión:</td>
                                <td>%s</td>
                            </tr>
                            <tr>
                                <td>💰 Monto Total:</td>
                                <td><strong style="color: #667eea; font-size: 18px;">₡%s</strong></td>
                            </tr>
                        </table>
                        
                        <p style="color: #666; margin-top: 30px;">
                            No es necesario realizar ninguna acción. El sistema ha omitido el procesamiento 
                            de esta factura para evitar duplicados.
                        </p>
                        
                        <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                        
                        <div class="footer">
                            <p>Este es un mensaje automático, por favor no responder.</p>
                            <p><strong>NathBit POS</strong> | Sistema de Punto de Venta</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(consecutivo, emisor, fecha, monto);
    }

    private void enviarEmail(String emailTo, String asunto, String htmlContent) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(new InternetAddress(emailTo.trim()));
            helper.setFrom(new InternetAddress(correoDistribucion, "NathBit POS - Sistema Automático"));
            helper.setSubject(asunto);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("✅ Email enviado exitosamente a: {}", emailTo);

        } catch (Exception e) {
            log.error("❌ Error enviando email a {}: {}", emailTo, e.getMessage());
        }
    }
}