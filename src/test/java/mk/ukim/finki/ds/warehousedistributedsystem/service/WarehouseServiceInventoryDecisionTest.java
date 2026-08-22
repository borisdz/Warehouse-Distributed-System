package mk.ukim.finki.ds.warehousedistributedsystem.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import mk.ukim.finki.ds.contracts.events.AvailabilityCheckedEvent;
import mk.ukim.finki.ds.contracts.events.OrderPlacedEvent;
import mk.ukim.finki.ds.contracts.model.OrderItem;
import mk.ukim.finki.ds.warehousedistributedsystem.model.Inventory;
import mk.ukim.finki.ds.warehousedistributedsystem.repository.InventoryRepository;
import mk.ukim.finki.ds.warehousedistributedsystem.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WarehouseServiceInventoryDecisionTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryReservationRepository reservationRepository;
    @Mock
    private KafkaTemplate<String, AvailabilityCheckedEvent> kafkaTemplate;
    @Mock
    private Tracer tracer;

    private WarehouseService warehouseService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        SpanBuilder spanBuilder = mock(SpanBuilder.class);
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        Span span = mock(Span.class);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(mock(Scope.class));
        when(kafkaTemplate.send(anyString(), anyString(), any(AvailabilityCheckedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        warehouseService = new WarehouseService(inventoryRepository, reservationRepository, kafkaTemplate, tracer);
        ReflectionTestUtils.setField(warehouseService, "region", "us-1");
        ReflectionTestUtils.setField(warehouseService, "responseTopic", "availability-response");
    }

    @SuppressWarnings("unchecked")
    private AvailabilityCheckedEvent captureResponse() {
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AvailabilityCheckedEvent> eventCaptor = ArgumentCaptor.forClass(AvailabilityCheckedEvent.class);
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());
        assertEquals("availability-response", topicCaptor.getValue());
        return eventCaptor.getValue();
    }

    @Test
    void allItemsInStockYieldsAvailableResponseWithFullPayload() {
        when(inventoryRepository.findByProductIdForUpdate("P1"))
                .thenReturn(Optional.of(new Inventory("P1", 10)));
        when(inventoryRepository.findByProductIdForUpdate("P2"))
                .thenReturn(Optional.of(new Inventory("P2", 5)));

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-1", 1, Instant.now(), "correlation-1", "order-1", "cust-1", "us",
                List.of(new OrderItem("P1", 2), new OrderItem("P2", 1)));

        warehouseService.handleOrderPlaced(event);

        AvailabilityCheckedEvent response = captureResponse();
        assertTrue(response.isAvailable());
        assertEquals("order-1", response.getOrderId());
        assertEquals("us-1", response.getWarehouseRegion());
        assertEquals("us-1", response.getWarehouseId());
        assertEquals("correlation-1", response.getCorrelationId());
        assertEquals(2, response.getEtaHours());
    }

    @Test
    void missingProductYieldsUnavailableResponse() {
        when(inventoryRepository.findByProductIdForUpdate("P1")).thenReturn(Optional.empty());

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-2", 1, Instant.now(), "correlation-2", "order-2", "cust-2", "us",
                List.of(new OrderItem("P1", 1)));

        warehouseService.handleOrderPlaced(event);

        AvailabilityCheckedEvent response = captureResponse();
        assertFalse(response.isAvailable());
        assertEquals(0, response.getEtaHours());
    }

    @Test
    void insufficientStockOnOneOfMultipleItemsYieldsUnavailableResponse() {
        when(inventoryRepository.findByProductIdForUpdate("P1"))
                .thenReturn(Optional.of(new Inventory("P1", 10)));
        when(inventoryRepository.findByProductIdForUpdate("P2"))
                .thenReturn(Optional.of(new Inventory("P2", 1)));

        OrderPlacedEvent event = new OrderPlacedEvent(
                "event-3", 1, Instant.now(), "correlation-3", "order-3", "cust-3", "us",
                List.of(new OrderItem("P1", 2), new OrderItem("P2", 5)));

        warehouseService.handleOrderPlaced(event);

        AvailabilityCheckedEvent response = captureResponse();
        assertFalse(response.isAvailable());
    }
}
