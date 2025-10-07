package digital.samyx.mailreceptor.enums;

public enum TipoDocumentoElectronico {
    FACTURA_ELECTRONICA("FE", "FacturaElectronica"),
    NOTA_CREDITO("NC", "NotaCreditoElectronica"),
    MENSAJE_RECEPTOR("MR", "MensajeReceptor"),
    DESCONOCIDO("DESCONOCIDO", "");

    private final String codigo;
    private final String rootElement;

    TipoDocumentoElectronico(String codigo, String rootElement) {
        this.codigo = codigo;
        this.rootElement = rootElement;
    }

    public String getCodigo() { return codigo; }
    public String getRootElement() { return rootElement; }
}