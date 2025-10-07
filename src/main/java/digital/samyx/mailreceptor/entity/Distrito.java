package digital.samyx.mailreceptor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "distrito")
public class Distrito {
    
    @Id
    private Integer id;
    
    @Column(name = "codigoProvincia")
    private Integer codigoProvincia;
    
    @Column(name = "codigoCanton")
    private Integer codigoCanton;
    
    @Column(name = "codigo")
    private Integer codigo;
    
    @Column(name = "distrito", length = 70)
    private String distrito;
}