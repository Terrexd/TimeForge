package es.timeforge.modelo.integraciones;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
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
        return send(HttpRequest.newBuilder(URI.create(uri)).timeout(Duration.ofSeconds(2)).GET().build());
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
                || host.equals("media.rawg.io") || host.endsWith("anilist.co")))
            return null;
        var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200 || response.body().length > 2_000_000)
            return null;
        return response.body();
    }
}
