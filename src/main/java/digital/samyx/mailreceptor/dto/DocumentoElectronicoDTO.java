package digital.samyx.mailreceptor.dto;

import lombok.Data;

@Data
public class DocumentoElectronicoDTO {
    private String tipoDocumento; // FE, NC, MR
    private String claveDocumento;
    private String xmlFileName;
    private String pdfFileName;
    private FacturaDataDTO datosFactura;
    private boolean procesado = false;
}