package mk.ukim.finki.ds.warehousedistributedsystem.service;

import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.ds.contracts.events.OrderPlacedEvent;
import mk.ukim.finki.ds.warehousedistributedsystem.model.ProcessedWarehouseEvent;
import mk.ukim.finki.ds.warehousedistributedsystem.repository.ProcessedWarehouseEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedListener {
    private final WarehouseService warehouseService;
    private final ProcessedWarehouseEventRepository processedEventRepository;

    @KafkaListener(
        topics = "${warehouse.order-topic}",
        groupId="${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onOrderPlaced(OrderPlacedEvent event){
        log.info("Received OrderPlacedEvent: orderId={}, region={}",
                event.getOrderId(), event.getCustomerRegion());

        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Ignoring duplicate OrderPlacedEvent: eventId={}", event.getEventId());
            return;
        }

        processedEventRepository.save(ProcessedWarehouseEvent.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .warehouseRegion(event.getCustomerRegion())
                .build());
        warehouseService.handleOrderPlaced(event);
    }
}
