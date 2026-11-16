package es.timeforge;

import es.timeforge.model.Domain.*;
import es.timeforge.service.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoulettePolicyTest {
    private Progress progress(MediaType type,int duration,boolean quick){var w=new Work();w.type=type;w.minutesPerUnit=duration;w.totalUnits=10;w.quickSession=quick;var p=new Progress();p.work=w;p.lastInteraction=Instant.parse("2026-11-01T00:00:00Z");return p;}
    @Test void costsCannotBeChosenByClient(){assertEquals(80,RoulettePolicy.cost(15));assertEquals(150,RoulettePolicy.cost(30));assertEquals(280,RoulettePolicy.cost(60));assertThrows(BusinessException.class,()->RoulettePolicy.cost(45));}
    @Test void longEpisodesNeverFitShortBreaks(){var p=progress(MediaType.SERIES,45,false);assertFalse(RoulettePolicy.eligible(p,15));assertFalse(RoulettePolicy.eligible(p,30));assertTrue(RoulettePolicy.eligible(p,60));}
    @Test void narrativeGamesNeedAnHour(){var p=progress(MediaType.GAME,15,false);assertFalse(RoulettePolicy.eligible(p,30));assertTrue(RoulettePolicy.eligible(p,60));p.work.quickSession=true;assertTrue(RoulettePolicy.eligible(p,15));}
    @Test void pausedAndCompletedAreExcluded(){var p=progress(MediaType.MANGA,10,false);p.state=ProgressState.PAUSED;assertFalse(RoulettePolicy.eligible(p,15));p.state=ProgressState.COMPLETED;assertFalse(RoulettePolicy.eligible(p,15));}
    @Test void inactivityAndNearCompletionIncreaseWeight(){var now=Instant.parse("2026-11-16T00:00:00Z");var old=progress(MediaType.MANGA,10,false);var recent=progress(MediaType.MANGA,10,false);recent.lastInteraction=now;assertTrue(RoulettePolicy.weight(old,now)>RoulettePolicy.weight(recent,now));recent.currentUnits=9;assertTrue(RoulettePolicy.weight(recent,now)>1);}
    @Test void emptyCatalogFailsBeforeCharging(){assertThrows(BusinessException.class,()->RoulettePolicy.choose(List.of(),Instant.now(),new Random(1)));}
}
