package mk.ukim.finki.ds.warehousedistributedsystem.service;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.ds.contracts.events.AvailabilityCheckedEvent;
import mk.ukim.finki.ds.contracts.events.OrderPlacedEvent;
import mk.ukim.finki.ds.contracts.model.OrderItem;
import mk.ukim.finki.ds.warehousedistributedsystem.model.Inventory;
import mk.ukim.finki.ds.warehousedistributedsystem.model.InventoryReservation;
import mk.ukim.finki.ds.warehousedistributedsystem.repository.InventoryReservationRepository;
import mk.ukim.finki.ds.warehousedistributedsystem.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final KafkaTemplate<String, AvailabilityCheckedEvent> kafkaTemplate;
    private final Tracer tracer;

    @Value("${warehouse.region}")
    private String region;

    @Value("${warehouse.response-topic}")
    private String responseTopic;

    @Transactional
    public void handleOrderPlaced(OrderPlacedEvent orderPlacedEvent) {
        Span span = tracer.spanBuilder("warehouse.handleOrder")
                .setAttribute("order.id", orderPlacedEvent.getOrderId())
                .setAttribute("warehouse.region", region)
                .startSpan();
        try (var scope = span.makeCurrent()) {
            boolean available = reserveInventory(orderPlacedEvent.getOrderId(), orderPlacedEvent.getItems());
            int eta = available ? 2 : 0;

            AvailabilityCheckedEvent response = new AvailabilityCheckedEvent(
                    UUID.randomUUID().toString(),
                    AvailabilityCheckedEvent.CURRENT_VERSION,
                    Instant.now(),
                    orderPlacedEvent.getCorrelationId(),
                    orderPlacedEvent.getOrderId(),
                    region,
                    region,
                    available,
                    eta);

            kafkaTemplate.send(
                    Objects.requireNonNull(responseTopic, "responseTopic must not be null"),
                    Objects.requireNonNull(orderPlacedEvent.getOrderId(), "orderId must not be null"),
                    response);
            span.addEvent("response.published", Attributes.of(AttributeKey.booleanKey("available"), available));
        } finally {
            span.end();
        }
    }

    private boolean reserveInventory(String orderId, java.util.List<OrderItem> items) {
        java.util.List<Inventory> inventories = items.stream()
                .map(item -> inventoryRepository.findByProductIdForUpdate(item.getProductId()).orElse(null))
                .toList();
        for (int index = 0; index < items.size(); index++) {
            Inventory inventory = inventories.get(index);
            if (inventory == null
                    || inventory.getStock() - inventory.getReserved() < items.get(index).getQuantity()) {
                return false;
            }
        }

        for (int index = 0; index < items.size(); index++) {
            OrderItem item = items.get(index);
            Inventory inventory = inventories.get(index);
            inventory.setReserved(inventory.getReserved() + item.getQuantity());
            inventoryRepository.save(inventory);
            reservationRepository.save(
                    Objects.requireNonNull(
                            InventoryReservation.builder()
                                    .orderId(orderId)
                                    .productId(item.getProductId())
                                    .quantityReserved(item.getQuantity())
                                    .reservedAt(Instant.now())
                                    .status("RESERVED")
                                    .build()));
        }
        return true;
    }
}
