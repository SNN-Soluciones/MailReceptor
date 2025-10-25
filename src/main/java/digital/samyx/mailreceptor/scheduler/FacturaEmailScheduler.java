package digital.samyx.mailreceptor.scheduler;

import digital.samyx.mailreceptor.service.email.FacturaEmailBatchProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduler para envío automático de emails de facturas
 * 
 * Configuración:
 * - Se ejecuta cada 1 minuto
 * - Procesa hasta 50 facturas por ciclo
 * - Se puede deshabilitar con: app.email.scheduler.enabled=false
 * 
 * Monitoreo:
 * - Logs detallados de cada ejecución
 * - Métricas de tiempo de ejecución
 * - Circuit breaker para prevenir sobrecarga
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.email.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true // Habilitado por defecto
)
public class FacturaEmailScheduler {

    private final FacturaEmailBatchProcessor batchProcessor;
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Job principal: se ejecuta cada 1 minuto
     * 
     * fixedDelay: espera 60 segundos desde que termina la ejecución anterior
     * initialDelay: espera 10 segundos al iniciar la aplicación
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void procesarFacturasPendientes() {
        
        String horaInicio = LocalDateTime.now().format(TIME_FORMAT);
        log.info("⏰ ========== INICIO PROCESAMIENTO FACTURAS: {} ==========", horaInicio);
        
        long startTime = System.currentTimeMillis();
        
        try {
            int procesadas = batchProcessor.procesarLote();
            
            long duration = System.currentTimeMillis() - startTime;
            String horaFin = LocalDateTime.now().format(TIME_FORMAT);
            
            if (procesadas > 0) {
                log.info("✅ ========== FIN PROCESAMIENTO: {} - Procesadas: {} - Duración: {}ms ==========", 
                    horaFin, procesadas, duration);
            } else {
                log.debug("✅ ========== FIN PROCESAMIENTO: {} - Sin facturas pendientes - Duración: {}ms ==========", 
                    horaFin, duration);
            }
            
            // Alerta si tarda mucho (>45 segundos)
            if (duration > 45_000) {
                log.warn("⚠️  ALERTA: Procesamiento tardó {}ms (>45s). Riesgo de overlap con siguiente ciclo.", duration);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ ========== ERROR EN PROCESAMIENTO - Duración: {}ms ==========", duration, e);
        }
    }
    
    /**
     * Job de monitoreo: se ejecuta cada 5 minutos
     * Reporta estado del circuit breaker
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reportarEstado() {
        int fallos = batchProcessor.getFallosConsecutivos();
        
        if (fallos > 0) {
            log.warn("⚠️  Circuit breaker: {} fallos consecutivos", fallos);
        }
    }
}