package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.entity.EmisorSMTP;

import java.util.List;
import java.util.Optional;

public interface IEmisoresSMTPService {
    Optional<EmisorSMTP> findById(Long id);
    EmisorSMTP save(EmisorSMTP emisor);
    List<EmisorSMTP> findAll();

}
