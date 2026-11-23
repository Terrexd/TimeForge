package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import es.timeforge.modelo.entidades.TipoMedio;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.DatosEntrada.DatosObra;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/** Adaptación pura de IGDB, Steam y Jikan al dominio, independiente de HTTP y persistencia. */
public final class ConversorCatalogo {
    private static final Pattern HOURS = Pattern.compile("(\\d+)\\s*h");
    private static final Pattern MINUTES = Pattern.compile("(\\d+)\\s*min");

    private ConversorCatalogo() {
    }

    public record ImportedWork(DatosObra input, String coverUrl) {
    }

    public static ImportedWork map(String source, JsonNode response) {
        boolean igdb = source.equals("IGDB");
        boolean steam = source.equals("STEAM");
        boolean game = igdb || steam;
        boolean manga = source.endsWith("_MANGA");
        JsonNode data = game ? response : response.path("data");
        String title = data.path(game ? "name" : "title").asText();
        ErrorNegocio.require(!title.isBlank(), "La ficha externa no contiene un título válido");
        String synopsis = data.path(igdb ? "summary" : steam ? "short_description" : "synopsis").asText("");
        StringJoiner genres = new StringJoiner(", ");
        for (var genre : data.path("genres"))
            genres.add(genre.path(steam ? "description" : "name").asText());
        int total = game ? 0
                : data.path(manga ? "chapters" : "episodes").asInt(1);
        int duration = game ? 60 : manga ? 10 : episodeMinutes(data.path("duration").asText());
        String cover = igdb ? CatalogoIgdb.imagen(data) : steam ? data.path("header_image").asText("")
                : data.path("images").path("jpg").path("image_url").asText();
        var input = new DatosObra(limit(title, 150),
                game ? TipoMedio.GAME : manga ? TipoMedio.MANGA : TipoMedio.ANIME, limit(synopsis, 8000),
                limit(genres.toString(), 250), game ? 0 : Math.max(1, total), duration, false);
        return new ImportedWork(input, cover);
    }

    public static ImportedWork mapAniList(String source, JsonNode media) {
        boolean manga = source.endsWith("_MANGA");
        String title = media.path("title").path("english").asText("");
        if (title.isBlank())
            title = media.path("title").path("romaji").asText("");
        if (title.isBlank())
            title = media.path("title").path("native").asText("");
        ErrorNegocio.require(!title.isBlank(), "La ficha externa no contiene un título válido");
        String synopsis = media.path("description").asText("");
        StringJoiner genres = new StringJoiner(", ");
        for (var g : media.path("genres"))
            genres.add(g.asText());
        int total = media.path(manga ? "chapters" : "episodes").asInt(1);
        int duration = manga ? 10 : media.path("duration").asInt();
        String cover = media.path("coverImage").path("large").asText("");
        var input = new DatosObra(limit(title, 150), manga ? TipoMedio.MANGA : TipoMedio.ANIME,
                limit(synopsis, 8000), limit(genres.toString(), 250), Math.max(1, total), duration, false);
        return new ImportedWork(input, cover);
    }

    public static int episodeMinutes(String value) {
        int minutos = minutos(value);
        return minutos > 0 ? minutos : 24;
    }

    static int minutos(String value) {
        var hours = HOURS.matcher(value);
        var minutes = MINUTES.matcher(value);
        int result = 0;
        if (hours.find())
            result += Integer.parseInt(hours.group(1)) * 60;
        if (minutes.find())
            result += Integer.parseInt(minutes.group(1));
        return result;
    }

    static String limit(String value, int length) {
        return value.substring(0, Math.min(length, value.length()));
    }
}
