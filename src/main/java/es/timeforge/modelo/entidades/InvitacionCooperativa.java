package es.timeforge.modelo.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Invitación para una tirada de ruleta cooperativa entre amigos.
 */
@Entity(name = "InvitacionCooperativa")
@Table(name = "coop_invite")
public class InvitacionCooperativa extends EntidadBase {

    public long senderId;
    public long receiverId;
    public String senderName;
    public String state = "PENDING";
    public int minutes;
    public int cost;
    public Instant expiresAt;
}
