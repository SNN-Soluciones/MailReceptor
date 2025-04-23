package digital.samyx.mailreceptor.repository;

import digital.samyx.mailreceptor.entity.EmisorSMTP;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEmisoresSMTPDao extends JpaRepository<EmisorSMTP, Long> {

}
