package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registro de una sesión de descanso canjeada en la ruleta de ocio.
 */
@Entity(name = "Rest")
@Table(name = "rest_session")
public class Rest extends EntityBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Account owner;

    @ManyToOne(optional = false)
    public Work work;

    public int minutes;
    public int cost;
    public Instant startedAt;
    public Instant endsAt;
    public boolean completed;
    public boolean cancelled;
    public Long partnerId;
}
