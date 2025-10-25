package digital.samyx.mailreceptor.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "actividades_economicas")
public class ActividadEconomica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 6)
    private String codigo; // Ej: "523110" - 6 dígitos según Hacienda
    
    @Column(name = "descripcion", length = 300, nullable = false)
    private String descripcion;
    
    @Column(name = "categoria", length = 100)
    private String categoria;
    
    @Column(nullable = false)
    private Boolean activa = true;
}