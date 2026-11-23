package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class ClienteJson {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper mapper;

    public ClienteJson(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode get(String uri) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(8)).GET().build());
    }

    public JsonNode post(String uri, String key, Object body) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json").header("x-goog-api-key", key)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build());
    }

    public JsonNode post(String uri, Object body) throws Exception {
        return send(HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(4))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build());
    }

    public JsonNode tokenTwitch(String id, String secreto) throws Exception {
        String datos = "grant_type=client_credentials&client_id=" + URLEncoder.encode(id, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(secreto, StandardCharsets.UTF_8);
        return send(HttpRequest.newBuilder(URI.create("https://id.twitch.tv/oauth2/token")).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(datos)).build());
    }

    public JsonNode consultaIgdb(String recurso, String id, String token, String consulta) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("https://api.igdb.com/v4/" + recurso)).timeout(Duration.ofSeconds(8))
                .header("Client-ID", id).header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(consulta)).build());
    }

    private JsonNode send(HttpRequest request) throws Exception {
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IllegalStateException("Servicio externo: HTTP " + response.statusCode());
        return mapper.readTree(response.body());
    }

    public byte[] cover(String url) throws Exception {
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (!"https".equals(uri.getScheme()) || host == null || !(host.equals("cdn.myanimelist.net")
                || host.equals("cdn.akamai.steamstatic.com") || host.equals("shared.akamai.steamstatic.com")
                || host.equals("images.igdb.com") || host.equals("images.metahub.space") || host.endsWith(".anilist.co")))
            return null;
        var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200 || response.body().length > 2_000_000)
            return null;
        return response.body();
    }

    public static String tipoImagen(byte[] datos) {
        if (datos.length >= 8 && datos[0] == (byte) 0x89 && datos[1] == 'P') return "image/png";
        if (datos.length >= 12 && datos[0] == 'R' && datos[8] == 'W') return "image/webp";
        if (datos.length >= 6 && datos[0] == 'G' && datos[1] == 'I') return "image/gif";
        return "image/jpeg";
    }
}
