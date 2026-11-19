package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Misión semanal dinámica generada a partir del catálogo del usuario.
 */
@Entity(name = "Mision")
@Table(name = "mission", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id", "work_id",
        "weekStart" }))
public class Mision extends EntidadBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Usuario owner;

    @ManyToOne(optional = false)
    public Obra work;

    public String description;
    public double baseline;
    public double target;
    public double progress;
    public int reward = 40;
    public boolean claimed;
    public LocalDate weekStart;
    public Instant expiresAt;
}
