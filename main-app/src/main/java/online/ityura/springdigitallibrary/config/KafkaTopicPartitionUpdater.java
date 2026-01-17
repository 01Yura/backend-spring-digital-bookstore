package online.ityura.springdigitallibrary.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.TopicDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Компонент для автоматического обновления количества партиций существующих топиков Kafka
 * при старте приложения. Увеличивает количество партиций до требуемого значения,
 * если текущее количество меньше требуемого.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(AdminClient.class)
public class KafkaTopicPartitionUpdater implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicPartitionUpdater.class);

    @Autowired
    private AdminClient adminClient;

    // Требуемое количество партиций для каждого топика
    private static final Map<String, Integer> REQUIRED_PARTITIONS = new HashMap<>();
    
    static {
        REQUIRED_PARTITIONS.put("book.views", 3);
        REQUIRED_PARTITIONS.put("book.downloads", 3);
        REQUIRED_PARTITIONS.put("book.purchases", 2);
        REQUIRED_PARTITIONS.put("book.reviews", 2);
        REQUIRED_PARTITIONS.put("book.ratings", 2);
        REQUIRED_PARTITIONS.put("analytics.aggregated-stats", 2);
    }

    @Override
    public void run(String... args) {
        try {
            // Небольшая задержка, чтобы Kafka успел полностью запуститься
            Thread.sleep(2000);
            updateTopicPartitions();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Прервано обновление партиций топиков Kafka", e);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении партиций топиков Kafka. " +
                    "Приложение продолжит работу, но топики могут иметь неправильное количество партиций.", e);
        }
    }

    private void updateTopicPartitions() {
        try {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            
            for (Map.Entry<String, Integer> entry : REQUIRED_PARTITIONS.entrySet()) {
                String topicName = entry.getKey();
                int requiredPartitions = entry.getValue();
                
                if (existingTopics.contains(topicName)) {
                    TopicDescription topicDescription = adminClient.describeTopics(Set.of(topicName))
                            .allTopicNames()
                            .get()
                            .get(topicName);
                    
                    int currentPartitions = topicDescription.partitions().size();
                    
                    if (currentPartitions < requiredPartitions) {
                        logger.info("Увеличиваем количество партиций для топика '{}' с {} до {}", 
                                topicName, currentPartitions, requiredPartitions);
                        
                        Map<String, NewPartitions> newPartitions = new HashMap<>();
                        newPartitions.put(topicName, NewPartitions.increaseTo(requiredPartitions));
                        
                        adminClient.createPartitions(newPartitions).all().get();
                        
                        logger.info("Количество партиций для топика '{}' успешно увеличено до {}", 
                                topicName, requiredPartitions);
                    } else if (currentPartitions == requiredPartitions) {
                        logger.debug("Топик '{}' уже имеет требуемое количество партиций: {}", 
                                topicName, requiredPartitions);
                    } else {
                        logger.warn("Топик '{}' имеет больше партиций ({}) чем требуется ({}). " +
                                "Уменьшение количества партиций не поддерживается Kafka.", 
                                topicName, currentPartitions, requiredPartitions);
                    }
                } else {
                    logger.debug("Топик '{}' еще не существует, будет создан автоматически с {} партициями", 
                            topicName, requiredPartitions);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Прервано обновление партиций топиков Kafka", e);
        } catch (ExecutionException e) {
            logger.error("Ошибка при работе с Kafka Admin API: {}", e.getMessage(), e);
            // Не бросаем исключение, чтобы не блокировать запуск приложения
        }
    }
}
