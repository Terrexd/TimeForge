package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Movimiento / Transacción en la Billetera Virtual (Ingreso o Gasto).
 */
@Entity(name = "Movement")
@Table(name = "wallet_movement")
public class Movement extends EntityBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Wallet wallet;

    public String concept;
    public int amount;
    public Instant createdAt;
}
