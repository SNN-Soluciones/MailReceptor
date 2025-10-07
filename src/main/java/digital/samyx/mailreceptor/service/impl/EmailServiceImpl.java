package digital.samyx.mailreceptor.service.impl;

import digital.samyx.mailreceptor.service.IEmailService;
import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.SearchTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Service
@Slf4j
public class EmailServiceImpl implements IEmailService {

    @Override
    public List<Message> getUnreadMessages(String email, String password, String host) {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            Session session = Session.getDefaultInstance(properties, null);
            
            Store store = session.getStore("imaps");
            store.connect(host, email, password);
            
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_WRITE);
            
            Message[] messages = folderInbox.search(
                new FlagTerm(new Flags(Flags.Flag.SEEN), false)
            );
            
            return Arrays.asList(messages);
            
        } catch (Exception e) {
            log.error("Error al obtener mensajes no leídos: ", e);
            return Arrays.asList();
        }
    }

    @Override
    public void markMessageAsRead(Message message) {
        try {
            message.setFlag(Flags.Flag.SEEN, true);
        } catch (MessagingException e) {
            log.error("Error al marcar mensaje como leído: ", e);
        }
    }
}