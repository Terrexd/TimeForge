package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tarea productiva gestionada por un usuario.
 */
@Entity(name = "Tarea")
@Table(name = "task", indexes = @Index(columnList = "owner_id,state"))
public class Tarea extends EntidadBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Usuario owner;

    public String title;

    @Column(length = 2000)
    public String description = "";

    public String category;
    public String priority;
    public int estimatedMinutes;
    public int coins;
    public long elapsedSeconds;
    public boolean timed;

    @Enumerated(EnumType.STRING)
    public EstadoTarea state = EstadoTarea.PENDING;

    public Instant startedAt;
    public Instant createdAt;
    public Instant completedAt;
}
