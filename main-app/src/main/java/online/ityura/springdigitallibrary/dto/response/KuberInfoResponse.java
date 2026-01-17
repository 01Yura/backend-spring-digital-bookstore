package online.ityura.springdigitallibrary.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.ityura.springdigitallibrary.dto.BaseDto;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Информация о Kubernetes окружении")
public class KuberInfoResponse extends BaseDto {
    
    @Schema(description = "Имя пода", example = "spring-students-app-7d8f9c4b5-abc12")
    private String podName;
    
    @Schema(description = "Имя ноды", example = "worker-node-1")
    private String nodeName;
    
    @Schema(description = "IP адрес пода", example = "10.244.1.5")
    private String podIP;
    
    @Schema(description = "IP адрес ноды", example = "192.168.0.100")
    private String nodeIP;
    
    @Schema(description = "Название операционной системы", example = "Linux")
    private String osName;
    
    @Schema(description = "Версия операционной системы", example = "5.10.0-18-cloud-amd64")
    private String osVersion;
    
    @Schema(description = "Архитектура операционной системы", example = "amd64")
    private String osArch;
    
    @Schema(description = "Имя хоста", example = "spring-students-app-7d8f9c4b5-abc12")
    private String hostname;
    
    @Schema(description = "Текущая метка времени в формате ISO-8601", example = "2025-12-13T04:14:37.122Z")
    private String timestamp;
    
    @Schema(description = "Время работы JVM в формате HH:mm:ss", example = "02:15:30")
    private String jvmUptime;
}









