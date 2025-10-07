package digital.samyx.mailreceptor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "canton")
public class Canton {
    
    @Id
    private Integer id;
    
    @Column(name = "codigoProvincia", nullable = false)
    private Integer codigoProvincia;
    
    @Column(name = "codigo", nullable = false)
    private Integer codigo;
    
    @Column(name = "canton", length = 45)
    private String canton;
}