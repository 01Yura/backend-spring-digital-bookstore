package online.ityura.springdigitallibrary.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 секунд для подключения
        factory.setReadTimeout(120000); // 120 секунд (2 минуты) для чтения ответа - OpenAI может обрабатывать запрос долго
        return new RestTemplate(factory);
    }
    
    /**
     * Registers {@link JavaTimeModule} so {@code java.time} types (e.g. {@code Instant} in
     * {@link online.ityura.springdigitallibrary.dto.response.ErrorResponse}) serialize as ISO-8601 strings.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

