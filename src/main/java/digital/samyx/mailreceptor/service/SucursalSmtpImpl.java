package digital.samyx.mailreceptor.service;

import digital.samyx.mailreceptor.dto.ReceptorSmtpConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Lee las configuraciones de buzones del POS por HTTP
 * (GET /api/facturas-recepcion/smtp-configs, protegido por X-API-Key), en vez de
 * consultar Postgres directamente. Así el MailReceptor queda sin dependencia de
 * la base de datos.
 */
@Service
@Slf4j
public class SucursalSmtpImpl implements SucursalSmtp {

    private final RestTemplate restTemplate;

    @Value("${nathbit.api.url}")
    private String nathbitApiUrl;

    @Value("${nathbit.api.api-key}")
    private String nathbitApiKey;

    public SucursalSmtpImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<ReceptorSmtpConfig> findAllActivos() {
        String url = nathbitApiUrl + "/api/facturas-recepcion/smtp-configs";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", nathbitApiKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<List<ReceptorSmtpConfig>> response = restTemplate.exchange(
                    url, HttpMethod.GET, request,
                    new ParameterizedTypeReference<List<ReceptorSmtpConfig>>() {});

            List<ReceptorSmtpConfig> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (Exception e) {
            // Sin configs este ciclo; el @Scheduled reintenta en el próximo (10 min).
            log.error("❌ No se pudieron obtener las configs SMTP desde {}: {}", url, e.getMessage());
            return Collections.emptyList();
        }
    }
}
