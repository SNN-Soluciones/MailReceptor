package digital.samyx.mailreceptor.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

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
    @DisplayName("dos adjuntos con el mismo nombre conviven: el segundo no pisa al primero")
    void nombresRepetidosNoSePisan() {
        Map<String, byte[]> destino = new LinkedHashMap<>();
        byte[] primero = {1};
        byte[] segundo = {2};
        byte[] tercero = {3};

        assertThat(MensajeReceptorAutomatico.claveUnica(destino, "factura.xml", primero))
                .isEqualTo("factura.xml");
        assertThat(MensajeReceptorAutomatico.claveUnica(destino, "factura.xml", segundo))
                .isEqualTo("factura-2.xml");
        assertThat(MensajeReceptorAutomatico.claveUnica(destino, "factura.xml", tercero))
                .isEqualTo("factura-3.xml");

        assertThat(destino).hasSize(3);
        assertThat(destino.get("factura.xml")).isEqualTo(primero);
        assertThat(destino.get("factura-2.xml")).isEqualTo(segundo);
        // La extensión se conserva: el resto del flujo decide por ella.
        assertThat(destino.keySet()).allMatch(k -> k.endsWith(".xml"));
    }

    @Test
    @DisplayName("un nombre sin extensión también recibe sufijo único")
    void nombreSinExtension() {
        Map<String, byte[]> destino = new LinkedHashMap<>();
        MensajeReceptorAutomatico.claveUnica(destino, "adjunto", new byte[]{1});
        assertThat(MensajeReceptorAutomatico.claveUnica(destino, "adjunto", new byte[]{2}))
                .isEqualTo("adjunto-2");
    }

    @Test
    @DisplayName("los topes tienen holgura para una factura real y freno para una bomba")
    void topesRazonables() {
        assertThat(MensajeReceptorAutomatico.MAX_BYTES_ADJUNTO).isBetween(1L << 20, 64L << 20);
        assertThat(MensajeReceptorAutomatico.MAX_BYTES_DESCOMPRIMIDOS_ZIP)
                .isGreaterThanOrEqualTo(MensajeReceptorAutomatico.MAX_BYTES_ADJUNTO);
        assertThat(MensajeReceptorAutomatico.MAX_ENTRADAS_ZIP).isBetween(10, 1000);
        // El techo por correo manda sobre los otros dos: un correo con varios
        // ZIP no puede sumar más que esto aunque ninguno se pase por su cuenta.
        assertThat(MensajeReceptorAutomatico.MAX_BYTES_MENSAJE)
                .isGreaterThanOrEqualTo(MensajeReceptorAutomatico.MAX_BYTES_DESCOMPRIMIDOS_ZIP);
    }
}
