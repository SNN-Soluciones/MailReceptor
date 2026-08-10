package digital.samyx.mailreceptor.service;

import jakarta.mail.Message;

public interface IEmailService {

    /**
     * Abre el INBOX del buzón y devuelve sus mensajes no leídos. La conexión
     * queda abierta: hay que usarlo dentro de un try-with-resources.
     */
    BuzonImap abrirBuzon(String email, String password, String host);

    void markMessageAsRead(Message message);

    /**
     * Deja el correo como NO LEÍDO en el buzón del cliente, para que quede
     * visible y se reintente en el próximo ciclo.
     */
    void markMessageAsUnread(Message message);
}
