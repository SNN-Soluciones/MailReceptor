package digital.samyx.mailreceptor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configuración optimizada de JavaMailSender para alto volumen
 * Lee configuración desde application.yml
 */
@Configuration
public class EmailPoolConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    @Primary
    public JavaMailSender optimizedMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Configuración básica desde YML
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        // Propiedades SMTP optimizadas
        Properties props = mailSender.getJavaMailProperties();

        // Protocolo y seguridad
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", host);

        // Timeouts (desde YML pero con fallback)
        props.put("mail.smtp.connectiontimeout", "30000");
        props.put("mail.smtp.timeout", "60000");
        props.put("mail.smtp.writetimeout", "60000");

        // Optimizaciones adicionales
        props.put("mail.smtp.quitwait", "false");
        props.put("mail.mime.charset", "UTF-8");
        props.put("mail.mime.splitlongparameters", "false");

        // Debug OFF (controlado por YML)
        props.put("mail.debug", "false");
        props.put("mail.debug.auth", "false");

        return mailSender;
    }
}