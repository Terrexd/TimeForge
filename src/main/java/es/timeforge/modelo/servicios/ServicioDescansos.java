package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
@Transactional
public class ServicioDescansos {
    public record RestCreated(long restId) {
    }

    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final ServicioBilletera wallets;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final RecursosUsuario owned;

    public ServicioDescansos(Repositorio s, ServicioUsuarios a, ServicioBilletera w, Clock c,
            ApplicationEventPublisher e, RecursosUsuario owned) {
        repositorio = s;
        accounts = a;
        wallets = w;
        clock = c;
        events = e;
        this.owned = owned;
    }

    public Descanso spin(long user, int minutes, Long friend) {
        int cost = ReglasRuleta.cost(minutes);
        require(friend == null || friend != user, "Selecciona a un amigo distinto de ti");
        // Orden global de bloqueos para evitar interbloqueos entre tiradas cruzadas.
        if (friend != null) {
            accounts.locked(Math.min(user, friend));
            accounts.locked(Math.max(user, friend));
            var f = repositorio.amistad(user, friend);
            require(f != null && f.state == EstadoAmistad.ACCEPTED,
                    "La tirada cooperativa requiere una amistad aceptada");
        } else
            accounts.locked(user);
        available(user, cost);
        if (friend != null)
            available(friend, cost);
        List<Progreso> options = repositorio.catalogoPersonal(user).stream()
                .filter(p -> ReglasRuleta.eligible(p, minutes)).toList();
        if (friend != null) {
            Set<Long> common = new HashSet<>();
            repositorio.catalogoPersonal(friend).stream().filter(p -> ReglasRuleta.eligible(p, minutes))
                    .forEach(p -> common.add(p.work.id));
            options = options.stream().filter(p -> common.contains(p.work.id)).toList();
        }
        var chosen = ReglasRuleta.choose(options, clock.instant(), ThreadLocalRandom.current());
        var rest = create(user, chosen.work, minutes, cost, friend);
        if (friend != null)
            create(friend, chosen.work, minutes, cost, user);
        return rest;
    }

    private void available(long user, int cost) {
        require(repositorio.billetera(user).balance >= cost,
                "Saldo insuficiente en una de las billeteras. Completa tareas para ganar monedas");
        require(repositorio.tareas(user).stream().noneMatch(t -> t.state == EstadoTarea.RUNNING),
                "Pausa la tarea activa antes del descanso (ambos usuarios en modo cooperativo)");
        require(repositorio.descansosActivos(user, clock.instant()).isEmpty(),
                "Ya existe un descanso activo para uno de los participantes");
    }

    private Descanso create(long user, Obra work, int minutes, int cost, Long partner) {
        wallets.move(user, -cost, "RULETA");
        var r = new Descanso();
        r.owner = repositorio.buscarPorId(Usuario.class, user);
        r.work = work;
        r.minutes = minutes;
        r.cost = cost;
        r.startedAt = clock.instant();
        r.endsAt = r.startedAt.plusSeconds(minutes * 60L);
        r.remainingSeconds = minutes * 60;
        r.paused = false;
        r.partnerId = partner;
        repositorio.guardar(r);
        var rec = new Recomendacion();
        rec.rest = r;
        rec.previousTask = repositorio.ultimaTarea(user);
        rec.text = "¡Gran trabajo! Disfruta de «" + work.title + "» durante estos " + minutes
                + " minutos. Tu siguiente objetivo puede esperar.";
        rec.createdAt = clock.instant();
        repositorio.guardar(rec);
        events.publishEvent(new RestCreated(r.id));
        return r;
    }

    public Descanso pause(long user, long id) {
        accounts.locked(user);
        var r = owned.rest(user, id);
        require(!r.cancelled && !r.completed, "El descanso ya ha finalizado");
        require(!r.paused, "El descanso ya está pausado");
        int left = (int) Math.max(0, Duration.between(clock.instant(), r.endsAt).getSeconds());
        require(left > 0, "El descanso ya ha finalizado");
        r.paused = true;
        r.remainingSeconds = left;
        return r;
    }

    public Descanso resume(long user, long id) {
        accounts.locked(user);
        var r = owned.rest(user, id);
        require(!r.cancelled && !r.completed, "El descanso ya ha finalizado");
        require(r.paused, "El descanso no está pausado");
        require(repositorio.tareas(user).stream().noneMatch(t -> t.state == EstadoTarea.RUNNING),
                "Pausa la tarea activa antes de reanudar el descanso");
        r.paused = false;
        r.startedAt = clock.instant();
        r.endsAt = clock.instant().plusSeconds(Math.max(1, r.remainingSeconds));
        return r;
    }

    public Descanso finish(long user, long id, boolean cancel) {
        accounts.locked(user);
        var r = owned.rest(user, id);
        if (r.cancelled || r.completed)
            return r;
        require(cancel || (!r.paused && !clock.instant().isBefore(r.endsAt)), "El descanso todavía no ha terminado");
        r.cancelled = cancel;
        r.completed = !cancel;
        r.paused = false;
        return r;
    }

    public List<Descanso> history(long user) {
        accounts.locked(user);
        var rests = repositorio.descansos(user);
        for (var r : rests)
            if (!r.cancelled && !r.completed && !r.paused && r.endsAt != null && !clock.instant().isBefore(r.endsAt))
                r.completed = true;
        return rests;
    }
}
