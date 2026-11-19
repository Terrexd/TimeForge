package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import java.time.*;
import org.springframework.stereotype.Service;

@Service
public class ServicioLogros {
    public static final ZoneId ZONE = ZoneId.of("Europe/Madrid");
    private final Repositorio repositorio;
    private final Clock clock;

    public ServicioLogros(Repositorio repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    public void activity(Usuario a, int xp) {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        if (!today.equals(a.lastActiveDay)) {
            a.streak = today.minusDays(1).equals(a.lastActiveDay) ? a.streak + 1 : 1;
            a.lastActiveDay = today;
        }
        a.xp += xp;
        evaluate(a);
    }

    public void evaluate(Usuario a) {
        var tasks = repositorio.tareas(a.id).stream()
                .filter(t -> t.state == EstadoTarea.COMPLETED && t.timed && t.elapsedSeconds >= 300).toList();
        if (!tasks.isEmpty())
            award(a, "FIRST_FOCUS", "Primer paso: primera sesión de concentración");
        if (tasks.stream().filter(t -> t.category.equals("ESTUDIO")).count() >= 5)
            award(a, "SCHOLAR", "Aprendiz constante: 5 sesiones de estudio");
        if (a.streak >= 3)
            award(a, "STREAK_3", "En racha: 3 días de actividad");
        if (tasks.size() >= 3
                && tasks.stream().limit(3).allMatch(t -> t.elapsedSeconds <= t.estimatedMinutes * 60L))
            award(a, "ON_TIME", "A tiempo: 3 tareas puntuales consecutivas");
        if (repositorio.catalogoPersonal(a.id).stream().anyMatch(p -> p.state == EstadoProgreso.COMPLETED))
            award(a, "FINISHER", "Hasta el final: obra completa");
        if (repositorio.catalogoPersonal(a.id).stream().filter(p -> p.state == EstadoProgreso.COMPLETED)
                .count() >= 3)
            award(a, "COLLECTOR", "Coleccionista: 3 obras completas");
    }

    private void award(Usuario a, String code, String title) {
        if (repositorio.insignias(a.id).stream().anyMatch(b -> b.code.equals(code)))
            return;
        var b = new Insignia();
        b.owner = a;
        b.code = code;
        b.title = title;
        b.createdAt = clock.instant();
        repositorio.guardar(b);
    }
}
