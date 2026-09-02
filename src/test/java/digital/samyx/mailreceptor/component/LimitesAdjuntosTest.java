package digital.samyx.mailreceptor.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("leerBytesAcotado: los adjuntos no pueden agotar la memoria del worker")
class LimitesAdjuntosTest {

    @Test
    @DisplayName("devuelve el contenido completo cuando cabe en el tope")
    void devuelveContenidoSiCabe() throws IOException {
        byte[] datos = new byte[10_000];
        datos[9_999] = 7;
        byte[] leido = MensajeReceptorAutomatico.leerBytesAcotado(new ByteArrayInputStream(datos), 10_000);
        assertThat(leido).isEqualTo(datos);
    }

    @Test
    @DisplayName("devuelve null y deja de leer cuando el contenido supera el tope")
    void devuelveNullSiSupera() throws IOException {
        // Stream "infinito": si el tope no cortara la lectura, este test no terminaría.
        InputStream infinito = new InputStream() {
            @Override
            public int read() {
                return 1;
            }
        };
        assertThat(MensajeReceptorAutomatico.leerBytesAcotado(infinito, 1_000_000)).isNull();
    }

    @Test
    @DisplayName("los topes tienen holgura para una factura real y freno para una bomba")
    void topesRazonables() {
        assertThat(MensajeReceptorAutomatico.MAX_BYTES_ADJUNTO).isBetween(1L << 20, 64L << 20);
        assertThat(MensajeReceptorAutomatico.MAX_BYTES_DESCOMPRIMIDOS_ZIP)
                .isGreaterThanOrEqualTo(MensajeReceptorAutomatico.MAX_BYTES_ADJUNTO);
        assertThat(MensajeReceptorAutomatico.MAX_ENTRADAS_ZIP).isBetween(10, 1000);
    }
}
