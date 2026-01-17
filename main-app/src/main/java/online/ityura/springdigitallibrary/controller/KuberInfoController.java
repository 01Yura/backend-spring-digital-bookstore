package online.ityura.springdigitallibrary.controller;

import online.ityura.springdigitallibrary.dto.response.KuberInfoResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "Системные эндпоинты для мониторинга и диагностики (если приложение запущено не в кубере, то некоторые параметры будут в статусе 'unknown' или 'not configured') (доступно без авторизации)")
public class KuberInfoController {

    @GetMapping("/kuberinfo")
    @Operation(
            summary = "Информация о Kubernetes окружении (доступен без авторизации)",
            description = "Возвращает информацию о контейнере, поде, ноде и операционной системе, " +
                    "на которых запущено приложение. Полезно для диагностики и понимания, " +
                    "на какой под и воркер-ноду попал запрос при использовании балансировщика нагрузки (доступен без авторизации)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Информация успешно получена",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KuberInfoResponse.class),
                            examples = @ExampleObject(name = "kuberInfoResponse",
                                    value = """
                                    {
                                      "podName": "spring-digital-bookstore-app-7d8f9c4b5-abc12",
                                      "nodeName": "worker-node-1",
                                      "podIP": "10.244.1.5",
                                      "nodeIP": "192.168.0.100",
                                      "osName": "Linux",
                                      "osVersion": "5.10.0-18-cloud-amd64",
                                      "osArch": "amd64",
                                      "hostname": "spring-digital-bookstore-app-7d8f9c4b5-abc12",
                                      "timestamp": "2025-12-13T04:14:37.122Z",
                                      "jvmUptime": "02:15:30"
                                    }
                                    """)))
    })
    public ResponseEntity<KuberInfoResponse> kuberinfo() {
        // Получаем имя пода из переменной окружения HOSTNAME (автоматически устанавливается Kubernetes)
        String podName = System.getenv("HOSTNAME");
        if (podName == null || podName.isEmpty()) {
            podName = "unknown";
        }

        // Получаем имя ноды из переменной окружения NODE_NAME (если настроено через downward API)
        String nodeName = System.getenv("NODE_NAME");
        if (nodeName == null || nodeName.isEmpty()) {
            nodeName = "not configured";
        }

        // Получаем IP адрес пода из переменной окружения POD_IP (если настроено через downward API)
        String podIP = System.getenv("POD_IP");
        if (podIP == null || podIP.isEmpty()) {
            // Пытаемся получить через InetAddress как fallback
            try {
                podIP = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                podIP = "unknown";
            }
        }

        // Получаем IP адрес ноды из переменной окружения NODE_IP (если настроено через downward API)
        String nodeIP = System.getenv("NODE_IP");
        if (nodeIP == null || nodeIP.isEmpty()) {
            nodeIP = "not configured";
        }

        // Получаем информацию об операционной системе
        String osName = System.getProperty("os.name", "unknown");
        String osVersion = System.getProperty("os.version", "unknown");
        String osArch = System.getProperty("os.arch", "unknown");

        // Получаем hostname через InetAddress
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // Если не удалось получить, используем значение по умолчанию
        }

        // Получаем текущую метку времени
        String timestamp = Instant.now().toString();

        // Получаем время работы JVM и форматируем его
        long jvmUptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String jvmUptime = formatUptime(jvmUptimeMs);

        KuberInfoResponse response = new KuberInfoResponse(
                podName,
                nodeName,
                podIP,
                nodeIP,
                osName,
                osVersion,
                osArch,
                hostname,
                timestamp,
                jvmUptime
        );

        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(response);
    }

    private String formatUptime(long uptimeMs) {
        long totalSeconds = uptimeMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}



























