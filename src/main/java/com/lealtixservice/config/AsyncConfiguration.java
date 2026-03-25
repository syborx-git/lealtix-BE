package com.lealtixservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configuración personalizada para procesamiento asíncrono.
 * Define el executor y el manejo de excepciones no capturadas en métodos @Async.
 */
@Configuration
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    /**
     * Configura el executor para tareas asíncronas.
     * Usa un pool de hilos dedicado para envío de emails y otras operaciones post-commit.
     *
     * @return executor configurado
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Async executor configurado: corePoolSize=5, maxPoolSize=10, queueCapacity=100");
        return executor;
    }

    /**
     * Maneja excepciones no capturadas en métodos @Async.
     * Registra el error en los logs para visibilidad.
     *
     * @return handler de excepciones
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Uncaught exception in async method [{}]: {}", 
                        method.getName(), ex.getMessage(), ex);
                log.error("Method parameters: {}", (Object) params);
            }
        };
    }
}
