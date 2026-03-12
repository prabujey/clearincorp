package com.trivine.llc.api.config;

import com.trivine.llc.api.constants.ServiceConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ServiceConstants.ASYNC_CORE_POOL_SIZE);
        executor.setMaxPoolSize(ServiceConstants.ASYNC_MAX_POOL_SIZE);
        executor.setQueueCapacity(ServiceConstants.ASYNC_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(ServiceConstants.ASYNC_THREAD_NAME_PREFIX);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // CallerRunsPolicy: when the queue is full, the calling thread executes the task
        // This prevents task loss and provides backpressure
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

    /**
     * Custom handler for uncaught exceptions in async methods that return void.
     * For methods returning CompletableFuture, exceptions are propagated to the future.
     */
    private static class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Uncaught async exception in method '{}': {}", method.getName(), ex.getMessage(), ex);
        }
    }
}
