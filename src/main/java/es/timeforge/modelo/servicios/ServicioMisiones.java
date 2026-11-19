package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
@Transactional
public class ServicioMisiones {
    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final ServicioBilletera wallets;
    private final ServicioLogros achievements;
    private final Clock clock;

    public ServicioMisiones(Repositorio s, ServicioUsuarios a, ServicioBilletera w, ServicioLogros h,
            Clock c) {
        repositorio = s;
        accounts = a;
        wallets = w;
        achievements = h;
        clock = c;
    }

    private LocalDate week() {
        return LocalDate.now(clock.withZone(ServicioLogros.ZONE))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public List<Mision> generate(long user) {
        var a = accounts.locked(user);
        var week = week();
        var existing = repositorio.misiones(user, week);
        if (existing.isEmpty()) {
            repositorio.catalogoPersonal(user).stream()
                    .filter(p -> p.state != EstadoProgreso.COMPLETED
                            && (p.work.type == TipoMedio.GAME || p.currentUnits < p.work.totalUnits))
                    .sorted(Comparator.comparing(p -> p.lastInteraction)).limit(3).forEach(p -> {
                        var m = new Mision();
                        m.owner = a;
                        m.work = p.work;
                        m.weekStart = week;
                        m.baseline = p.currentUnits;
                        m.target = p.work.type == TipoMedio.GAME ? 3 : Math.min(3, p.work.totalUnits - p.currentUnits);
                        m.description = "Retoma «" + p.work.title + "»: avanza " + m.target + " unidades";
                        m.expiresAt = week.plusWeeks(1).atStartOfDay(ServicioLogros.ZONE).toInstant();
                        repositorio.guardar(m);
                    });
        }
        return repositorio.misiones(user, week);
    }

    public void update(long user, Progreso p) {
        for (var m : repositorio.misiones(user, week()))
            if (m.work.id.equals(p.work.id) && clock.instant().isBefore(m.expiresAt) && !m.claimed)
                m.progress = Math.max(m.progress, Math.max(0, p.currentUnits - m.baseline));
    }

    public Mision claim(long user, long id) {
        var a = accounts.locked(user);
        var m = repositorio.buscarPorId(Mision.class, id);
        require(m != null && m.owner.id == user, "Misión no disponible");
        require(clock.instant().isBefore(m.expiresAt), "La misión ha caducado");
        require(!m.claimed, "Ya has reclamado esta recompensa");
        require(m.progress >= m.target, "Todavía no has completado el objetivo");
        m.claimed = true;
        wallets.move(user, m.reward, "MISION");
        achievements.activity(a, 40);
        return m;
    }
}
