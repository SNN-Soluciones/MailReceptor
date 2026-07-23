package digital.samyx.mailreceptor.dto;

import lombok.Data;
import lombok.ToString;

/**
 * Config de un buzón receptor, obtenida del POS por HTTP
 * (GET /api/facturas-recepcion/smtp-configs). Reemplaza a la antigua entidad
 * JPA: el MailReceptor ya no se conecta a Postgres.
 *
 * smtpPassword es la credencial del buzón; se excluye del toString para no
 * filtrarla en logs.
 */
@Data
@ToString(exclude = {"smtpPassword"})
public class ReceptorSmtpConfig {
    private Long empresaId;
    private Long sucursalId;
    private String email;
    private String smtpPassword;
    private String emailDomain;
}
