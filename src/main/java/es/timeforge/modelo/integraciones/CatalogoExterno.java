package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.servicios.*;
import es.timeforge.modelo.DatosEntrada.DatosObra;
import es.timeforge.modelo.HistoriasPrincipales;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * Servicio de sincronización y búsqueda contra catálogos externos.
 */
@Service
public class CatalogoExterno {

    private final ClienteJson remote;
    private final ServicioCatalogo catalog;
    private final CatalogoIgdb igdb;

    public CatalogoExterno(ClienteJson remote, ServicioCatalogo catalog,
            CatalogoIgdb igdb) {
        this.remote = remote;
        this.catalog = catalog;
        this.igdb = igdb;
    }

    private String endpoint(String source) {
        return switch (source) {
        case "JIKAN_ANIME" -> "https://api.jikan.moe/v4/anime";
        case "JIKAN_MANGA" -> "https://api.jikan.moe/v4/manga";
        default -> throw new ErrorNegocio("Fuente desconocida: " + source);
        };
    }

    private JsonNode resultadosSteam(String titulo) throws Exception {
        String termino = URLEncoder.encode(titulo, StandardCharsets.UTF_8);
        return remote.get("https://store.steampowered.com/api/storesearch/?term=" + termino + "&l=english&cc=US").path("items");
    }

    private List<Map<String, Object>> buscarVideojuegos(String titulo) throws Exception {
        if (igdb.configurado()) {
            try {
                return igdb.buscar(titulo);
            } catch (InterruptedException error) {
                throw error;
            } catch (Exception error) {
                // Steam sigue disponible si falla el acceso a IGDB.
            }
        }
        List<Map<String, Object>> resultados = new ArrayList<>();
        for (var item : resultadosSteam(titulo)) {
            String nombre = item.path("name").asText();
            long id = item.path("id").asLong();
            if (nombre.isBlank() || id <= 0) continue;
            Map<String, Object> ficha = new HashMap<>(Map.of("id", id, "title", nombre, "source", "STEAM", "imagen", imagenSteam(id)));
            ficha.put("horasHistoriaPrincipal", HistoriasPrincipales.horas(nombre));
            resultados.add(ficha);
        }
        return resultados;
    }

    private static String imagenSteam(long id) {
        return "https://cdn.akamai.steamstatic.com/steam/apps/" + id + "/header.jpg";
    }

    public List<Map<String, Object>> search(String source, String query) {
        ErrorNegocio.require(query.length() >= 2 && query.length() <= 100,
                "La búsqueda debe contener entre 2 y 100 caracteres");
        try {
            if ("VIDEOJUEGOS".equals(source)) return buscarVideojuegos(query);
            String url = endpoint(source) + "?limit=12&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            List<Map<String, Object>> out = new ArrayList<>();
            for (var item : remote.get(url).path("data")) {
                out.add(Map.of("id", item.path("mal_id").asLong(), "title", item.path("title").asText(), "source", source));
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
            JsonNode datos;
            if ("STEAM".equals(source)) {
                var ficha = remote.get("https://store.steampowered.com/api/appdetails?appids=" + id + "&l=english&cc=US").path(Long.toString(id));
                ErrorNegocio.require(ficha.path("success").asBoolean(), "Esta ficha ya no está disponible en Steam");
                datos = ficha.path("data");
            } else {
                datos = usarIgdb ? igdb.juego(id) : remote.get(endpoint(source) + "/" + id);
            }
            var imported = ConversorCatalogo.map(source, datos);
            byte[] cover = optionalCover(imported.coverUrl());
            if (cover == null && "STEAM".equals(source)) cover = optionalCover(imagenSteam(id));
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
        if ("STEAM".equals(obra.source) && obra.externalId != null && obra.externalId.matches("[0-9]{1,12}"))
            return optionalCover(imagenSteam(Long.parseLong(obra.externalId)));
        try {
            String buscado = HistoriasPrincipales.normalizar(obra.title);
            for (var juego : resultadosSteam(obra.title)) {
                if (buscado.equals(HistoriasPrincipales.normalizar(juego.path("name").asText())) && juego.path("id").asLong() > 0) {
                    byte[] imagen = optionalCover(imagenSteam(juego.get("id").asLong()));
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
