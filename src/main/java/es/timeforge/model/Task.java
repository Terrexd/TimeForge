package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Tarea productiva gestionada por un usuario.
 */
@Entity(name = "Task")
@Table(name = "task", indexes = @Index(columnList = "owner_id,state"))
public class Task extends EntityBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Account owner;

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
    public TaskState state = TaskState.PENDING;

    public Instant startedAt;
    public Instant createdAt;
    public Instant completedAt;
}
