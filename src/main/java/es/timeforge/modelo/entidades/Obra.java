package es.timeforge.modelo.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Obra de ocio digital dentro del catálogo (Videojuego, Manga, Anime, etc.).
 */
@Entity(name = "Obra")
@Table(name = "media_work", uniqueConstraints = @UniqueConstraint(columnNames = { "canonicalTitle", "type" }))
public class Obra extends EntidadBase {

    public String title;
    public String canonicalTitle;

    @Enumerated(EnumType.STRING)
    public TipoMedio type;

    @Column(length = 8000)
    public String synopsis = "";

    public String genres = "";
    public String tags = "";
    public String pace = "Sin clasificar";
    public String externalId = "";
    public String source = "MANUAL";

    public int totalUnits;
    public int minutesPerUnit;
    public boolean quickSession;

    @Lob
    @JsonIgnore
    public byte[] cover;

    public String coverType;

    public Double getHorasHistoriaPrincipal() {
        return type == TipoMedio.GAME ? es.timeforge.modelo.HistoriasPrincipales.horas(title) : null;
    }

    public double objetivoProgreso() {
        if (type != TipoMedio.GAME) return totalUnits;
        Double horas = getHorasHistoriaPrincipal();
        return horas == null ? 0 : horas;
    }
}
