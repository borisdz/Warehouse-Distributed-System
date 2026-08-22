package mk.ukim.finki.ds.warehousedistributedsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "processed_warehouse_events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "warehouse_region"}))
public class ProcessedWarehouseEvent {
    @Id
    private String eventId;
    private String orderId;
    private String warehouseRegion;
    @Builder.Default
    private Instant processedAt = Instant.now();
}