package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entidad Billetera Virtual asociada de forma 1:1 a un usuario.
 */
@Entity(name = "Wallet")
@Table(name = "wallet")
public class Wallet extends EntityBase {

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @JsonIgnore
    public Account owner;

    public int balance;
    public Instant updatedAt;
}
