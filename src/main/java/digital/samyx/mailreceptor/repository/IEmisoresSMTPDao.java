package digital.samyx.mailreceptor.repository;

import digital.samyx.mailreceptor.entity.SucursalReceptorSmtp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEmisoresSMTPDao extends JpaRepository<SucursalReceptorSmtp, Long> {

}
