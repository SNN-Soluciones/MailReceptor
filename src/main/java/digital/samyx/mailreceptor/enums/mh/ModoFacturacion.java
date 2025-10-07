package digital.samyx.mailreceptor.enums.mh;

public enum ModoFacturacion {
    ELECTRONICO("Facturación Electrónica"),
    SOLO_INTERNO("Solo Control Interno"),
    MIXTO("Mixto - Electrónico e Interno"); // NUEVO

    private final String descripcion;

    ModoFacturacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}