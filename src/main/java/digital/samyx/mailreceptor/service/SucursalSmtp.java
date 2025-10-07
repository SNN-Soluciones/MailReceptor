package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.entity.SucursalReceptorSmtp;

import java.util.List;
import java.util.Optional;

public interface SucursalSmtp {
    Optional<SucursalReceptorSmtp> findById(Long id);
    SucursalReceptorSmtp save(SucursalReceptorSmtp emisor);
    List<SucursalReceptorSmtp> findAll();

}
