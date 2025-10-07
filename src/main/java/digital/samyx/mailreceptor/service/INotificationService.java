package digital.samyx.mailreceptor.service;

public interface INotificationService {
    void sendDuplicateNotification(String clave, String emisorFactura, 
                                  String fechaEmision, String totalComprobante, 
                                  String emailTo) throws Exception;
}