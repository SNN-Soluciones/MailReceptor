package digital.samyx.mailreceptor.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuración de procesamiento asíncrono para envío de emails
 * 
 * Pool separado del principal para no afectar otras operaciones
 * Capacidad: 2000-5000 emails/día = ~3-5 emails/minuto en promedio
 * Picos: hasta 50 emails/minuto
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncEmailConfig implements AsyncConfigurer {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Pool core: hilos mínimos siempre activos
        executor.setCorePoolSize(5);
        
        // Pool máximo: escala hasta este número bajo carga
        executor.setMaxPoolSize(20);
        
        // Cola: facturas esperando si todos los hilos están ocupados
        executor.setQueueCapacity(100);
        
        // Nombres de hilos para debugging
        executor.setThreadNamePrefix("EmailSender-");
        
        // Timeout de hilos idle
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        
        // Política de rechazo: si la cola está llena, el hilo que invoca ejecuta
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Esperar que terminen tareas al shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("✅ Email TaskExecutor configurado: core={}, max={}, queue={}", 
            executor.getCorePoolSize(), 
            executor.getMaxPoolSize(), 
            executor.getQueueCapacity());
        
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("❌ Error asíncrono en {}: {}", method.getName(), ex.getMessage(), ex);
        };
    }
}