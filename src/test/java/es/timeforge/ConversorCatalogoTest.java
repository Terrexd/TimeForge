package es.timeforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.timeforge.modelo.integraciones.ConversorCatalogo;
import es.timeforge.modelo.entidades.TipoMedio;
import es.timeforge.modelo.servicios.ErrorNegocio;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConversorCatalogoTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void readsHoursAndMinutes() {
        assertEquals(125, ConversorCatalogo.episodeMinutes("2 hr 5 min"));
        assertEquals(24, ConversorCatalogo.episodeMinutes("24 min per ep"));
        assertEquals(24, ConversorCatalogo.episodeMinutes("Unknown"));
    }

    @Test
    void importsAnimeMetadata() throws Exception {
        var work = ConversorCatalogo.map("JIKAN_ANIME", json.readTree(
                "{\"data\":{\"title\":\"Serie\",\"episodes\":12,\"duration\":\"24 min per ep\",\"genres\":[{\"name\":\"Adventure\"}]}}"));
        assertEquals(TipoMedio.ANIME, work.input().type());
        assertEquals(12, work.input().totalUnits());
        assertEquals(24, work.input().minutesPerUnit());
        assertEquals("Adventure", work.input().genres());
    }

    @Test
    void unknownTotalsUseDocumentedMinimum() throws Exception {
        var work = ConversorCatalogo.map("JIKAN_MANGA",
                json.readTree("{\"data\":{\"title\":\"En publicación\",\"chapters\":null}}"));
        assertEquals(1, work.input().totalUnits());
    }

    @Test
    void invalidRemoteResponseDoesNotCreateBlankWork() throws Exception {
        var response = json.readTree("{}");
        assertThrows(ErrorNegocio.class, () -> ConversorCatalogo.map("STEAM", response));
    }
}
