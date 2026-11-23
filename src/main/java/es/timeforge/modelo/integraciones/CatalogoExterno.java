package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.servicios.*;
import es.timeforge.modelo.DatosEntrada.DatosObra;
import es.timeforge.modelo.HistoriasPrincipales;
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
    private final CatalogoIgdb igdb;

    public CatalogoExterno(ClienteJson remote, ServicioCatalogo catalog,
            @Value("${timeforge.rawg-key}") String rawgKey, CatalogoIgdb igdb) {
        this.remote = remote;
        this.catalog = catalog;
        this.rawgKey = rawgKey.strip();
        this.igdb = igdb;
    }

    private String endpoint(String source) {
        return switch (source) {
        case "JIKAN_ANIME" -> "https://api.jikan.moe/v4/anime";
        case "JIKAN_MANGA" -> "https://api.jikan.moe/v4/manga";
        case "RAWG" -> {
            if (rawgKey.isEmpty()) {
                throw new ErrorNegocio(503, "Falta la clave de RAWG en el servidor. Guarda RAWG_API_KEY en el archivo .env junto a iniciar.bat y reinicia TimeForge.");
            }
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
        try {
            if ("IGDB".equals(source)) return igdb.buscar(query);
            String url = endpoint(source) + "?" + (source.equals("RAWG") ? "page_size=12&search=" : "limit=12&q=")
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            var json = remote.get(conClaveRawg(url, source));
            List<Map<String, Object>> out = new ArrayList<>();
            for (var item : json.path(source.equals("RAWG") ? "results" : "data")) {
                boolean juego = source.equals("RAWG");
                String titulo = item.path(juego ? "name" : "title").asText();
                Map<String, Object> ficha = new HashMap<>();
                ficha.put("id", item.path(juego ? "id" : "mal_id").asLong());
                ficha.put("title", titulo);
                ficha.put("source", source);
                if (juego) {
                    String imagen = item.path("background_image").asText("");
                    for (var captura : item.path("short_screenshots")) {
                        String urlCaptura = captura.path("image").asText("");
                        if (captura.path("id").asLong() > 0 && urlCaptura.startsWith("https://media.rawg.io/")) {
                            imagen = urlCaptura;
                            break;
                        }
                    }
                    if (imagen.startsWith("https://media.rawg.io/")) ficha.put("imagen", imagen);
                    ficha.put("horasHistoriaPrincipal", HistoriasPrincipales.horas(titulo));
                }
                out.add(ficha);
            }
            return out;
        } catch (ErrorNegocio e) {
            throw e;
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
        try {
            boolean usarIgdb = "IGDB".equals(source);
            var datos = usarIgdb ? igdb.juego(id) : remote.get(conClaveRawg(endpoint(source) + "/" + id, source));
            var imported = ConversorCatalogo.map(source, datos);
            byte[] cover = optionalCover(imported.coverUrl());
            return catalog.addImported(user, imported.input(), source, Long.toString(id), cover,
                    cover == null ? null : ClienteJson.tipoImagen(cover), usarIgdb ? igdb.duraciones(List.of(id)).get(id) : null);
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

    /** Recupera también las portadas de juegos añadidos a mano o importados sin imagen. */
    public byte[] portadaJuego(Obra obra) {
        if ("IGDB".equals(obra.source)) {
            try {
                byte[] portada = optionalCover(CatalogoIgdb.imagen(igdb.juego(Long.parseLong(obra.externalId))));
                if (portada != null) return portada;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception ignored) {
                // Si IGDB no está disponible, se consulta Steam.
            }
        }
        if ("RAWG".equals(obra.source) && !rawgKey.isBlank() && obra.externalId != null && obra.externalId.matches("[0-9]+")) {
            try {
                var ficha = remote.get(conClaveRawg(endpoint("RAWG") + "/" + obra.externalId, "RAWG"));
                byte[] imagen = optionalCover(ficha.path("background_image").asText(""));
                if (imagen != null) return imagen;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception ignored) {
                // También hay juegos con portada disponible en Steam.
            }
        }
        try {
            String titulo = URLEncoder.encode(obra.title, StandardCharsets.UTF_8);
            var juegos = remote.get("https://store.steampowered.com/api/storesearch/?term=" + titulo + "&l=english&cc=US");
            String buscado = HistoriasPrincipales.normalizar(obra.title);
            for (var juego : juegos.path("items")) {
                if (buscado.equals(HistoriasPrincipales.normalizar(juego.path("name").asText())) && juego.path("id").asLong() > 0) {
                    byte[] imagen = optionalCover("https://cdn.akamai.steamstatic.com/steam/apps/" + juego.get("id").asLong() + "/header.jpg");
                    return imagen != null ? imagen : optionalCover(juego.path("tiny_image").asText(""));
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // La biblioteca sigue mostrando el título si la tienda está fuera de línea.
        }
        return null;
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
