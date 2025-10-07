package digital.samyx.mailreceptor.dto;

import lombok.Data;

@Data
public class FacturaDataDTO {
    private String claveFactura;
    private String emisorFactura;
    private String emisorTipoIdentificacion;
    private String emisorIdentificacion;
    private String fechaEmision;
    private String moneda;
    private String tipoCambio;
    private String totalImpuestos;
    private String totalComprobante;
    private String receptorTipoIdentificacion;
    private String receptorIdentificacion;
}