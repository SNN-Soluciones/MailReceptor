package digital.samyx.mailreceptor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "provincia")
public class Provincia {
    
    @Id
    private Integer id;
    
    @Column(name = "codigo")
    private Integer codigo;
    
    @Column(name = "provincia", length = 45)
    private String provincia;
}