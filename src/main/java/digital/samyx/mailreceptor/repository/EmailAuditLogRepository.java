package digital.samyx.mailreceptor.repository;

import digital.samyx.mailreceptor.enums.EstadoEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface EmailAuditLogRepository extends JpaRepository<EmailAuditLog, Long> {

    /**
     * Verifica si existe un email enviado para una factura
     */
    boolean existsByFacturaIdAndEstado(Long facturaId, EstadoEmail estado);

    /**
     * Obtiene IDs de facturas que ya fueron enviadas exitosamente
     */
    @Query("""
        SELECT DISTINCT e.facturaId 
        FROM EmailAuditLog e 
        WHERE e.facturaId IN :facturaIds 
        AND e.estado = 'ENVIADO'
        """)
    Set<Long> findFacturaIdsEnviados(@Param("facturaIds") Set<Long> facturaIds);

    /**
     * Encuentra emails fallidos que pueden reintentarse
     */
    List<EmailAuditLog> findByEstadoInAndIntentosLessThan(
        List<EstadoEmail> estados, 
        Integer maxIntentos
    );

    /**
     * Cuenta emails por estado en un rango de fechas
     */
    @Query("""
        SELECT e.estado, COUNT(e) 
        FROM EmailAuditLog e 
        WHERE e.createdAt BETWEEN :fechaInicio AND :fechaFin 
        GROUP BY e.estado
        """)
    List<Object[]> contarPorEstado(
        @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
        @Param("fechaFin") java.time.LocalDateTime fechaFin
    );
}