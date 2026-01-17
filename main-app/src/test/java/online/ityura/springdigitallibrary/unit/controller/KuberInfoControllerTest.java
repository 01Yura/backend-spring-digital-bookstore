package online.ityura.springdigitallibrary.unit.controller;

import online.ityura.springdigitallibrary.controller.KuberInfoController;
import online.ityura.springdigitallibrary.dto.response.KuberInfoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KuberInfoControllerTest {
    
    @InjectMocks
    private KuberInfoController kuberInfoController;
    
    @Test
    void testKuberinfo_ShouldReturnInfo() {
        // When
        ResponseEntity<KuberInfoResponse> response = kuberInfoController.kuberinfo();
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getPodName());
        assertNotNull(response.getBody().getNodeName());
        assertNotNull(response.getBody().getPodIP());
        assertNotNull(response.getBody().getNodeIP());
        assertNotNull(response.getBody().getOsName());
        assertNotNull(response.getBody().getOsVersion());
        assertNotNull(response.getBody().getOsArch());
        assertNotNull(response.getBody().getHostname());
        assertNotNull(response.getBody().getTimestamp());
        assertNotNull(response.getBody().getJvmUptime());
        assertNotNull(response.getHeaders().getCacheControl());
    }
}

