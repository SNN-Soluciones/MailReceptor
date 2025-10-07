package digital.samyx.mailreceptor.enums.mh;

/**
 * Define el modo de impresión que usa una sucursal
 * Determina si imprime localmente (navegador) o usa orquestador de red
 */
public enum ModoImpresion {
    /**
     * Imprime desde el navegador usando Web API
     * (Share API, Quick Printer en Android, impresión del navegador, etc.)
     */
    LOCAL,
    
    /**
     * Usa "La Chismosa" - Orquestador de impresoras en red local
     * La IP del orquestador se configura en el campo ipOrquestador
     * Si falla, hace fallback a LOCAL
     */
    ORQUESTADOR
}