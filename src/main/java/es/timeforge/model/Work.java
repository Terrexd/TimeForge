package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Obra de ocio digital dentro del catálogo (Videojuego, Manga, Anime, etc.).
 */
@Entity(name = "Work")
@Table(name = "media_work", uniqueConstraints = @UniqueConstraint(columnNames = {"canonicalTitle", "type"}))
public class Work extends EntityBase {

    public String title;
    public String canonicalTitle;

    @Enumerated(EnumType.STRING)
    public MediaType type;

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
}
