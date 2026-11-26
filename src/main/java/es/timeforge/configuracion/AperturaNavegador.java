package es.timeforge.configuracion;

import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "timeforge.abrir-navegador", havingValue = "true")
public class AperturaNavegador {
    private static final Logger LOG = LoggerFactory.getLogger(AperturaNavegador.class);

    @EventListener
    public void abrir(ApplicationReadyEvent evento) {
        var contexto = (ServletWebServerApplicationContext) evento.getApplicationContext();
        var servidor = contexto.getBean(ServerProperties.class);
        var direccion = servidor.getAddress();
        String host = direccion == null || direccion.isAnyLocalAddress() ? "localhost" : direccion.getHostAddress();
        URI url = UriComponentsBuilder.newInstance().scheme("http").host(host)
                .port(contexto.getWebServer().getPort()).path("/").build().toUri();
        LOG.info("TimeForge listo en {} · Base de datos en {}bbdd", url, url);
        try {
            new ProcessBuilder("rundll32.exe", "url.dll,FileProtocolHandler", url.toString()).start();
        } catch (IOException e) {
            LOG.warn("Abre TimeForge en tu navegador: {}", url);
        }
    }
}
