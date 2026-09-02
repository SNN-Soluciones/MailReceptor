package digital.samyx.mailreceptor.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("esHostImapPermitido: solo hostnames públicos")
class EmailServiceImplTest {

    @ParameterizedTest
    @ValueSource(strings = {"imap.gmail.com", "outlook.office365.com", "mail.miempresa.co.cr", "IMAP.Ejemplo.COM"})
    @DisplayName("acepta nombres de dominio públicos")
    void aceptaHostsPublicos(String host) {
        assertThat(EmailServiceImpl.esHostImapPermitido(host)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "10.0.0.5", "192.168.1.20", "127.0.0.1", "169.254.169.254", // IPs literales (red interna / metadata)
            "localhost", "redis.localhost", "servidor.local", "db.internal", "nas.lan",
            "sinpunto", ".gmail.com", "imap.gmail.com.", "imap gmail.com", "imap.gmail.com/../x", ""
    })
    @DisplayName("rechaza IPs literales, hosts internos y valores malformados")
    void rechazaHostsInternosOMalformados(String host) {
        assertThat(EmailServiceImpl.esHostImapPermitido(host)).isFalse();
    }
}
