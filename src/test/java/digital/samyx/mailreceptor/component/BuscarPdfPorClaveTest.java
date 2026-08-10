package digital.samyx.mailreceptor.component;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("buscarPdfPorClave: emparejar el PDF con la factura del correo")
class BuscarPdfPorClaveTest {

    /**
     * Clave de Hacienda de 50 dígitos: 506 país + 100826 fecha + 12 de cédula del
     * emisor + 20 de consecutivo (posiciones 21..40) + 1 situación + 8 de seguridad.
     */
    private static final String CLAVE = "50610082631012345678900100001010000000123199999999";
    private static final String CONSECUTIVO = "00100001010000000123";

    private final MensajeReceptorAutomatico receptor = new MensajeReceptorAutomatico();

    @Test
    @DisplayName("la clave de prueba respeta el formato de Hacienda")
    void laClaveDePruebaEsRealista() {
        assertThat(CLAVE).hasSize(50);
        assertThat(CLAVE.substring(21, 41)).isEqualTo(CONSECUTIVO);
    }

    private Map<String, byte[]> pdfs(String... nombres) {
        Map<String, byte[]> pdfFiles = new LinkedHashMap<>();
        for (String nombre : nombres) {
            pdfFiles.put(nombre, nombre.getBytes(StandardCharsets.UTF_8));
        }
        return pdfFiles;
    }

    @Test
    @DisplayName("sin PDFs adjuntos devuelve null")
    void sinPdfsDevuelveNull() {
        assertThat(receptor.buscarPdfPorClave(CLAVE, Map.of())).isNull();
    }

    @Test
    @DisplayName("encuentra el PDF nombrado con la clave completa")
    void encuentraPorClaveCompleta() {
        Map<String, byte[]> pdfFiles = pdfs("otra-cosa.pdf", CLAVE.toLowerCase() + ".pdf");

        assertThat(receptor.buscarPdfPorClave(CLAVE, pdfFiles))
                .isEqualTo(pdfFiles.get(CLAVE.toLowerCase() + ".pdf"));
    }

    @Test
    @DisplayName("encuentra el PDF nombrado solo con el consecutivo")
    void encuentraPorConsecutivo() {
        Map<String, byte[]> pdfFiles = pdfs("otra-cosa.pdf", "factura-" + CONSECUTIVO + ".pdf");

        assertThat(receptor.buscarPdfPorClave(CLAVE, pdfFiles))
                .isEqualTo(pdfFiles.get("factura-" + CONSECUTIVO + ".pdf"));
    }

    @Test
    @DisplayName("si viene un solo PDF lo usa aunque el nombre no calce")
    void usaElUnicoPdfDisponible() {
        Map<String, byte[]> pdfFiles = pdfs("comprobante.pdf");

        assertThat(receptor.buscarPdfPorClave(CLAVE, pdfFiles))
                .isEqualTo(pdfFiles.get("comprobante.pdf"));
    }

    @Test
    @DisplayName("con varios PDFs que no calzan no adivina")
    void conVariosPdfsQueNoCalzanDevuelveNull() {
        assertThat(receptor.buscarPdfPorClave(CLAVE, pdfs("uno.pdf", "dos.pdf"))).isNull();
    }

    @Test
    @DisplayName("una clave más corta de lo esperado no revienta")
    void claveCortaNoRevienta() {
        // Antes reventaba con StringIndexOutOfBounds y el correo quedaba atascado
        // reintentándose cada ciclo.
        for (String claveCorta : new String[]{"", "506", "5061008263101234567890"}) {
            assertThatCode(() -> receptor.buscarPdfPorClave(claveCorta, pdfs("uno.pdf", "dos.pdf")))
                    .doesNotThrowAnyException();
        }
    }
}
