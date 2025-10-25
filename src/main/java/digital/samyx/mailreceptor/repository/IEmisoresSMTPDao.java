package digital.samyx.mailreceptor.repository;

import digital.samyx.mailreceptor.entity.SucursalReceptorSmtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IEmisoresSMTPDao extends JpaRepository<SucursalReceptorSmtp, Long> {
    List<SucursalReceptorSmtp> findByActivoTrueAndProcesarAutomaticamenteTrue();
}
