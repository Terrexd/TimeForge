package es.timeforge.model;

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
@Entity(name = "Mission")
@Table(name = "mission", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "work_id", "weekStart"}))
public class Mission extends EntityBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Account owner;

    @ManyToOne(optional = false)
    public Work work;

    public String description;
    public double baseline;
    public double target;
    public double progress;
    public int reward = 40;
    public boolean claimed;
    public LocalDate weekStart;
    public Instant expiresAt;
}
