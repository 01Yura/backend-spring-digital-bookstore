package online.ityura.springdigitallibrary.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // Изменено с "all" на "1" для более быстрой обработки
        configProps.put(ProducerConfig.RETRIES_CONFIG, 0); // Отключаем повторные попытки при недоступности
        // Таймауты для более graceful handling недоступности Kafka
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000); // Не блокировать больше 2 секунд
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000); // 5 секунд на запрос
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000); // 10 секунд на доставку
        configProps.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Kafka Consumer для получения агрегированных данных
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "main-app-analytics-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Отключаем использование типа из JSON, десериализуем в Map
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        // Настройки для graceful handling недоступности Kafka
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Настройка обработки ошибок - не логируем ошибки подключения постоянно
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L)));
        
        // Отключаем автоматический перезапуск контейнера при ошибках подключения
        factory.getContainerProperties().setShutdownTimeout(5000);
        factory.setAutoStartup(true);
        
        // Настройка для graceful shutdown при недоступности Kafka
        ContainerProperties containerProps = factory.getContainerProperties();
        containerProps.setAckMode(ContainerProperties.AckMode.BATCH);
        
        return factory;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin admin = new KafkaAdmin(configs);
        // Не падаем при недоступности Kafka при старте
        admin.setFatalIfBrokerNotAvailable(false);
        admin.setAutoCreate(false);
        return admin;
    }

    @Bean
    public AdminClient adminClient() {
        return AdminClient.create(kafkaAdmin().getConfigurationProperties());
    }

    // Конфигурация топиков Kafka с правильным количеством партиций
    // Spring Kafka автоматически создаст эти топики при старте приложения
    @Bean
    public NewTopic bookViewsTopic() {
        return TopicBuilder.name("book.views")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookDownloadsTopic() {
        return TopicBuilder.name("book.downloads")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookPurchasesTopic() {
        return TopicBuilder.name("book.purchases")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookReviewsTopic() {
        return TopicBuilder.name("book.reviews")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookRatingsTopic() {
        return TopicBuilder.name("book.ratings")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic analyticsAggregatedStatsTopic() {
        return TopicBuilder.name("analytics.aggregated-stats")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
