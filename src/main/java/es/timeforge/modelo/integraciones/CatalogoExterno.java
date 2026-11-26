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
    private final CatalogoCine cine;

    public CatalogoExterno(ClienteJson remote, ServicioCatalogo catalog,
            CatalogoIgdb igdb, CatalogoCine cine) {
        this.remote = remote;
        this.catalog = catalog;
        this.igdb = igdb;
        this.cine = cine;
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
        var juegos = titulo.isBlank() ? remote.get("https://store.steampowered.com/api/featuredcategories?l=english&cc=US")
                .path("specials").path("items") : resultadosSteam(titulo);
        Set<Long> vistos = new HashSet<>();
        for (var item : juegos) {
            String nombre = item.path("name").asText();
            long id = item.path("id").asLong();
            if (nombre.isBlank() || id <= 0 || !vistos.add(id)) continue;
            String imagen = item.path("header_image").asText(item.path("tiny_image").asText(""));
            if (imagen.isBlank()) imagen = imagenSteam(id);
            Map<String, Object> ficha = new HashMap<>(Map.of("id", id, "title", nombre, "source", "STEAM", "imagen", imagen));
            ficha.put("horasHistoriaPrincipal", HistoriasPrincipales.horas(nombre));
            resultados.add(ficha);
        }
        if (titulo.isBlank()) resultados.sort(Comparator.comparing(f -> f.get("horasHistoriaPrincipal") == null));
        return resultados;
    }

    private static String imagenSteam(long id) {
        return "https://cdn.akamai.steamstatic.com/steam/apps/" + id + "/header.jpg";
    }

    public List<Map<String, Object>> search(String source, String query) {
        ErrorNegocio.require(query.isEmpty() || query.length() >= 2 && query.length() <= 100,
                "La búsqueda debe contener entre 2 y 100 caracteres");
        try {
            if ("VIDEOJUEGOS".equals(source)) return buscarVideojuegos(query);
            if ("CINE_SERIES".equals(source) || "CINE_PELICULAS".equals(source)) return cine.buscar(source, query);
            String base = endpoint(source);
            try {
                return searchAniList(source, query);
            } catch (InterruptedException error) {
                throw error;
            } catch (Exception error) {
                // Los resultados de respaldo llevan el identificador de MyAnimeList.
            }
            String url = query.isEmpty() ? base.replace("/v4/", "/v4/top/") + "?limit=20&sfw=true"
                    : base + "?limit=20&sfw=true&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            List<Map<String, Object>> out = new ArrayList<>();
            for (var item : remote.get(url).path("data")) {
                out.add(Map.of("id", item.path("mal_id").asLong(), "title", item.path("title").asText(), "source", source,
                        "imagen", item.path("images").path("jpg").path("image_url").asText(""),
                        "minutos", ConversorCatalogo.minutos(item.path("duration").asText("")),
                        "unidades", item.path(source.endsWith("_MANGA") ? "chapters" : "episodes").asInt()));
            }
            return out;
        } catch (ErrorNegocio e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ErrorNegocio(503,
                    "El catálogo externo no responde. Reintenta en unos segundos o añade la obra manualmente");
        }
    }

    public Progreso importWork(long user, String source, long id) {
        try {
            if (source.startsWith("ANILIST_")) return importAniList(user, source, id);
            if (source.startsWith("CINE_")) {
                var ficha = cine.ficha(source, id);
                byte[] portada = optionalCover(ficha.coverUrl());
                return catalog.guardarObraEnBiblioteca(user, ficha.input(), source, Long.toString(id), portada,
                        portada == null ? null : ClienteJson.tipoImagen(portada));
            }
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
            return catalog.guardarObraEnBiblioteca(user, imported.input(), source, Long.toString(id), cover,
                    cover == null ? null : ClienteJson.tipoImagen(cover), usarIgdb ? igdb.duraciones(List.of(id)).get(id) : null);
        } catch (ErrorNegocio e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ErrorNegocio(503, "No se pudo importar la obra. Puedes registrarla manualmente");
        }
    }

    private List<Map<String, Object>> searchAniList(String source, String query) throws Exception {
        String type = source.equals("JIKAN_MANGA") ? "MANGA" : "ANIME";
        Map<String, String> variables = new HashMap<>(Map.of("type", type));
        if (!query.isEmpty()) variables.put("q", query);
        String orden = query.isEmpty() ? "POPULARITY_DESC" : "SEARCH_MATCH";
        var payload = Map.of("query",
                "query ($q: String, $type: MediaType) { Page(page: 1, perPage: 20) { media(search: $q, type: $type, isAdult: false, sort: "
                        + orden + ") { id title { romaji english } coverImage { large } duration chapters episodes } } }",
                "variables", variables);
        var json = remote.post("https://graphql.anilist.co", payload);
        if (json.has("errors")) throw new IllegalStateException("AniList no pudo completar la consulta");
        List<Map<String, Object>> out = new ArrayList<>();
        for (var item : json.path("data").path("Page").path("media")) {
            long id = item.path("id").asLong();
            String title = item.path("title").path("english").asText("");
            if (title.isBlank()) {
                title = item.path("title").path("romaji").asText();
            }
            out.add(Map.of("id", id, "title", title, "source", "ANILIST_" + type,
                    "imagen", item.path("coverImage").path("large").asText(""), "minutos", item.path("duration").asInt(),
                    "unidades", item.path(type.equals("MANGA") ? "chapters" : "episodes").asInt()));
        }
        return out;
    }

    private Progreso importAniList(long user, String source, long id) throws Exception {
        String tipo = source.endsWith("_MANGA") ? "MANGA" : "ANIME";
        var payload = Map.of("query",
                "query ($id: Int, $type: MediaType) { Media(id: $id, type: $type) { id title { romaji english native } description(asHtml: false) genres duration chapters episodes coverImage { large } } }",
                "variables", Map.of("id", id, "type", tipo));
        var json = remote.post("https://graphql.anilist.co", payload);
        var media = json.path("data").path("Media");
        var imported = ConversorCatalogo.mapAniList(source, media);
        byte[] cover = optionalCover(imported.coverUrl());
        return catalog.guardarObraEnBiblioteca(user, imported.input(), source, Long.toString(id), cover,
                cover == null ? null : ClienteJson.tipoImagen(cover));
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
