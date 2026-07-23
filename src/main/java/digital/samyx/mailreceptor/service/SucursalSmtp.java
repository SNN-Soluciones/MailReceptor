package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.dto.ReceptorSmtpConfig;

import java.util.List;

public interface SucursalSmtp {
    /** Buzones activos con procesamiento automático (leídos del POS por HTTP). */
    List<ReceptorSmtpConfig> findAllActivos();
}
