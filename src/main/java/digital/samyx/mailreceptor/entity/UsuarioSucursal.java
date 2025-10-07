package digital.samyx.mailreceptor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios_sucursales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UsuarioSucursal.UsuarioSucursalId.class)
public class UsuarioSucursal {

    @Id
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Id
    @ManyToOne
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_asignacion")
    @Builder.Default
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

    // Clase para la clave compuesta
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioSucursalId implements Serializable {
        private Long usuario;
        private Long sucursal;
    }
}