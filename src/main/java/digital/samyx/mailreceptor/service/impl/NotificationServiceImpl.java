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

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${correo.de.distribucion}")
    private String correoDistribucion;

    private final SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final SimpleDateFormat formato1 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a");

    @Override
    public void sendDuplicateNotification(String clave, String emisorFactura, 
                                         String fechaEmision, String totalComprobante, 
                                         String emailTo) throws Exception {
        
        String empresaSaluda = "Soluciones InformMata";
        String asunto = "Notificación del sistema de recepción automática - La Factura Electrónica generada por " 
                       + emisorFactura + ", ya fue recibida anteriormente.";
        
        Date fechaEmisionDate = formato.parse(fechaEmision);
        String fechaFormateada = formato1.format(fechaEmisionDate);
        
        enviaNotificacionMR(clave, emisorFactura, empresaSaluda, fechaFormateada, 
                           totalComprobante, emailTo, asunto);
    }

    private void enviaNotificacionMR(String clave, String emisorFactura, String empresaSaluda,
                                    String fechaEmision, String totalComprobante, 
                                    String emailTo, String asunto) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String consecutivo = clave.substring(21, 41);
            
            StringBuilder msj = new StringBuilder();
            msj.append("<p style=\"font-family: Arial;\">Estimado cliente,</p>");
            msj.append("<p style=\"font-family: Arial;\">El comprobante de Factura Electrónica con la consecutivo: <b>")
               .append(consecutivo)
               .append("</b>, generada por <b>").append(emisorFactura)
               .append("</b> el <b>").append(fechaEmision)
               .append("</b> por un monto de <b>").append(totalComprobante)
               .append("</b> ya fue recibida anteriormente.</p>");
            msj.append("<p style=\"font-family: Arial;\">Este correo se generó de forma automática, por favor no responder.</p>");
            msj.append("<p style=\"font-family: Arial;\">Saludos,</p>");
            msj.append("<p style=\"font-family: Arial;\"><b>").append(empresaSaluda).append("</b></p>");
            
            helper.setTo(new InternetAddress(emailTo.trim(), 
                "Notificación del sistema de recepción automático - " + empresaSaluda));
            helper.setFrom(new InternetAddress(correoDistribucion, 
                "Notificación del sistema de recepción automático"));
            helper.setSubject(asunto);
            helper.setText(msj.toString(), true);
            
            emailSender.send(message);
            log.info("Se envió un mail a {}", emailTo);
            
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", emailTo, e.getMessage());
        }
    }
}