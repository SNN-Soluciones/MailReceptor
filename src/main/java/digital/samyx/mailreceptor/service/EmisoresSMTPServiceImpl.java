package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.entity.EmisorSMTP;
import digital.samyx.mailreceptor.repository.IEmisoresSMTPDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmisoresSMTPServiceImpl implements IEmisoresSMTPService {

    @Autowired
    private IEmisoresSMTPDao repository;

    @Override
    public Optional<EmisorSMTP> findById(Long id) {
        return repository.findById(id);
    }
    @Override
    public List<EmisorSMTP> findAll() {
        return repository.findAll();
    }


    @Override
    public EmisorSMTP save(EmisorSMTP emisor) {
        return repository.save(emisor);
    }
}