package digital.samyx.mailreceptor.enums;

import java.util.Collection;

/**
 * Qué pasó al procesar un correo del buzón receptor. Decide si el correo queda
 * LEÍDO o NO LEÍDO en el buzón del cliente.
 *
 * La regla es: el correo se marca como leído cuando ya no necesita ojo humano —
 * porque su factura llegó al POS, o porque es un mensaje de sistema de Hacienda
 * que nadie tiene que leer. Todo lo demás (no traía factura, la factura es de
 * otra empresa, algo falló) se deja NO LEÍDO para que quede visible en el buzón
 * y el próximo ciclo lo vuelva a intentar.
 */
public enum ResultadoMensaje {

    /** Se envió al POS al menos una factura del correo (nueva o ya registrada). */
    PROCESADO("la factura se envió al POS", true),

    /** Solo traía acuses de Hacienda (MensajeHacienda/MensajeReceptor/ConfirmacionComprobante). */
    MENSAJE_SISTEMA("es un acuse de Hacienda, no una factura que registrar", true),

    /** El correo no traía ninguna factura electrónica: sin adjuntos, solo PDF, o XML desconocido. */
    SIN_FACTURA("no se encontró ninguna factura en el correo", false),

    /** Traía factura(s), pero el receptor no es la empresa dueña del buzón. */
    NO_CORRESPONDE("la factura no pertenece a esta empresa", false),

    /** Falló algo técnico: POS caído, XML ilegible, no se pudo validar la empresa. */
    ERROR("falló el procesamiento, se reintenta en el próximo ciclo", false);

    private final String descripcion;
    private final boolean marcarLeido;

    ResultadoMensaje(String descripcion, boolean marcarLeido) {
        this.descripcion = descripcion;
        this.marcarLeido = marcarLeido;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Solo se marca leído lo que ya no necesita que nadie lo revise. */
    public boolean debeMarcarseLeido() {
        return marcarLeido;
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
     *   <li>NO_CORRESPONDE — las facturas eran de otra empresa.</li>
     *   <li>SIN_FACTURA — había algo que no se pudo reconocer como factura (también
     *       si el correo no traía XML del todo, o sea si la lista viene vacía).</li>
     *   <li>MENSAJE_SISTEMA — TODOS los adjuntos eran acuses de Hacienda; basta un
     *       adjunto no reconocido para que el correo quede no leído.</li>
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
        if (resultados.contains(SIN_FACTURA)) {
            return SIN_FACTURA;
        }
        return MENSAJE_SISTEMA;
    }
}
