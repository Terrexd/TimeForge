package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.servicios.*;
import es.timeforge.modelo.DatosEntrada.DatosObra;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de sincronización y búsqueda contra catálogos externos (RAWG, Jikan, AniList).
 */
@Service
public class CatalogoExterno {

    private final ClienteJson remote;
    private final ServicioCatalogo catalog;
    private final String rawgKey;

    public CatalogoExterno(ClienteJson remote, ServicioCatalogo catalog,
            @Value("${timeforge.rawg-key}") String rawgKey) {
        this.remote = remote;
        this.catalog = catalog;
        this.rawgKey = rawgKey;
    }

    private String endpoint(String source) {
        return switch (source) {
        case "JIKAN_ANIME" -> "https://api.jikan.moe/v4/anime";
        case "JIKAN_MANGA" -> "https://api.jikan.moe/v4/manga";
        case "RAWG" -> {
            ErrorNegocio.require(!rawgKey.isBlank(),
                    "Configura RAWG_API_KEY para buscar videojuegos; el alta manual sigue disponible");
            yield "https://api.rawg.io/api/games";
        }
        default -> throw new ErrorNegocio("Fuente desconocida: " + source);
        };
    }

    private String conClaveRawg(String url, String source) {
        if (!source.equals("RAWG")) return url;
        String separador = url.contains("?") ? "&" : "?";
        return url + separador + "key=" + URLEncoder.encode(rawgKey, StandardCharsets.UTF_8);
    }

    public List<Map<String, Object>> search(String source, String query) {
        ErrorNegocio.require(query.length() >= 2 && query.length() <= 100,
                "La búsqueda debe contener entre 2 y 100 caracteres");
        String url = endpoint(source) + "?" + (source.equals("RAWG") ? "page_size=12&search=" : "limit=12&q=")
                + URLEncoder.encode(query, StandardCharsets.UTF_8);
        try {
            var json = remote.get(conClaveRawg(url, source));
            List<Map<String, Object>> out = new ArrayList<>();
            for (var item : json.path(source.equals("RAWG") ? "results" : "data")) {
                out.add(Map.of("id", item.path(source.equals("RAWG") ? "id" : "mal_id").asLong(), "title",
                        item.path(source.equals("RAWG") ? "name" : "title").asText(), "source", source));
            }
            return out;
        } catch (Exception e) {
            if (source.startsWith("JIKAN_")) {
                try {
                    return searchAniList(source, query);
                } catch (Exception ignored) {
                }
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ErrorNegocio(503,
                    "El catálogo externo no responde. Reintenta en unos segundos o añade la obra manualmente");
        }
    }

    public Progreso importWork(long user, String source, long id) {
        String url = conClaveRawg(endpoint(source) + "/" + id, source);
        try {
            var imported = ConversorCatalogo.map(source, remote.get(url));
            byte[] cover = optionalCover(imported.coverUrl());
            return catalog.addImported(user, imported.input(), source, Long.toString(id), cover,
                    "image/jpeg");
        } catch (ErrorNegocio e) {
            throw e;
        } catch (Exception e) {
            if (source.startsWith("JIKAN_")) {
                try {
                    return importAniList(user, source, id);
                } catch (Exception ignored) {
                }
            }
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ErrorNegocio(503, "No se pudo importar la obra. Puedes registrarla manualmente");
        }
    }

    private List<Map<String, Object>> searchAniList(String source, String query) throws Exception {
        String type = source.equals("JIKAN_MANGA") ? "MANGA" : "ANIME";
        var payload = Map.of("query",
                "query ($q: String, $type: MediaType) { Page(page: 1, perPage: 12) { media(search: $q, type: $type) { id title { romaji english } } } }",
                "variables", Map.of("q", query, "type", type));
        var json = remote.post("https://graphql.anilist.co", payload);
        List<Map<String, Object>> out = new ArrayList<>();
        for (var item : json.path("data").path("Page").path("media")) {
            long id = item.path("id").asLong();
            String title = item.path("title").path("english").asText("");
            if (title.isBlank()) {
                title = item.path("title").path("romaji").asText();
            }
            out.add(Map.of("id", id, "title", title, "source", source));
        }
        return out;
    }

    private Progreso importAniList(long user, String source, long id) throws Exception {
        var payload = Map.of("query",
                "query ($id: Int) { Media(id: $id) { id title { romaji english native } description(asHtml: false) genres chapters episodes coverImage { large } type } }",
                "variables", Map.of("id", id));
        var json = remote.post("https://graphql.anilist.co", payload);
        var media = json.path("data").path("Media");
        var imported = ConversorCatalogo.mapAniList(source, media);
        byte[] cover = optionalCover(imported.coverUrl());
        return catalog.addImported(user, imported.input(), source, Long.toString(id), cover, "image/jpeg");
    }

    private byte[] optionalCover(String url) {
        if (url == null || url.isBlank())
            return null;
        try {
            return remote.cover(url);
        } catch (Exception error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }
}
