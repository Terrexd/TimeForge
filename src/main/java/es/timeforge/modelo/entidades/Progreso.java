package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Progreso individual de un usuario sobre una obra concreta de su catálogo.
 */
@Entity(name = "Progreso")
@Table(name = "media_progress", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id",
        "work_id" }))
public class Progreso extends EntidadBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Usuario owner;

    @ManyToOne(optional = false)
    public Obra work;

    @Enumerated(EnumType.STRING)
    public EstadoProgreso state = EstadoProgreso.PENDING;

    public double currentUnits;
    public int volume;
    public int season;
    public String platform = "";
    public Instant lastInteraction;
}
