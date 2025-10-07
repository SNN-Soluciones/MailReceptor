package digital.samyx.mailreceptor.entity;

import digital.samyx.mailreceptor.enums.TipoImpresion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "terminales")
@ToString(exclude = {"sucursal"})
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Terminal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;
    
    @Column(name = "numero_terminal", length = 5, nullable = false)
    private String numeroTerminal; // 00001, 00002, etc.
    
    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre; // "Caja 1", "Terminal Principal", etc.
    
    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activa = true;
    
    // === Consecutivos para documentos electrónicos ===
    @Builder.Default
    @Column(name = "consecutivo_factura_electronica")
    private Long consecutivoFacturaElectronica = 0L;

    @Builder.Default
    @Column(name = "consecutivo_tiquete_electronico")
    private Long consecutivoTiqueteElectronico = 0L;

    @Builder.Default
    @Column(name = "consecutivo_nota_credito")
    private Long consecutivoNotaCredito = 0L;

    @Builder.Default
    @Column(name = "consecutivo_nota_debito")
    private Long consecutivoNotaDebito = 0L;

    @Builder.Default
    @Column(name = "consecutivo_factura_compra")
    private Long consecutivoFacturaCompra = 0L;

    @Builder.Default
    @Column(name = "consecutivo_factura_exportacion")
    private Long consecutivoFacturaExportacion = 0L;

    @Column(name = "consecutivo_mensaje_receptor", nullable = false)
    @Builder.Default
    private Long consecutivoMensajeReceptor = 0L;

    @Builder.Default
    @Column(name = "consecutivo_recibo_pago")
    private Long consecutivoReciboPago = 0L;
    
    // === Consecutivos para documentos internos (no Hacienda) ===
    @Builder.Default
    @Column(name = "consecutivo_tiquete_interno")
    private Long consecutivoTiqueteInterno = 0L;

    @Builder.Default
    @Column(name = "consecutivo_factura_interna")
    private Long consecutivoFacturaInterna = 0L;

    @Builder.Default
    @Column(name = "consecutivo_proforma")
    private Long consecutivoProforma = 0L;

    @Builder.Default
    @Column(name = "consecutivo_orden_pedido")
    private Long consecutivoOrdenPedido = 0L;

    @Builder.Default
    private TipoImpresion tipoImpresion = TipoImpresion.TICKET;

    @Builder.Default
    @Column(name = "imprimir_automatico")
    private Boolean imprimirAutomatico = false;
    
    // Auditoría
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Validar límite de terminales por sucursal
        if (sucursal != null && sucursal.getTerminales() != null) {
            long terminalesActivas = sucursal.getTerminales().stream()
                .filter(t -> !t.equals(this) && Boolean.TRUE.equals(t.getActiva()))
                .count();
            
            if (terminalesActivas >= 2) {
                throw new IllegalStateException("Máximo 2 terminales activas por sucursal");
            }
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Obtiene el consecutivo según el tipo de documento
     */
    public Long getConsecutivoPorTipo(String tipoDocumento) {
      return switch (tipoDocumento) {
        case "01" -> consecutivoFacturaElectronica;
        case "02" -> consecutivoNotaDebito;
        case "03" -> consecutivoNotaCredito;
        case "04" -> consecutivoTiqueteElectronico;
        case "05" -> consecutivoMensajeReceptor;
        case "08" -> consecutivoFacturaCompra;
        case "09" -> consecutivoFacturaExportacion;
        case "10" -> consecutivoReciboPago;
        case "TI" -> consecutivoTiqueteInterno;
        case "FI" -> consecutivoFacturaInterna;
        case "PF" -> consecutivoProforma;
        case "OP" -> consecutivoOrdenPedido;
        default ->
            throw new IllegalArgumentException("Tipo de documento no válido: " + tipoDocumento);
      };
    }
    
    /**
     * Incrementa y retorna el consecutivo según el tipo de documento
     */
    public Long incrementarConsecutivo(String tipoDocumento) {
        Long consecutivo = getConsecutivoPorTipo(tipoDocumento);
        consecutivo++;
        
        switch (tipoDocumento) {
            case "01": consecutivoFacturaElectronica = consecutivo; break;
            case "02": consecutivoNotaDebito = consecutivo; break;
            case "03": consecutivoNotaCredito = consecutivo; break;
            case "04": consecutivoTiqueteElectronico = consecutivo; break;
            case "08": consecutivoFacturaCompra = consecutivo; break;
            case "09": consecutivoFacturaExportacion = consecutivo; break;
            case "10": consecutivoReciboPago = consecutivo; break;
            case "TI": consecutivoTiqueteInterno = consecutivo; break;
            case "FI": consecutivoFacturaInterna = consecutivo; break;
            case "PF": consecutivoProforma = consecutivo; break;
            case "OP": consecutivoOrdenPedido = consecutivo; break;
        }
        
        return consecutivo;
    }
}