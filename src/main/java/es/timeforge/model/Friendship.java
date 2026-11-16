package es.timeforge.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * Vínculo de amistad bidireccional entre dos usuarios.
 */
@Entity(name = "Friendship")
@Table(name = "friendship", uniqueConstraints = @UniqueConstraint(columnNames = {"lowId", "highId"}))
public class Friendship extends EntityBase {

    public long lowId;
    public long highId;
    public long senderId;
    public long receiverId;

    @Enumerated(EnumType.STRING)
    public FriendshipState state = FriendshipState.PENDING;

    public Instant createdAt;
}
