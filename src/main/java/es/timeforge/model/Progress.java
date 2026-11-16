package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Progreso individual de un usuario sobre una obra concreta de su catálogo.
 */
@Entity(name = "Progress")
@Table(name = "media_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "work_id"}))
public class Progress extends EntityBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Account owner;

    @ManyToOne(optional = false)
    public Work work;

    @Enumerated(EnumType.STRING)
    public ProgressState state = ProgressState.PENDING;

    public double currentUnits;
    public int volume;
    public int season;
    public String platform = "";
    public Instant lastInteraction;
}
