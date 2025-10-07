package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.dto.DocumentoElectronicoDTO;
import digital.samyx.mailreceptor.dto.FacturaDataDTO;
import digital.samyx.mailreceptor.enums.TipoDocumentoElectronico;
import org.w3c.dom.Document;

import java.util.List;

public interface IFacturaParserService {
    FacturaDataDTO parseFacturaXml(Document xml) throws Exception;
    Document loadXmlDocument(String filePath) throws Exception;
    TipoDocumentoElectronico detectarTipoDocumento(Document xml);
    String extraerClave(Document xml, TipoDocumentoElectronico tipo);
    List<DocumentoElectronicoDTO> procesarArchivosXml(List<String> xmlFiles) throws Exception;
}