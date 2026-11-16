package es.timeforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.timeforge.integration.CatalogMapper;
import es.timeforge.model.MediaType;
import es.timeforge.service.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CatalogMapperTest {
    private final ObjectMapper json=new ObjectMapper();
    @Test void readsHoursAndMinutes(){assertEquals(125,CatalogMapper.episodeMinutes("2 hr 5 min"));assertEquals(24,CatalogMapper.episodeMinutes("24 min per ep"));assertEquals(24,CatalogMapper.episodeMinutes("Unknown"));}
    @Test void importsAnimeMetadata()throws Exception{var work=CatalogMapper.map("JIKAN_ANIME",json.readTree("{\"data\":{\"title\":\"Serie\",\"episodes\":12,\"duration\":\"24 min per ep\",\"genres\":[{\"name\":\"Adventure\"}]}}"));assertEquals(MediaType.ANIME,work.input().type());assertEquals(12,work.input().totalUnits());assertEquals(24,work.input().minutesPerUnit());assertEquals("Adventure",work.input().genres());}
    @Test void unknownTotalsUseDocumentedMinimum()throws Exception{var work=CatalogMapper.map("JIKAN_MANGA",json.readTree("{\"data\":{\"title\":\"En publicación\",\"chapters\":null}}"));assertEquals(1,work.input().totalUnits());}
    @Test void invalidRemoteResponseDoesNotCreateBlankWork()throws Exception{var response=json.readTree("{}");assertThrows(BusinessException.class,()->CatalogMapper.map("RAWG",response));}
}
