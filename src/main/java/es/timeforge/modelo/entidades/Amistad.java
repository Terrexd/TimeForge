package es.timeforge.modelo.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Vínculo de amistad bidireccional entre dos usuarios.
 */
@Entity(name = "Amistad")
@Table(name = "friendship", uniqueConstraints = @UniqueConstraint(columnNames = { "lowId", "highId" }))
public class Amistad extends EntidadBase {

    public long lowId;
    public long highId;
    public long senderId;
    public long receiverId;

    @Enumerated(EnumType.STRING)
    public EstadoAmistad state = EstadoAmistad.PENDING;

    public Instant createdAt;
}
