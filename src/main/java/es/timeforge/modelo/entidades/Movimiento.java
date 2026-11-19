package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Movimiento / Transacción en la Billetera Virtual (Ingreso o Gasto).
 */
@Entity(name = "Movimiento")
@Table(name = "wallet_movement")
public class Movimiento extends EntidadBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Billetera wallet;

    public String concept;
    public int amount;
    public Instant createdAt;
}
