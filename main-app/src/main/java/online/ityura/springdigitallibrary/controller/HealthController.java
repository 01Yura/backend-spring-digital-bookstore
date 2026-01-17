package online.ityura.springdigitallibrary.controller;

import online.ityura.springdigitallibrary.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;

@RestController
@Tag(name = "System", description = "Системные эндпоинты для мониторинга и диагностики (доступно без авторизации)")
public class HealthController {

    @GetMapping("/api/v1/health")
    @Operation(
            summary = "Проверка состояния приложения (доступен без авторизации)",
            description = "Возвращает статус работы приложения, время работы (uptime) и текущую метку времени. " +
                    "Используется для мониторинга работоспособности сервиса (доступен без авторизации)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Приложение работает",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HealthResponse.class),
                            examples = @ExampleObject(name = "healthResponse",
                                    value = """
                                    {
                                      "status": "UP",
                                      "uptime": "02:15:30",
                                      "timestamp": "2025-12-13T04:14:37.122Z"
                                    }
                                    """)))
    })
    public ResponseEntity<HealthResponse> health() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String uptimeHms = formatUptime(uptimeMs);
        HealthResponse body = new HealthResponse("UP", uptimeHms, Instant.now().toString());
        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(body);
    }

    private String formatUptime(long uptimeMs) {
        long totalSeconds = uptimeMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}

