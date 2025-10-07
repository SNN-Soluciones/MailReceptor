package digital.samyx.mailreceptor.service.impl;

import digital.samyx.mailreceptor.dto.DocumentoElectronicoDTO;
import digital.samyx.mailreceptor.dto.FacturaDataDTO;
import digital.samyx.mailreceptor.enums.TipoDocumentoElectronico;
import digital.samyx.mailreceptor.service.IFacturaParserService;
import digital.samyx.mailreceptor.util.XmlHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FacturaParserServiceImpl implements IFacturaParserService {

    private final XPath xPath = XPathFactory.newInstance().newXPath();

    @Override
    public TipoDocumentoElectronico detectarTipoDocumento(Document xml) {
        try {
            // Verificar FacturaElectronica
            NodeList fe = xml.getElementsByTagName("FacturaElectronica");
            if (fe.getLength() > 0) {
                return TipoDocumentoElectronico.FACTURA_ELECTRONICA;
            }

            // Verificar NotaCreditoElectronica
            NodeList nc = xml.getElementsByTagName("NotaCreditoElectronica");
            if (nc.getLength() > 0) {
                return TipoDocumentoElectronico.NOTA_CREDITO;
            }

            // Verificar MensajeReceptor
            NodeList mr = xml.getElementsByTagName("MensajeReceptor");
            if (mr.getLength() > 0) {
                return TipoDocumentoElectronico.MENSAJE_RECEPTOR;
            }

        } catch (Exception e) {
            log.error("Error detectando tipo de documento: ", e);
        }

        return TipoDocumentoElectronico.DESCONOCIDO;
    }

    @Override
    public String extraerClave(Document xml, TipoDocumentoElectronico tipo) {
        try {
            String xpath = "/" + tipo.getRootElement() + "/Clave";
            NodeList nodes = (NodeList) xPath.evaluate(xpath, xml.getDocumentElement(), XPathConstants.NODESET);

            if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
                return nodes.item(0).getTextContent();
            }
        } catch (Exception e) {
            log.error("Error extrayendo clave: ", e);
        }
        return null;
    }

    @Override
    public List<DocumentoElectronicoDTO> procesarArchivosXml(List<String> xmlFiles) throws Exception {
        List<DocumentoElectronicoDTO> documentos = new ArrayList<>();

        for (String xmlFile : xmlFiles) {
            try {
                Document xml = loadXmlDocument(xmlFile);
                TipoDocumentoElectronico tipo = detectarTipoDocumento(xml);

                if (tipo == TipoDocumentoElectronico.DESCONOCIDO) {
                    log.warn("Tipo de documento desconocido para archivo: {}", xmlFile);
                    continue;
                }

                // Solo procesamos Facturas y Notas de Crédito
                if (tipo != TipoDocumentoElectronico.FACTURA_ELECTRONICA &&
                        tipo != TipoDocumentoElectronico.NOTA_CREDITO) {
                    log.info("Ignorando documento tipo {}: {}", tipo.getCodigo(), xmlFile);
                    continue;
                }

                String clave = extraerClave(xml, tipo);
                if (clave == null || clave.length() <= 30) {
                    log.warn("Clave inválida para documento: {}", xmlFile);
                    continue;
                }

                DocumentoElectronicoDTO documento = new DocumentoElectronicoDTO();
                documento.setTipoDocumento(tipo.getCodigo());
                documento.setClaveDocumento(clave);
                documento.setXmlFileName(xmlFile);

                // Parsear datos de la factura
                FacturaDataDTO datosFactura = parseFacturaXml(xml);
                documento.setDatosFactura(datosFactura);

                documentos.add(documento);
                log.info("Documento procesado - Tipo: {}, Clave: {}", tipo.getCodigo(), clave);

            } catch (Exception e) {
                log.error("Error procesando archivo XML {}: ", xmlFile, e);
            }
        }

        return documentos;
    }

    @Override
    public FacturaDataDTO parseFacturaXml(Document xml) throws Exception {
        FacturaDataDTO factura = new FacturaDataDTO();

        // Detectar tipo para usar el path correcto
        TipoDocumentoElectronico tipo = detectarTipoDocumento(xml);
        String rootPath = "/" + tipo.getRootElement() + "/";

        factura.setClaveFactura(getFieldValue(xml, rootPath + "Clave"));
        factura.setFechaEmision(getFieldValue(xml, rootPath + "FechaEmision"));
        factura.setEmisorFactura(getFieldValue(xml, rootPath + "Emisor/Nombre"));
        factura.setEmisorTipoIdentificacion(getFieldValue(xml, rootPath + "Emisor/Identificacion/Tipo"));
        factura.setEmisorIdentificacion(getFieldValue(xml, rootPath + "Emisor/Identificacion/Numero"));

        // Moneda y tipo de cambio con valores por defecto
        try {
            factura.setMoneda(getFieldValue(xml, rootPath + "ResumenFactura/CodigoTipoMoneda/CodigoMoneda"));
            factura.setTipoCambio(getFieldValue(xml, rootPath + "ResumenFactura/CodigoTipoMoneda/TipoCambio"));
        } catch (Exception e) {
            factura.setMoneda("CRC");
            factura.setTipoCambio("1.00");
        }

        // Total impuestos con valor por defecto
        try {
            factura.setTotalImpuestos(getFieldValue(xml, rootPath + "ResumenFactura/TotalImpuesto"));
        } catch (Exception e) {
            factura.setTotalImpuestos("0.00");
        }

        factura.setTotalComprobante(getFieldValue(xml, rootPath + "ResumenFactura/TotalComprobante"));
        factura.setReceptorTipoIdentificacion(getFieldValue(xml, rootPath + "Receptor/Identificacion/Tipo"));
        factura.setReceptorIdentificacion(getFieldValue(xml, rootPath + "Receptor/Identificacion/Numero"));

        return factura;
    }

    private String getFieldValue(Document xml, String xpath) {
        try {
            NodeList nodes = (NodeList) xPath.evaluate(xpath, xml.getDocumentElement(), XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
                return nodes.item(0).getTextContent();
            }
        } catch (Exception e) {
            log.debug("Campo no encontrado: {}", xpath);
        }
        return "";
    }

    @Override
    public Document loadXmlDocument(String filePath) throws Exception {
        return XmlHelper.getDocument(filePath);
    }
}