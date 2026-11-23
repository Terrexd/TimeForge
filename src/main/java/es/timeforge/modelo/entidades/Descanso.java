package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registro de una sesión de descanso canjeada en la ruleta de ocio.
 */
@Entity(name = "Descanso")
@Table(name = "rest_session")
public class Descanso extends EntidadBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Usuario owner;

    @ManyToOne(optional = false)
    public Obra work;

    public int minutes;
    public int cost;
    public Instant startedAt;
    public Instant endsAt;
    public boolean completed;
    public boolean cancelled;
    @jakarta.persistence.Column(columnDefinition = "boolean default false")
    public boolean paused = false;
    @jakarta.persistence.Column(columnDefinition = "integer default 0")
    public int remainingSeconds = 0;
    public Long partnerId;
}
