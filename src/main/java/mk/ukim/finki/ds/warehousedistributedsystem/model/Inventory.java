package mk.ukim.finki.ds.warehousedistributedsystem.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    @Id
    private String productId;
    private int stock;
    @Column(nullable = false)
    private int reserved;

    public Inventory(String productId, int stock) {
        this.productId = productId;
        this.stock = stock;
        this.reserved = 0;
    }
}
