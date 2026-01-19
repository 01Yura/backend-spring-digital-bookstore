package online.ityura.springdigitallibrary.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    
    private final MeterRegistry meterRegistry;
    private final Counter http4xxErrors;
    private final Counter http5xxErrors;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Общие счетчики для 4xx и 5xx ошибок
        this.http4xxErrors = Counter.builder("http.errors.4xx.total")
                .description("Total number of 4xx HTTP errors")
                .register(meterRegistry);
        
        this.http5xxErrors = Counter.builder("http.errors.5xx.total")
                .description("Total number of 5xx HTTP errors")
                .register(meterRegistry);
    }
    
    /**
     * Увеличить счетчик ошибок по статусу кода
     */
    public void incrementErrorCounter(int statusCode, String path, String method, String errorType) {
        // Увеличить общий счетчик по статусу
        Counter.builder("http.errors.by.status")
                .tag("status", String.valueOf(statusCode))
                .tag("path", path != null ? path : "unknown")
                .tag("method", method != null ? method : "unknown")
                .tag("error_type", errorType != null ? errorType : "unknown")
                .description("HTTP errors by status code, path, method and error type")
                .register(meterRegistry)
                .increment();
        
        // Увеличить счетчики 4xx или 5xx
        if (statusCode >= 400 && statusCode < 500) {
            http4xxErrors.increment();
        } else if (statusCode >= 500) {
            http5xxErrors.increment();
        }
    }
    
    /**
     * Записать время выполнения запроса
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Остановить таймер и записать метрику
     */
    public void recordTimer(Timer.Sample sample, String path, String method, int statusCode) {
        sample.stop(Timer.builder("http.request.duration")
                .description("HTTP request duration")
                .tag("path", path != null ? path : "unknown")
                .tag("method", method != null ? method : "unknown")
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry));
    }
}
