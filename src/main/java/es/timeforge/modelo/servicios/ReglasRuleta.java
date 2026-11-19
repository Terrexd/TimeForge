package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import java.time.*;
import java.util.*;
import java.util.random.RandomGenerator;

public final class ReglasRuleta {
    private ReglasRuleta() {
    }

    public static int cost(int minutes) {
        return switch (minutes) {
        case 15 -> 80;
        case 30 -> 150;
        case 60 -> 280;
        default -> throw new ErrorNegocio("Elige una pausa de 15, 30 o 60 minutos");
        };
    }

    public static boolean eligible(Progreso p, int minutes) {
        return (p.state == EstadoProgreso.PENDING || p.state == EstadoProgreso.IN_PROGRESS)
                && p.work.minutesPerUnit <= minutes
                && (p.work.type != TipoMedio.GAME || minutes == 60 || p.work.quickSession);
    }

    public static double weight(Progreso p, Instant now) {
        long days = Math.max(0, Duration.between(p.lastInteraction, now).toDays());
        double completion = p.work.totalUnits > 0 ? Math.min(1, p.currentUnits / p.work.totalUnits) : 0;
        return 1 + Math.min(days, 60) / 7.0 + completion * 3;
    }

    public static Progreso choose(List<Progreso> options, Instant now, RandomGenerator random) {
        ErrorNegocio.require(!options.isEmpty(), "No hay obras compatibles con esta pausa en el catálogo");
        double target = random.nextDouble() * options.stream().mapToDouble(p -> weight(p, now)).sum();
        for (var p : options) {
            target -= weight(p, now);
            if (target < 0)
                return p;
        }
        return options.getLast();
    }
}
