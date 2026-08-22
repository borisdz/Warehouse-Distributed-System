package mk.ukim.finki.ds.warehousedistributedsystem.repository;

import mk.ukim.finki.ds.warehousedistributedsystem.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    List<InventoryReservation> findByOrderIdAndStatus(String orderId, String status);
}