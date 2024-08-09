package digital.samyx.mailreceptor.component;

import digital.samyx.mailreceptor.entity.FEMensajeReceptorAutomatico;
import digital.samyx.mailreceptor.service.IFEMensajeReceptorAutomaticoService;
import digital.samyx.mailreceptor.util.UnzipFiles;
import digital.samyx.mailreceptor.util.XmlHelper;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.SearchTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class MensajeReceptorAutomatico {
    @Value("${path.upload.files.api}")
    private String pathUploadFilesApi;

    @Autowired
    private IFEMensajeReceptorAutomaticoService _mrService;

    @Autowired
    public JavaMailSender emailSender;

    @Value("${correo.de.distribucion}")
    private String correoDistribucion;

    @Value("${api.host}")
    private String apiHost;

    @Value("${api.userName}")
    private String apiUserName;

    @Value("${api.password}")
    private String apiPassword;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ZipFile zipFile;

    @Scheduled(fixedDelay = 60000L)
    public void downloadEmailAttachments() throws ParserConfigurationException, SAXException, SQLException, ParseException {
        String saveDirectory = this.pathUploadFilesApi + "/mr-automatico";
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat formato1 = new SimpleDateFormat("dd/mm/YYYY HH:mm:ss a");
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(properties, null);
        String emisorFactura = "";
        String emisorTipoIdentificacion = "";
        String emisorIdentificacion = "";
        String fechaEmision = "";
        String moneda = "";
        String tipoCambio = "";
        String totalImpuestos = "";
        String totalComprobante = "";
        String receptorTipoIdentificacion = "";
        String receptorIdentificacion = "";
        String claveFactura = "";
        String facturaXml = "";
        String facturaXmlZip = "";
        String facturaPdfZip = "";
        try {
            Store store = session.getStore("imaps");
            store.connect(this.apiHost, this.apiUserName, this.apiPassword);
            XPath xPath = XPathFactory.newInstance().newXPath();
            Folder folderInbox = store.getFolder("INBOX");
            folderInbox.open(2);
            Message[] arrayMessages = folderInbox.search((SearchTerm)new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            for (int i = 0; i < arrayMessages.length; i++) {
                String prefijo = System.currentTimeMillis() + "-";
                Message message = arrayMessages[i];
                Address[] fromAddress = message.getFrom();
                String from = fromAddress[0].toString();
                Pattern pattern = Pattern.compile("<(.*?)>");
                Matcher matcher = pattern.matcher(from);
                String enviarA = "";
                if (matcher.find())
                    enviarA = matcher.group(1).toString();
                String contentType = message.getContentType();
                String messageContent = "";
                String extension = "";
                String attachFiles = "";
                if (contentType.contains("multipart")) {
                    Multipart multiPart = (Multipart)message.getContent();
                    int numberOfParts = multiPart.getCount();
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        MimeBodyPart part = (MimeBodyPart)multiPart.getBodyPart(partCount);
                        if ("attachment".equalsIgnoreCase(part.getDisposition())) {
                            String fileName = prefijo + "sinmata" + getFileName((Part)part).substring(getFileName((Part)part).toString().lastIndexOf("."));
                            String rutaAchivoGuardado = saveDirectory + File.separator + fileName;
                            String rutaAchivoGuardado2 = saveDirectory + File.separator;
                            int n = fileName.lastIndexOf('.');
                            extension = fileName.substring(n + 1);
                            if (extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("pdf") || extension.equalsIgnoreCase("zip")) {
                                part.saveFile(rutaAchivoGuardado);
                                try {
                                    if (extension.equalsIgnoreCase("zip")) {
                                        this.zipFile = new ZipFile(rutaAchivoGuardado);
                                        (new UnzipFiles()).unzip(rutaAchivoGuardado, rutaAchivoGuardado2);
                                        Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
                                        while (entries.hasMoreElements()) {
                                            ZipEntry entry = entries.nextElement();
                                            String nameFile = entry.getName();
                                            int n2 = nameFile.lastIndexOf('.');
                                            extension = nameFile.substring(n2 + 1);
                                            if (extension.equalsIgnoreCase("xml")) {
                                                Document xml = XmlHelper.getDocument(rutaAchivoGuardado2 + nameFile);
                                                if (extension.equalsIgnoreCase("pdf"))
                                                    facturaPdfZip = facturaPdfZip + nameFile;
                                                claveFactura = getNameFieldXml(xPath, xml, "Clave");
                                                if (claveFactura.length() > 30) {
                                                    fechaEmision = getNameFieldXml(xPath, xml, "FechaEmision");
                                                    emisorFactura = getNameFieldXml(xPath, xml, "Emisor/Nombre");
                                                    emisorTipoIdentificacion = getNameFieldXml(xPath, xml, "Emisor/Identificacion/Tipo");
                                                    emisorIdentificacion = getNameFieldXml(xPath, xml, "Emisor/Identificacion/Numero");
                                                    try {
                                                        moneda = getNameFieldXml(xPath, xml, "ResumenFactura/CodigoTipoMoneda/CodigoMoneda");
                                                        tipoCambio = getNameFieldXml(xPath, xml, "ResumenFactura/CodigoTipoMoneda/TipoCambio");
                                                    } catch (Exception e) {
                                                        moneda = "CRC";
                                                        tipoCambio = "1.00";
                                                    }
                                                    try {
                                                        totalImpuestos = getNameFieldXml(xPath, xml, "ResumenFactura/TotalImpuesto");
                                                    } catch (Exception e) {
                                                        totalImpuestos = "0.00";
                                                    }
                                                    totalComprobante = getNameFieldXml(xPath, xml, "ResumenFactura/TotalComprobante");
                                                    receptorTipoIdentificacion = getNameFieldXml(xPath, xml, "Receptor/Identificacion/Tipo");
                                                    receptorIdentificacion = getNameFieldXml(xPath, xml, "Receptor/Identificacion/Numero");
                                                    facturaXmlZip = nameFile;
                                                    try {
                                                        FEMensajeReceptorAutomatico mr = new FEMensajeReceptorAutomatico();
                                                        mr.setClave(claveFactura);
                                                        mr.setCorreoFrom(enviarA);
                                                        mr.setEmisorFactura(emisorFactura);
                                                        mr.setEmisorTipoIdentificacion(emisorTipoIdentificacion);
                                                        mr.setEmisorIdentificacion(emisorIdentificacion);
                                                        mr.setFechaEmision(fechaEmision);
                                                        mr.setTotalImpuestos(totalImpuestos);
                                                        mr.setTotalComprobante(totalComprobante);
                                                        mr.setReceptorTipoIdentificacion(receptorTipoIdentificacion);
                                                        mr.setReceptorIdentificacion(receptorIdentificacion);
                                                        mr.setFechaCreacion(new Date());
                                                        mr.setFacturaPdf(fileName);
                                                        mr.setFacturaXml(facturaXmlZip);
                                                        mr.setMoneda(moneda);
                                                        mr.setTipoCambio(tipoCambio);
                                                        mr.setEstado("P");
                                                        this._mrService.save(mr);
                                                    } catch (Exception e) {
                                                        this.log.info("Notifico a " + enviarA + " que ya la factura existe " + claveFactura + emisorFactura);
                                                        String empresaSaluda = "Soluciones InformMata";
                                                        String asunto = "Notificacidel sistema de recepciautom- La Factura Electrgenerada por " + emisorFactura + ", ya fue recibida anteriormente.";
                                                        Date _fechaEmision_ = formato.parse(fechaEmision);
                                                        this.log.info("Se enviara una notificacia :" + enviarA);
                                                    }
                                                    facturaPdfZip = "";
                                                }
                                            }
                                        }
                                    }
                                    if (extension.equalsIgnoreCase("xml")) {
                                        Document xml = XmlHelper.getDocument(rutaAchivoGuardado);
                                        claveFactura = getNameFieldXml(xPath, xml, "Clave");
                                        fechaEmision = getNameFieldXml(xPath, xml, "FechaEmision");
                                        emisorFactura = getNameFieldXml(xPath, xml, "Emisor/Nombre");
                                        emisorTipoIdentificacion = getNameFieldXml(xPath, xml, "Emisor/Identificacion/Tipo");
                                        emisorIdentificacion = getNameFieldXml(xPath, xml, "Emisor/Identificacion/Numero");
                                        try {
                                            moneda = getNameFieldXml(xPath, xml, "ResumenFactura/CodigoTipoMoneda/CodigoMoneda");
                                            tipoCambio = getNameFieldXml(xPath, xml, "ResumenFactura/CodigoTipoMoneda/TipoCambio");
                                        } catch (Exception e) {
                                            moneda = "CRC";
                                            tipoCambio = "1.00";
                                        }
                                        try {
                                            totalImpuestos = getNameFieldXml(xPath, xml, "ResumenFactura/TotalImpuesto");
                                        } catch (Exception e) {
                                            totalImpuestos = "0.00";
                                        }
                                        totalComprobante = getNameFieldXml(xPath, xml, "ResumenFactura/TotalComprobante");
                                        receptorTipoIdentificacion = getNameFieldXml(xPath, xml, "Receptor/Identificacion/Tipo");
                                        receptorIdentificacion = getNameFieldXml(xPath, xml, "Receptor/Identificacion/Numero");
                                        if (claveFactura.length() > 30) {
                                            File file = new File(rutaAchivoGuardado);
                                            String nameFe = "fe" + fileName;
                                            File file2 = new File(saveDirectory + File.separator + nameFe);
                                            boolean success = file.renameTo(file2);
                                            if (!success);
                                            facturaXml = nameFe;
                                            try {
                                                FEMensajeReceptorAutomatico mr = new FEMensajeReceptorAutomatico();
                                                mr.setClave(claveFactura);
                                                mr.setCorreoFrom(enviarA);
                                                mr.setEmisorFactura(emisorFactura);
                                                mr.setEmisorTipoIdentificacion(emisorTipoIdentificacion);
                                                mr.setEmisorIdentificacion(emisorIdentificacion);
                                                mr.setFechaEmision(fechaEmision);
                                                mr.setTotalImpuestos(totalImpuestos);
                                                mr.setTotalComprobante(totalComprobante);
                                                mr.setReceptorTipoIdentificacion(receptorTipoIdentificacion);
                                                mr.setReceptorIdentificacion(receptorIdentificacion);
                                                mr.setFechaCreacion(new Date());
                                                mr.setFacturaPdf(prefijo + "sinmata.pdf");
                                                mr.setFacturaXml(facturaXml);
                                                mr.setMoneda(moneda);
                                                mr.setTipoCambio(tipoCambio);
                                                mr.setEstado("P");
                                                this._mrService.save(mr);
                                            } catch (Exception e) {
                                                this.log.info("Notifico a " + enviarA + " que ya la factura existe " + claveFactura + emisorFactura);
                                                String empresaSaluda = "Soluciones InformMata";
                                                String asunto = "Notificacidel sistema de recepciautom- La Factura Electrgenerada por " + emisorFactura + ", ya fue recibida anteriormente.";
                                                Date _fechaEmision_ = formato.parse(fechaEmision);
                                                this.log.info("Se enviara una notificacia :" + enviarA);
                                                enviaNotificacionMR(claveFactura, emisorFactura, empresaSaluda, formato1.format(_fechaEmision_), totalComprobante, enviarA, asunto);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    this.log.info("No corresponde a un correo de factura electróniuca");
                                }
                            }
                        } else {
                            messageContent = part.getContent().toString();
                        }
                    }
                    if (attachFiles.length() > 1)
                        attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
                } else if (contentType.contains("application/xml") || contentType.contains("APPLICATION/XML") || contentType.contains("application/pdf") || contentType.contains("APPLICATION/PDF")) {
                    Object content = message.getContent();
                    if (content != null)
                        messageContent = content.toString();
                }
                try {
                    if (claveFactura.equals("") || claveFactura.trim().length() > 30);
                } catch (Exception exception) {}
            }
            folderInbox.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            this.log.info("NoSuchProviderException mr inbox" + ex.getMessage());
        } catch (MessagingException ex) {
            this.log.info("Could not connect to the message store " + ex.getMessage());
        } catch (IOException ex) {
            this.log.info("Otro error generado por el MR inbox " + ex.getMessage());
        }
    }

    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData)child;
            return cd.getData();
        }
        return "";
    }

    public void enviaNotificacionMR(String clave, String emisorFactura, String empresaSaluda, String fechaEmision, String totalComprobante, String emailTo, String asunto) throws IOException, SQLException, MessagingException {
        try {
            MimeMessage message = this.emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String msj = "";
            String consecutivo = clave.substring(21, 41);
            msj = msj + "<p style=\"font-family: Arial;\">Estimado cliente,</p>";
            msj = msj + "<p style=\"font-family: Arial;\">El comprobante de Factura Electrcon la consecutivo: <b>" + consecutivo + "</b>, generada por <b>" + emisorFactura + "</b> el <b>" + fechaEmision + "</b> por un monto de <b>" + totalComprobante + "</b> ya fue recibida anteriormente.</b></p>";
            msj = msj + "<p style=\"font-family: Arial;\">Este correo se genero de forma autompor favor no responder.</b></p>";
            msj = msj + "<p style=\"font-family: Arial;\">Saludos,</p>";
            msj = msj + "<p style=\"font-family: Arial;\"><b>" + empresaSaluda + "</b></p>";
            helper.setTo(new InternetAddress(emailTo.trim(), "Notificacidel sistema de recepciautomatico - " + empresaSaluda));
            helper.setFrom(new InternetAddress(this.correoDistribucion, "Notificacidel sistema de recepciautomatico"));
            helper.setSubject(asunto);
            helper.setText(msj, true);
            try {
                this.emailSender.send(message);
                this.log.info("Se envun mail a " + emailTo);
            } catch (Exception e) {
                this.log.info("No se pudo enviar el correo a " + emailTo);
                e.printStackTrace();
            }
        } catch (Exception ex) {
            System.out.println("Error del reporte: " + ex.getMessage());
        }
    }

    private String getFileName(Part part) throws MessagingException, UnsupportedEncodingException {
        if (!(part instanceof MimeBodyPart))
            return part.getFileName();
        String contentType = part.getContentType();
        String ret = null;
        try {
            ret = MimeUtility.decodeText(part.getFileName());
            if (contentType.contains("application/xml") || contentType.contains("APPLICATION/XML"))
                ret = "sinmata.xml";
            if (contentType.contains("application/pdf") || contentType.contains("APPLICATION/PDF"))
                ret = "sinmata.pdf";
        } catch (NullPointerException ex) {
            if (contentType.contains("application/xml") || contentType.contains("APPLICATION/XML"))
                ret = "sinmata.xml";
            if (contentType.contains("application/pdf") || contentType.contains("APPLICATION/PDF"))
                ret = "sinmata.pdf";
        }
        return (ret == null) ? "" : ret;
    }

    private String getNameFieldXml(XPath xPath, Document xml, String field) {
        String j = "";
        NodeList fe = null, nc = null;
        try {
            try {
                fe = (NodeList)xPath.evaluate("/FacturaElectronica/" + field, xml.getDocumentElement(), XPathConstants.NODESET);
                j = fe.item(0).getTextContent();
                this.log.info("FE _______________________________ " + j);
            } catch (Exception exception) {}
            try {
                nc = (NodeList)xPath.evaluate("/NotaCreditoElectronica/" + field, xml.getDocumentElement(), XPathConstants.NODESET);
                j = nc.item(0).getTextContent();
                this.log.info("NC _______________________________ " + j);
            } catch (Exception exception) {}
        } catch (Exception e) {
            j = "";
        }
        return j;
    }
}