package digital.samyx.mailreceptor.enums;

import java.util.Collection;

/**
 * Qué pasó al procesar un correo del buzón receptor. Decide si el correo queda
 * LEÍDO o NO LEÍDO en el buzón del cliente.
 *
 * La regla es simple: el correo solo se marca como leído cuando su factura llegó
 * al POS. Todo lo demás (no traía factura, la factura es de otra empresa, algo
 * falló) se deja NO LEÍDO para que quede visible en el buzón y el próximo ciclo
 * lo vuelva a intentar.
 */
public enum ResultadoMensaje {

    /** Se envió al POS al menos una factura del correo (nueva o ya registrada). */
    PROCESADO("la factura se envió al POS"),

    /** El correo no traía ninguna factura electrónica (mensajes de Hacienda, adjuntos sin XML, etc.). */
    SIN_FACTURA("no se encontró ninguna factura en el correo"),

    /** Traía factura(s), pero el receptor no es la empresa dueña del buzón. */
    NO_CORRESPONDE("la factura no pertenece a esta empresa"),

    /** Falló algo técnico: POS caído, XML ilegible, no se pudo validar la empresa. */
    ERROR("falló el procesamiento, se reintenta en el próximo ciclo");

    private final String descripcion;

    ResultadoMensaje(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Único caso en que el correo se marca como leído. */
    public boolean debeMarcarseLeido() {
        return this == PROCESADO;
    }

    /**
     * Resume en un solo resultado los de todos los XML de un mismo correo, con
     * este orden de prioridad:
     *
     * <ol>
     *   <li>ERROR — si algo falló, el correo se deja no leído aunque otra factura
     *       del mismo correo sí haya entrado; reenviarla es inofensivo porque el
     *       POS detecta las duplicadas.</li>
     *   <li>PROCESADO — al menos una factura entró y ninguna falló.</li>
     *   <li>NO_CORRESPONDE — todas las facturas eran de otra empresa.</li>
     *   <li>SIN_FACTURA — no había nada que procesar (también si la lista viene vacía).</li>
     * </ol>
     */
    public static ResultadoMensaje consolidar(Collection<ResultadoMensaje> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return SIN_FACTURA;
        }
        if (resultados.contains(ERROR)) {
            return ERROR;
        }
        if (resultados.contains(PROCESADO)) {
            return PROCESADO;
        }
        if (resultados.contains(NO_CORRESPONDE)) {
            return NO_CORRESPONDE;
        }
        return SIN_FACTURA;
    }
}
