package es.timeforge.modelo.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Registro de auditoría y métricas de consumo de la API de Inteligencia Artificial.
 */
@Entity(name = "LlamadaIA")
@Table(name = "ai_call")
public class LlamadaIA extends EntidadBase {

    public String operation;
    public String outcome;
    public long durationMs;
    public int inputTokens;
    public int outputTokens;
    public Instant createdAt;
}
