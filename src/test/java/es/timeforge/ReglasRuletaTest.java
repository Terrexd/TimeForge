package es.timeforge;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.servicios.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReglasRuletaTest {
    private Progreso progress(TipoMedio type, int duration, boolean quick) {
        var w = new Obra();
        w.type = type;
        w.minutesPerUnit = duration;
        w.totalUnits = 10;
        w.quickSession = quick;
        var p = new Progreso();
        p.work = w;
        p.lastInteraction = Instant.parse("2026-11-01T00:00:00Z");
        return p;
    }

    @Test
    void costsCannotBeChosenByClient() {
        assertEquals(80, ReglasRuleta.cost(15));
        assertEquals(150, ReglasRuleta.cost(30));
        assertEquals(280, ReglasRuleta.cost(60));
        assertThrows(ErrorNegocio.class, () -> ReglasRuleta.cost(45));
    }

    @Test
    void longEpisodesNeverFitShortBreaks() {
        var p = progress(TipoMedio.SERIES, 45, false);
        assertFalse(ReglasRuleta.eligible(p, 15));
        assertFalse(ReglasRuleta.eligible(p, 30));
        assertTrue(ReglasRuleta.eligible(p, 60));
    }

    @Test
    void narrativeGamesNeedAnHour() {
        var p = progress(TipoMedio.GAME, 15, false);
        assertFalse(ReglasRuleta.eligible(p, 30));
        assertTrue(ReglasRuleta.eligible(p, 60));
        p.work.quickSession = true;
        assertTrue(ReglasRuleta.eligible(p, 15));
    }

    @Test
    void pausedAndCompletedAreExcluded() {
        var p = progress(TipoMedio.MANGA, 10, false);
        p.state = EstadoProgreso.PAUSED;
        assertFalse(ReglasRuleta.eligible(p, 15));
        p.state = EstadoProgreso.COMPLETED;
        assertFalse(ReglasRuleta.eligible(p, 15));
    }

    @Test
    void inactivityAndNearCompletionIncreaseWeight() {
        var now = Instant.parse("2026-11-16T00:00:00Z");
        var old = progress(TipoMedio.MANGA, 10, false);
        var recent = progress(TipoMedio.MANGA, 10, false);
        recent.lastInteraction = now;
        assertTrue(ReglasRuleta.weight(old, now) > ReglasRuleta.weight(recent, now));
        recent.currentUnits = 9;
        assertTrue(ReglasRuleta.weight(recent, now) > 1);
    }

    @Test
    void emptyCatalogFailsBeforeCharging() {
        assertThrows(ErrorNegocio.class, () -> ReglasRuleta.choose(List.of(), Instant.now(), new Random(1)));
    }
}
