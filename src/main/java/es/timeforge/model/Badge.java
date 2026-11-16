package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Logro / Insignia desbloqueada por constancia, puntualidad o finalización de obras.
 */
@Entity(name = "Badge")
@Table(name = "badge", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "code"}))
public class Badge extends EntityBase {

    @ManyToOne(optional = false)
    @JsonIgnore
    public Account owner;

    public String code;
    public String title;
    public Instant createdAt;
}
