package digital.samyx.mailreceptor.service;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Store;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Conexión abierta al INBOX de un buzón receptor.
 *
 * Es AutoCloseable a propósito: cada ciclo del scheduler abre una conexión IMAP
 * por buzón y los proveedores limitan las simultáneas (Gmail ~15), así que hay
 * que cerrarlas. Los mensajes solo se pueden leer mientras la carpeta esté
 * abierta → procesarlos SIEMPRE dentro del try-with-resources.
 */
@Slf4j
public class BuzonImap implements AutoCloseable {

    private final Store store;
    private final Folder inbox;
    private final List<Message> noLeidos;

    public BuzonImap(Store store, Folder inbox, List<Message> noLeidos) {
        this.store = store;
        this.inbox = inbox;
        this.noLeidos = noLeidos;
    }

    /** Buzón que no se pudo abrir: se comporta como uno sin mensajes. */
    public static BuzonImap vacio() {
        return new BuzonImap(null, null, List.of());
    }

    public List<Message> getNoLeidos() {
        return noLeidos;
    }

    @Override
    public void close() {
        try {
            // expunge=false: nunca borramos correos del buzón del cliente.
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
        } catch (Exception e) {
            log.warn("⚠️ No se pudo cerrar el INBOX: {}", e.getMessage());
        }
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (Exception e) {
            log.warn("⚠️ No se pudo cerrar la conexión IMAP: {}", e.getMessage());
        }
    }
}
