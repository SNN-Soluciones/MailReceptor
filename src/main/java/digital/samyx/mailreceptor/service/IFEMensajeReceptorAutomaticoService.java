package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.entity.FEMensajeReceptorAutomatico;

public interface IFEMensajeReceptorAutomaticoService {
    void save(FEMensajeReceptorAutomatico paramFEMensajeReceptorAutomatico);

    FEMensajeReceptorAutomatico findAll();
}
