package digital.samyx.mailreceptor.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Procesador de lotes de facturas para envío de emails
 * 
 * Estrategia:
 * - Procesa máximo 50 facturas por ciclo (1 minuto)
 * - Solo facturas ACEPTADAS tipo 01 (Factura Electrónica)
 * - Excluye facturas ya enviadas
 * - Circuit breaker: detiene si hay >10 fallos consecutivos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacturaEmailBatchProcessor {

    private final FacturaBitacoraRepository bitacoraRepository;
    private final EmailAuditLogRepository auditRepository;
    private final FacturaEmailSenderService senderService;

    // Configuración
    private static final int MAX_FACTURAS_POR_CICLO = 50;
    private static final int MAX_FALLOS_CONSECUTIVOS = 10;
    private static final LocalDateTime FECHA_INICIO_ENVIOS = LocalDate.of(2024, 10, 1).atStartOfDay();
    
    // Estado del circuit breaker
    private int fallosConsecutivos = 0;
    
    /**
     * Procesa un lote de facturas pendientes
     * 
     * @return número de facturas procesadas
     */
    @Transactional(readOnly = true)
    public int procesarLote() {
        
        // Circuit breaker
        if (fallosConsecutivos >= MAX_FALLOS_CONSECUTIVOS) {
            log.error("🚨 CIRCUIT BREAKER ACTIVADO: {} fallos consecutivos. Deteniendo procesamiento.", 
                fallosConsecutivos);
            return 0;
        }
        
        try {
            log.debug("🔄 Iniciando procesamiento de lote de facturas...");
            
            // 1. Obtener facturas candidatas (solo metadatos mínimos)
            LocalDateTime fechaHasta = LocalDateTime.now();
            var candidatas = bitacoraRepository.findAceptadasTipoFacturaBetween(
                FECHA_INICIO_ENVIOS, fechaHasta);
            
            if (candidatas.isEmpty()) {
                log.debug("✅ No hay facturas pendientes");
                return 0;
            }
            
            log.info("📋 Candidatas encontradas: {}", candidatas.size());
            
            // 2. Filtrar las ya enviadas
            Set<Long> facturaIds = candidatas.stream()
                .map(b -> b.getFacturaId())
                .collect(Collectors.toSet());
                
            Set<Long> yaEnviadas = auditRepository.findFacturaIdsEnviados(facturaIds);
            
            var porEnviar = candidatas.stream()
                .filter(b -> !yaEnviadas.contains(b.getFacturaId()))
                .limit(MAX_FACTURAS_POR_CICLO)
                .toList();
            
            log.info("📬 Por enviar (sin audit ENVIADO): {}", porEnviar.size());
            
            if (porEnviar.isEmpty()) {
                log.info("✅ Todas las facturas ya fueron enviadas");
                fallosConsecutivos = 0; // Reset circuit breaker
                return 0;
            }
            
            // 3. Enviar en paralelo (hasta 20 hilos concurrentes)
            List<CompletableFuture<Boolean>> futures = porEnviar.stream()
                .map(b -> senderService.enviarFacturaAsync(b.getFacturaId()))
                .toList();
            
            // 4. Esperar resultados
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            allOf.join(); // Esperar que todos terminen
            
            // 5. Contar resultados
            long exitosos = futures.stream()
                .map(CompletableFuture::join)
                .filter(Boolean::booleanValue)
                .count();
            
            long fallidos = futures.size() - exitosos;
            
            log.info("✅ Lote procesado: enviados={}, fallidos={}", exitosos, fallidos);
            
            // Actualizar circuit breaker
            if (fallidos == 0) {
                fallosConsecutivos = 0;
            } else if (fallidos == futures.size()) {
                fallosConsecutivos++;
            } else {
                fallosConsecutivos = 0; // Si hay algún éxito, resetear
            }
            
            return (int) exitosos;
            
        } catch (Exception e) {
            log.error("❌ Error procesando lote: {}", e.getMessage(), e);
            fallosConsecutivos++;
            return 0;
        }
    }
    
    /**
     * Resetea el circuit breaker manualmente
     */
    public void resetCircuitBreaker() {
        fallosConsecutivos = 0;
        log.info("🔄 Circuit breaker reseteado");
    }
    
    /**
     * Obtiene el estado actual del circuit breaker
     */
    public int getFallosConsecutivos() {
        return fallosConsecutivos;
    }
}