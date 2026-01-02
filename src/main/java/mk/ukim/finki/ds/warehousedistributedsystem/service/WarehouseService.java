package mk.ukim.finki.ds.warehousedistributedsystem.service;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.ds.warehousedistributedsystem.events.AvailabilityCheckedEvent;
import mk.ukim.finki.ds.warehousedistributedsystem.events.OrderPlacedEvent;
import mk.ukim.finki.ds.warehousedistributedsystem.model.Inventory;
import mk.ukim.finki.ds.warehousedistributedsystem.model.OrderItem;
import mk.ukim.finki.ds.warehousedistributedsystem.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.swing.*;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, AvailabilityCheckedEvent> kafkaTemplate;
    private final Tracer tracer;
    private static final String RESPONSE_TOPIC = "availability-response";

    @Value("${warehouse.region}")
    private String region;

    @Value("${warehouse.response-topic}")
    private String responseTopic;

    public void handleOrderPlaced(OrderPlacedEvent orderPlacedEvent) {
        Span span = tracer.spanBuilder("warehouse.handleOrder")
                .setAttribute("order.id", orderPlacedEvent.getOrderId())
                .setAttribute("warehouse.region", region)
                .startSpan();
        try(var scope = span.makeCurrent()) {
            boolean available = checkInventory(orderPlacedEvent);
            int eta = available ? 2 : 0;

            AvailabilityCheckedEvent response = new AvailabilityCheckedEvent(
                    orderPlacedEvent.getOrderId(),
                    region,
                    available,
                    eta
            );

            kafkaTemplate.send(RESPONSE_TOPIC, orderPlacedEvent.getOrderId(), response);
            span.addEvent("response.published", Attributes.of(AttributeKey.booleanKey("available"), available));
        } finally {
            span.end();
        }
    }

    private boolean checkInventory(OrderPlacedEvent orderPlacedEvent) {
        for (OrderItem item : orderPlacedEvent.getItems()) {
            Inventory inv = inventoryRepository.findById(item.getProductId()).orElse(new Inventory(item.getProductId(), 0));
            if (inv.getStock() < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }
}
