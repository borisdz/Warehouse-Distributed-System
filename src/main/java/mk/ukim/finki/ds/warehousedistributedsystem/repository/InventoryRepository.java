package mk.ukim.finki.ds.warehousedistributedsystem.repository;

import mk.ukim.finki.ds.warehousedistributedsystem.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
}
