package digital.samyx.mailreceptor.service.impl;

import digital.samyx.mailreceptor.service.BuzonImap;
import digital.samyx.mailreceptor.service.IEmailService;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Properties;

@Service
@Slf4j
public class EmailServiceImpl implements IEmailService {

    @Override
    public BuzonImap abrirBuzon(String email, String password, String host) {
        try {
            String imapHost = normalizeImapHost(host, email);

            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", imapHost);
            properties.put("mail.imaps.port", "993");
            properties.put("mail.imaps.ssl.enable", "true");
            properties.put("mail.imaps.ssl.trust", imapHost);
            properties.put("mail.imaps.ssl.protocols", "TLSv1.2 TLSv1.3");
            properties.put("mail.imaps.timeout", "30000");
            properties.put("mail.imaps.connectiontimeout", "30000");

            // Autenticación básica (si el tenant la permite) sobre IMAPS 993
            properties.put("mail.imaps.auth", "true");
            properties.put("mail.imaps.starttls.enable", "false"); // IMAPS directo (SSL)

            // PEEK: leer el cuerpo y los adjuntos NO marca el correo como leído en el
            // servidor. Sin esto, con solo abrir los adjuntos el correo ya queda leído
            // y sería imposible dejarlo no leído cuando la factura no aplica.
            properties.put("mail.imaps.peek", "true");

            // Debug selectivo (útil para Microsoft 365)
            boolean debug = imapHost.contains("office365") || imapHost.contains("outlook");
            properties.put("mail.debug", String.valueOf(debug));

            Session session = Session.getInstance(properties);
            if (debug) session.setDebug(true);

            Store store = session.getStore("imaps");

            log.info("📧 Conectando a {} con usuario {}", imapHost, email);
            log.info("🔑 Longitud de password: {}", password != null ? password.length() : 0);

            store.connect(imapHost, email, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.info("✅ Conectado a {} - {} mensajes no leídos encontrados", imapHost, messages.length);

            return new BuzonImap(store, inbox, Arrays.asList(messages));

        } catch (Exception e) {
            log.error("❌ Error al obtener mensajes no leídos de {} con usuario {}: {}",
                    host, email, e.getMessage());
            log.error("Stack trace completo: ", e);
            return BuzonImap.vacio();
        }
    }

    @Override
    public void markMessageAsRead(Message message) {
        try {
            message.setFlag(Flags.Flag.SEEN, true);
            log.debug("✓ Mensaje marcado como leído");
        } catch (MessagingException e) {
            log.error("❌ Error al marcar mensaje como leído: ", e);
        }
    }

    @Override
    public void markMessageAsUnread(Message message) {
        try {
            // Con PEEK activo la bandera casi nunca llega puesta; se limpia igual por
            // si el servidor la marcó por su cuenta (o quedó de un ciclo anterior).
            if (message.isSet(Flags.Flag.SEEN)) {
                message.setFlag(Flags.Flag.SEEN, false);
            }
            log.debug("✓ Mensaje dejado como no leído");
        } catch (MessagingException e) {
            log.error("❌ Error al dejar el mensaje como no leído: ", e);
        }
    }

    /**
     * Normaliza el host recibido desde la DB para que siempre sea un host IMAP válido.
     * - Corrige typos ("smpt." → "smtp.")
     * - Mapea SMTP→IMAP para Gmail y Microsoft 365/Outlook
     * - Fuerza el host IMAP correcto por dominio
     */
    private String normalizeImapHost(String host, String email) {
        if (host == null) return null;
        String h = host.trim().toLowerCase();

        // Corrige typo común
        if (h.startsWith("smpt.")) {
            h = "smtp." + h.substring(5);
        }

        // Mapear SMTP -> IMAP
        if (h.equals("smtp.gmail.com")) h = "imap.gmail.com";
        if (h.equals("smtp.office365.com") || h.equals("smtp.outlook.com")) h = "outlook.office365.com";

        // Aliases antiguos o variantes
        if (h.equals("imap-mail.outlook.com")) h = "outlook.office365.com";

        // Si la cadena sugiere Microsoft 365/Outlook, forzar el host IMAP correcto
        if (h.contains("office365") || h.contains("outlook")) {
            h = "outlook.office365.com";
        }

        // Si el correo es @gmail.com, forzar IMAP de Gmail
        if (email != null && email.toLowerCase().endsWith("@gmail.com")) {
            h = "imap.gmail.com";
        }

        return h;
    }
}