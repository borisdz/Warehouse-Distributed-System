package mk.ukim.finki.ds.warehousedistributedsystem.config;

import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.ds.contracts.events.OrderPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.lang.NonNull;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @NonNull
    public ConsumerFactory<String, OrderPlacedEvent> consumerFactory(){
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<OrderPlacedEvent> deserializer = new JsonDeserializer<>(OrderPlacedEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer));
    }

    @Bean
    @NonNull
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Deserialization/processing failures are logged with full context instead of
        // being retried indefinitely or dropped silently.
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Skipping unprocessable OrderPlacedEvent record: topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset(), exception),
                new FixedBackOff(1000L, 2)));
        return factory;
    }
}
