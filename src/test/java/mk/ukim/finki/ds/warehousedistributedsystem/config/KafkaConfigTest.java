package mk.ukim.finki.ds.warehousedistributedsystem.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaConfigTest {

    @Test
    void consumerFactoryWrapsValueDeserializerForErrorHandling() {
        KafkaConfig config = new KafkaConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "us-1-group");

        var consumerFactory = (DefaultKafkaConsumerFactory<?, ?>) config.consumerFactory();

        assertInstanceOf(org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class,
                consumerFactory.getValueDeserializer());
        assertNotNull(consumerFactory.getConfigurationProperties().get(ConsumerConfig.GROUP_ID_CONFIG));
    }

    @Test
    void listenerContainerFactoryRegistersCommonErrorHandler() {
        KafkaConfig config = new KafkaConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "us-1-group");

        ConcurrentKafkaListenerContainerFactory<?, ?> factory = config.kafkaListenerContainerFactory();

        assertNotNull(ReflectionTestUtils.getField(factory, "commonErrorHandler"));
    }
}
