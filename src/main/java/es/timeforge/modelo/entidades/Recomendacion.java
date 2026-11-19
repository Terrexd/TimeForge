package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Justificación y recomendación generada por «El Oráculo» (IA) para un descanso.
 */
@Entity(name = "Recomendacion")
@Table(name = "ai_recommendation")
public class Recomendacion extends EntidadBase {

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    @JsonIgnore
    public Descanso rest;

    @ManyToOne
    @JsonIgnore
    public Tarea previousTask;

    @Column(length = 4000)
    public String text;

    public String source = "FALLBACK";
    public Instant createdAt;
}
