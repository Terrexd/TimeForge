package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Justificación y recomendación generada por «El Oráculo» (IA) para un descanso.
 */
@Entity(name = "Recommendation")
@Table(name = "ai_recommendation")
public class Recommendation extends EntityBase {

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @JsonIgnore
    public Rest rest;

    @ManyToOne
    @JsonIgnore
    public Task previousTask;

    @Column(length = 4000)
    public String text;

    public String source = "FALLBACK";
    public Instant createdAt;
}
