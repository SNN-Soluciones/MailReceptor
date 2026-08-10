package digital.samyx.mailreceptor.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResultadoMensaje: decide si el correo queda leído o no leído")
class ResultadoMensajeTest {

    @Test
    @DisplayName("solo queda leído lo que ya no necesita que nadie lo revise")
    void soloSeMarcaLeidoLoQueNoNecesitaRevision() {
        assertThat(ResultadoMensaje.PROCESADO.debeMarcarseLeido()).isTrue();
        assertThat(ResultadoMensaje.MENSAJE_SISTEMA.debeMarcarseLeido()).isTrue();
        assertThat(ResultadoMensaje.SIN_FACTURA.debeMarcarseLeido()).isFalse();
        assertThat(ResultadoMensaje.NO_CORRESPONDE.debeMarcarseLeido()).isFalse();
        assertThat(ResultadoMensaje.ERROR.debeMarcarseLeido()).isFalse();
    }

    @Test
    @DisplayName("un correo sin XML se considera sin factura")
    void correoSinAdjuntosProcesablesEsSinFactura() {
        assertThat(ResultadoMensaje.consolidar(List.of())).isEqualTo(ResultadoMensaje.SIN_FACTURA);
        assertThat(ResultadoMensaje.consolidar(null)).isEqualTo(ResultadoMensaje.SIN_FACTURA);
    }

    @Test
    @DisplayName("un correo con puros acuses de Hacienda se marca leído")
    void soloAcusesDeHaciendaSeMarcaLeido() {
        assertThat(ResultadoMensaje.consolidar(
                List.of(ResultadoMensaje.MENSAJE_SISTEMA, ResultadoMensaje.MENSAJE_SISTEMA)))
                .isEqualTo(ResultadoMensaje.MENSAJE_SISTEMA);
    }

    @Test
    @DisplayName("basta un adjunto no reconocido para que el correo quede no leído")
    void unAdjuntoDesconocidoJuntoAUnAcuseDejaElCorreoSinLeer() {
        ResultadoMensaje resultado = ResultadoMensaje.consolidar(
                List.of(ResultadoMensaje.MENSAJE_SISTEMA, ResultadoMensaje.SIN_FACTURA));

        assertThat(resultado).isEqualTo(ResultadoMensaje.SIN_FACTURA);
        assertThat(resultado.debeMarcarseLeido()).isFalse();
    }

    @Test
    @DisplayName("una factura junto a un acuse manda sobre el acuse")
    void laFacturaMandaSobreElAcuse() {
        assertThat(ResultadoMensaje.consolidar(
                List.of(ResultadoMensaje.MENSAJE_SISTEMA, ResultadoMensaje.PROCESADO)))
                .isEqualTo(ResultadoMensaje.PROCESADO);
    }

    @Test
    @DisplayName("una factura de otra empresa deja el correo no leído")
    void facturaDeOtraEmpresaNoCorresponde() {
        assertThat(ResultadoMensaje.consolidar(
                List.of(ResultadoMensaje.NO_CORRESPONDE, ResultadoMensaje.SIN_FACTURA)))
                .isEqualTo(ResultadoMensaje.NO_CORRESPONDE);
    }

    @Test
    @DisplayName("basta una factura enviada al POS para marcar el correo como leído")
    void unaFacturaProcesadaMarcaElCorreo() {
        assertThat(ResultadoMensaje.consolidar(
                List.of(ResultadoMensaje.SIN_FACTURA, ResultadoMensaje.PROCESADO,
                        ResultadoMensaje.NO_CORRESPONDE)))
                .isEqualTo(ResultadoMensaje.PROCESADO);
    }

    @Test
    @DisplayName("si algo falló, el error manda aunque otra factura sí haya entrado")
    void elErrorTienePrioridadSobreProcesado() {
        assertThat(ResultadoMensaje.consolidar(
                List.of(ResultadoMensaje.PROCESADO, ResultadoMensaje.ERROR)))
                .isEqualTo(ResultadoMensaje.ERROR);
    }

    @Test
    @DisplayName("todos los resultados tienen un motivo para el log")
    void todosTienenDescripcion() {
        for (ResultadoMensaje resultado : ResultadoMensaje.values()) {
            assertThat(resultado.getDescripcion()).isNotBlank();
        }
    }
}
