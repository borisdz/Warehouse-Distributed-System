package mk.ukim.finki.ds.warehousedistributedsystem.repository;

import mk.ukim.finki.ds.warehousedistributedsystem.model.ProcessedWarehouseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWarehouseEventRepository extends JpaRepository<ProcessedWarehouseEvent, String> {
}