package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import es.timeforge.modelo.DatosEntrada.DatosObra;
import es.timeforge.modelo.entidades.TipoMedio;
import es.timeforge.modelo.servicios.ErrorNegocio;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class CatalogoCine {
    private final ClienteJson cliente;

    public CatalogoCine(ClienteJson cliente) {
        this.cliente = cliente;
    }

    private String tipo(String fuente) {
        return "CINE_SERIES".equals(fuente) ? "series" : "movie";
    }

    public List<Map<String, Object>> buscar(String fuente, String texto) throws Exception {
        String ruta = "/catalog/" + tipo(fuente) + "/top";
        String url = texto.isBlank() ? "https://cinemeta-catalogs.strem.io/top" + ruta + ".json"
                : "https://v3-cinemeta.strem.io" + ruta + "/search="
                    + URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20") + ".json";
        List<Map<String, Object>> lista = new ArrayList<>();
        for (var obra : cliente.get(url).path("metas")) {
            String id = obra.path("id").asText();
            if (!id.matches("tt[0-9]{7,12}")) continue;
            lista.add(Map.of("id", Long.parseLong(id.substring(2)), "source", fuente, "title", obra.path("name").asText(),
                    "imagen", obra.path("poster").asText(""), "anio", obra.path("year").asText(""),
                    "minutos", minutos(obra)));
            if (lista.size() == 24) break;
        }
        return lista;
    }

    public ConversorCatalogo.ImportedWork ficha(String fuente, long id) throws Exception {
        String imdb = String.format(Locale.ROOT, "tt%07d", id);
        var obra = cliente.get("https://v3-cinemeta.strem.io/meta/" + tipo(fuente) + "/" + imdb + ".json").path("meta");
        String nombre = obra.path("name").asText("");
        ErrorNegocio.require(imdb.equals(obra.path("id").asText()) && !nombre.isBlank(), "La ficha ya no está disponible");
        boolean serie = "CINE_SERIES".equals(fuente);
        int episodios = 0;
        for (var episodio : obra.path("videos"))
            if (episodio.path("season").asInt() > 0 && episodio.path("episode").asInt() > 0) episodios++;
        StringJoiner generos = new StringJoiner(", ");
        obra.path("genres").forEach(g -> generos.add(g.asText()));
        var datos = new DatosObra(nombre.substring(0, Math.min(150, nombre.length())), serie ? TipoMedio.SERIES : TipoMedio.MOVIE,
                ConversorCatalogo.limit(obra.path("description").asText(""), 8000), ConversorCatalogo.limit(generos.toString(), 250),
                serie ? Math.max(1, episodios) : 1, minutos(obra), false);
        return new ConversorCatalogo.ImportedWork(datos, obra.path("poster").asText(""));
    }

    private int minutos(JsonNode obra) {
        return ConversorCatalogo.minutos(obra.path("runtime").asText(""));
    }
}
