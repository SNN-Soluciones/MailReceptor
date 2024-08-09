package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.entity.FEMensajeReceptorAutomatico;
import digital.samyx.mailreceptor.repository.IFEMensajeReceptorAutomaticoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FEMensajeReceptorAutomaticoServiceImpl implements IFEMensajeReceptorAutomaticoService {
    @Autowired
    private IFEMensajeReceptorAutomaticoDao _dao;

    public void save(FEMensajeReceptorAutomatico entity) {
        this._dao.save(entity);
    }

    public FEMensajeReceptorAutomatico findAll() {
        return null;
    }
}

