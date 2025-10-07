package digital.samyx.mailreceptor.service;

import jakarta.mail.Message;
import java.util.List;

public interface IEmailService {
    List<Message> getUnreadMessages(String email, String password, String host);
    void markMessageAsRead(Message message);
}