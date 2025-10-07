package digital.samyx.mailreceptor.enums.mh;

import lombok.Getter;

/**
 * Tipos de identificación según normativa costarricense
 */
@Getter
public enum TipoIdentificacion {

    CEDULA_FISICA("01", "Cédula Física", 9),
    CEDULA_JURIDICA("02", "Cédula Jurídica", 10),
    DIMEX("03", "DIMEX", 12),
    NITE("04", "NITE", 10),
    EXTRANJERO("05", "Extranjero", 20);

    private final String codigo;
    private final String descripcion;
    private final int longitudMaxima;

    TipoIdentificacion(String codigo, String descripcion, int longitudMaxima) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.longitudMaxima = longitudMaxima;
    }

    /**
     * Valida el formato de la identificación según el tipo
     */
    public boolean esValido(String identificacion) {
        if (identificacion == null || identificacion.trim().isEmpty()) {
            return false;
        }

        identificacion = identificacion.trim().replaceAll("-", "");

        switch (this) {
            case CEDULA_FISICA:
                return identificacion.matches("^[1-9]\\d{8}$");

            case CEDULA_JURIDICA:
                return identificacion.matches("^[1-9]\\d{9}$");

            case DIMEX:
                return identificacion.matches("^[1-9]\\d{10,11}$");

            case NITE:
                return identificacion.matches("^\\d{10}$");

            case EXTRANJERO:
                return identificacion.length() <= longitudMaxima;

            default:
                return false;
        }
    }

    /**
     * Formatea la identificación según el tipo
     */
    public String formatear(String identificacion) {
        if (identificacion == null) return null;

        identificacion = identificacion.trim().replaceAll("-", "");

        switch (this) {
            case CEDULA_FISICA:
                // Formato: 1-0234-5678
                if (identificacion.length() == 9) {
                    return identificacion.substring(0, 1) + "-" +
                        identificacion.substring(1, 5) + "-" +
                        identificacion.substring(5);
                }
                break;

            case CEDULA_JURIDICA:
                // Formato: 3-101-234567
                if (identificacion.length() == 10) {
                    return identificacion.substring(0, 1) + "-" +
                        identificacion.substring(1, 4) + "-" +
                        identificacion.substring(4);
                }
                break;
        }

        return identificacion;
    }

    /**
     * Obtiene el tipo por código
     */
    public static TipoIdentificacion porCodigo(String codigo) {
        for (TipoIdentificacion tipo : values()) {
            if (tipo.getCodigo().equals(codigo)) {
                return tipo;
            }
        }
        throw new IllegalArgumentException("Código de identificación no válido: " + codigo);
    }
}