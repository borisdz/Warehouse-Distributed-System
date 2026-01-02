package mk.ukim.finki.ds.warehousedistributedsystem.service;

import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.ds.warehousedistributedsystem.events.OrderPlacedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPlacedListener {
    private final WarehouseService warehouseService;

    @KafkaListener(
        topics = "${warehouse.order-topic}",
        groupId="${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderPlaced(OrderPlacedEvent event){
        log.info("Received OrderPlacedEvent: orderId={}, region={}",
                event.getOrderId(), event.getCustomerRegion());
        warehouseService.handleOrderPlaced(event);
    }
}
