package digital.samyx.mailreceptor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "barrio")
public class Barrio {
    
    @Id
    private Integer id;
    
    @Column(name = "codigoProvincia")
    private Integer codigoProvincia;
    
    @Column(name = "codigoCanton")
    private Integer codigoCanton;
    
    @Column(name = "codigoDistrito")
    private Integer codigoDistrito;
    
    @Column(name = "codigo")
    private Integer codigo;
    
    @Column(name = "barrio", length = 80)
    private String barrio;
}