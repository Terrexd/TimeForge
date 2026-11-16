package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entidad Usuario de la plataforma TimeForge.
 */
@Entity(name = "Account")
@Table(name = "tf_user")
public class Account extends EntityBase {

    @Column(nullable = false, unique = true, length = 40)
    public String username;

    @Column(nullable = false, unique = true, length = 160)
    public String email;

    @JsonIgnore
    @Column(nullable = false)
    public String passwordHash;

    public String displayName = "";
    public String avatar = "⚒";
    public String bio = "";
    public String role = "USER";

    public boolean active = true;
    public boolean deleted = false;

    public int xp = 0;
    public int streak = 0;

    public LocalDate lastActiveDay;
    public Instant createdAt;
}
