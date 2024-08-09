package digital.samyx.mailreceptor.entity;

import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;

@Entity
@Table(name = "fe_mensaje_receptor_automatico", uniqueConstraints = {@UniqueConstraint(columnNames = {"receptor_tipo_identificacion", "receptor_identificacion", "clave"})})
public class FEMensajeReceptorAutomatico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo_from", length = 255)
    private String correoFrom;

    @Column(name = "emisor_factura", length = 160)
    private String emisorFactura;

    @Column(name = "emisor_tipo_identificacion", length = 2)
    private String emisorTipoIdentificacion;

    @Column(name = "emisor_identificacion", length = 20)
    private String emisorIdentificacion;

    @Column(name = "fecha_emision", length = 45)
    private String fechaEmision;

    @Column(length = 5)
    private String moneda;

    @Column(name = "tipo_cambio", length = 10)
    private String tipoCambio;

    @Column(name = "total_impuestos", length = 20)
    private String totalImpuestos;

    @Column(name = "total_comprobante", length = 20)
    private String totalComprobante;

    @Column(name = "receptor_tipo_identificacion", length = 2)
    private String receptorTipoIdentificacion;

    @Column(name = "receptor_identificacion", length = 25)
    private String receptorIdentificacion;

    @Column(length = 50)
    private String clave;

    @Column(name = "factura_xml", length = 250)
    private String facturaXml;

    @Column(name = "factura_pdf", length = 250)
    private String facturaPdf;

    @NotNull
    @Column(name = "fecha_creacion")
    @Temporal(TemporalType.DATE)
    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date fechaCreacion;

    @Column(length = 1)
    private String estado;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCorreoFrom() {
        return this.correoFrom;
    }

    public void setCorreoFrom(String correoFrom) {
        this.correoFrom = correoFrom;
    }

    public String getEmisorFactura() {
        return this.emisorFactura;
    }

    public void setEmisorFactura(String emisorFactura) {
        this.emisorFactura = emisorFactura;
    }

    public String getEmisorTipoIdentificacion() {
        return this.emisorTipoIdentificacion;
    }

    public void setEmisorTipoIdentificacion(String emisorTipoIdentificacion) {
        this.emisorTipoIdentificacion = emisorTipoIdentificacion;
    }

    public String getEmisorIdentificacion() {
        return this.emisorIdentificacion;
    }

    public void setEmisorIdentificacion(String emisorIdentificacion) {
        this.emisorIdentificacion = emisorIdentificacion;
    }

    public String getMoneda() {
        return this.moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getTipoCambio() {
        return this.tipoCambio;
    }

    public void setTipoCambio(String tipoCambio) {
        this.tipoCambio = tipoCambio;
    }

    public String getFechaEmision() {
        return this.fechaEmision;
    }

    public void setFechaEmision(String fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getTotalImpuestos() {
        return this.totalImpuestos;
    }

    public void setTotalImpuestos(String totalImpuestos) {
        this.totalImpuestos = totalImpuestos;
    }

    public String getTotalComprobante() {
        return this.totalComprobante;
    }

    public void setTotalComprobante(String totalComprobante) {
        this.totalComprobante = totalComprobante;
    }

    public String getReceptorTipoIdentificacion() {
        return this.receptorTipoIdentificacion;
    }

    public void setReceptorTipoIdentificacion(String receptorTipoIdentificacion) {
        this.receptorTipoIdentificacion = receptorTipoIdentificacion;
    }

    public String getReceptorIdentificacion() {
        return this.receptorIdentificacion;
    }

    public void setReceptorIdentificacion(String receptorIdentificacion) {
        this.receptorIdentificacion = receptorIdentificacion;
    }

    public String getClave() {
        return this.clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getFacturaXml() {
        return this.facturaXml;
    }

    public void setFacturaXml(String facturaXml) {
        this.facturaXml = facturaXml;
    }

    public String getFacturaPdf() {
        return this.facturaPdf;
    }

    public void setFacturaPdf(String facturaPdf) {
        this.facturaPdf = facturaPdf;
    }

    public Date getFechaCreacion() {
        return this.fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getEstado() {
        return this.estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}

