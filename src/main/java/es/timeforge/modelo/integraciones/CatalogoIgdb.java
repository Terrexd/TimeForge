package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.JsonNode;
import es.timeforge.modelo.HistoriasPrincipales;
import es.timeforge.modelo.servicios.ErrorNegocio;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CatalogoIgdb {
    private final ClienteJson cliente;
    private final String identificador;
    private final String secreto;
    private String token;
    private Instant caducidad = Instant.EPOCH;

    public CatalogoIgdb(ClienteJson cliente, @Value("${IGDB_CLIENT_ID:}") String identificador,
            @Value("${IGDB_CLIENT_SECRET:}") String secreto) {
        this.cliente = cliente;
        this.identificador = identificador.strip();
        this.secreto = secreto.strip();
    }

    private synchronized String token() throws Exception {
        if (identificador.isEmpty() || secreto.isEmpty())
            throw new ErrorNegocio(503, "Configura IGDB_CLIENT_ID e IGDB_CLIENT_SECRET en .env y reinicia TimeForge.");
        if (Instant.now().isAfter(caducidad)) {
            var respuesta = cliente.tokenTwitch(identificador, secreto);
            token = respuesta.path("access_token").asText("");
            if (token.isBlank()) throw new IllegalStateException("Twitch no devolvió un token");
            caducidad = Instant.now().plusSeconds(respuesta.path("expires_in").asLong() - 60);
        }
        return token;
    }

    private JsonNode juegos(String filtro) throws Exception {
        return cliente.consultaIgdb("games", identificador, token(),
                "fields name,first_release_date,summary,genres.name,cover.image_id,screenshots.image_id; " + filtro);
    }

    public JsonNode juego(long id) throws Exception {
        var juego = juegos("where id = " + id + "; limit 1;").path(0);
        ErrorNegocio.require(juego.path("id").asLong() == id && !juego.path("name").asText("").isBlank(),
                "IGDB no encontró el juego seleccionado");
        return juego;
    }

    public Map<Long, Double> duraciones(List<Long> ids) throws Exception {
        Map<Long, Double> resultado = new HashMap<>();
        if (ids.isEmpty()) return resultado;
        String lista = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        var tiempos = cliente.consultaIgdb("game_time_to_beats", identificador, token(),
                "fields game_id,hastily; where game_id = (" + lista + "); limit " + ids.size() + ";");
        for (var tiempo : tiempos) {
            long segundos = tiempo.path("hastily").asLong();
            if (segundos > 0) resultado.put(tiempo.path("game_id").asLong(), Math.max(0.1, Math.round(segundos / 360.0) / 10.0));
        }
        return resultado;
    }

    public static String imagen(JsonNode juego) {
        String id = juego.path("cover").path("image_id").asText("");
        if (id.isBlank()) id = juego.path("screenshots").path(0).path("image_id").asText("");
        return id.matches("[A-Za-z0-9_-]+") ? "https://images.igdb.com/igdb/image/upload/t_cover_big/" + id + ".jpg" : "";
    }

    public List<Map<String, Object>> buscar(String texto) throws Exception {
        String literal = texto.replace("\\", "\\\\").replace("\"", "\\\"");
        var juegos = juegos("search \"" + literal + "\"; limit 12;");
        List<Long> ids = new ArrayList<>();
        juegos.forEach(j -> ids.add(j.path("id").asLong()));
        var horas = duraciones(ids);
        List<Map<String, Object>> resultados = new ArrayList<>();
        for (var juego : juegos) {
            long id = juego.path("id").asLong();
            String titulo = juego.path("name").asText();
            Map<String, Object> ficha = new HashMap<>(Map.of("id", id, "title", titulo, "source", "IGDB", "imagen", imagen(juego)));
            if (juego.hasNonNull("first_release_date"))
                ficha.put("anio", Instant.ofEpochSecond(juego.get("first_release_date").asLong()).atZone(java.time.ZoneOffset.UTC).getYear());
            ficha.put("horasHistoriaPrincipal", horas.getOrDefault(id, HistoriasPrincipales.horas(titulo)));
            resultados.add(ficha);
        }
        return resultados;
    }
}
