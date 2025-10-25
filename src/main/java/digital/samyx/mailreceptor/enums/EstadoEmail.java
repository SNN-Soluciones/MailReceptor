package digital.samyx.mailreceptor.enums;

public enum EstadoEmail {
    PENDIENTE,           // Creado pero no enviado
    ENVIANDO,            // En proceso de envío (lock optimista)
    ENVIADO,             // Enviado exitosamente
    ERROR,               // Error temporal (puede reintentar)
    FALLO_PERMANENTE,    // Error permanente (no reintentar)
    OMITIDO,             // Omitido por validaciones
    REINTENTANDO         // En proceso de reintento
}