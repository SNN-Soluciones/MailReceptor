package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.entity.SucursalReceptorSmtp;
import digital.samyx.mailreceptor.repository.IEmisoresSMTPDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SucursalSmtpImpl implements SucursalSmtp {

    @Autowired
    private IEmisoresSMTPDao repository;

    @Override
    public Optional<SucursalReceptorSmtp> findById(Long id) {
        return repository.findById(id);
    }
    @Override
    public List<SucursalReceptorSmtp> findAll() {
        return repository.findAll();
    }


    @Override
    public SucursalReceptorSmtp save(SucursalReceptorSmtp emisor) {
        return repository.save(emisor);
    }

    @Override
    public List<SucursalReceptorSmtp> findAllActivos() {
        return repository.findByActivoTrueAndProcesarAutomaticamenteTrue();
    }
}