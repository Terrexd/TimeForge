package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Entidad Billetera Virtual asociada de forma 1:1 a un usuario.
 */
@Entity(name = "Billetera")
@Table(name = "wallet")
public class Billetera extends EntidadBase {

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @JsonIgnore
    public Usuario owner;

    public int balance;
    public Instant updatedAt;
}
