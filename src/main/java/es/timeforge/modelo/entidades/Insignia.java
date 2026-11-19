package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Logro / Insignia desbloqueada por constancia, puntualidad o finalización de obras.
 */
@Entity(name = "Insignia")
@Table(name = "badge", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id", "code" }))
public class Insignia extends EntidadBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Usuario owner;

    public String code;
    public String title;
    public Instant createdAt;
}
