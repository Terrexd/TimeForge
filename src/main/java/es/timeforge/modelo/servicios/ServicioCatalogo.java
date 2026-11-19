package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.DatosEntrada.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
@Transactional
public class ServicioCatalogo {
    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final ServicioMisiones missions;
    private final ServicioLogros achievements;
    private final Clock clock;
    private final RecursosUsuario owned;

    public ServicioCatalogo(Repositorio s, ServicioUsuarios a, ServicioMisiones m, ServicioLogros h, Clock c,
            RecursosUsuario owned) {
        repositorio = s;
        accounts = a;
        missions = m;
        achievements = h;
        clock = c;
        this.owned = owned;
    }

    public Progreso add(long user, DatosObra input) {
        return addImported(user, input, "MANUAL", "", null, null);
    }

    public Progreso addImported(long user, DatosObra input, String source, String externalId, byte[] cover,
            String coverType) {
        var a = accounts.locked(user);
        String normalized = input.title().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        Obra w = repositorio.buscarObra(normalized, input.type());
        if (w == null) {
            w = new Obra();
            w.title = input.title().trim();
            w.canonicalTitle = normalized;
            w.type = input.type();
            w.synopsis = input.synopsis() == null ? "" : input.synopsis();
            w.genres = input.genres() == null ? "" : input.genres();
            w.totalUnits = input.totalUnits();
            w.minutesPerUnit = input.minutesPerUnit();
            w.quickSession = input.quickSession();
            w.source = source;
            w.externalId = externalId;
            w.cover = cover;
            w.coverType = coverType;
            repositorio.guardar(w);
        } else if (!source.equals("MANUAL")) {
            w.synopsis = input.synopsis();
            w.genres = input.genres();
            w.source = source;
            w.externalId = externalId;
            if (cover != null) {
                w.cover = cover;
                w.coverType = coverType;
            }
        }
        long workId = w.id;
        require(repositorio.catalogoPersonal(user).stream().noneMatch(p -> p.work.id == workId),
                "Esta obra ya existe en tu catálogo. Edita su estado o progreso desde tu lista");
        var p = new Progreso();
        p.owner = a;
        p.work = w;
        p.lastInteraction = clock.instant();
        repositorio.guardar(p);
        missions.generate(user);
        return p;
    }

    public Progreso update(long user, long id, DatosProgreso input) {
        var a = accounts.locked(user);
        var p = owned(user, id);
        require(Double.isFinite(input.currentUnits()) && input.currentUnits() <= p.work.totalUnits,
                "El progreso actual no puede superar el total de la obra");
        require(input.state() != EstadoProgreso.COMPLETED || input.currentUnits() == p.work.totalUnits,
                "Para completar la obra debes alcanzar su total de unidades");
        p.state = input.state();
        p.currentUnits = input.currentUnits();
        p.volume = input.volume();
        p.season = input.season();
        p.platform = input.platform() == null ? "" : input.platform();
        p.lastInteraction = clock.instant();
        missions.update(user, p);
        achievements.evaluate(a);
        return p;
    }

    public Progreso owned(long user, long id) {
        return owned.progress(user, id);
    }

    public void delete(long user, long id) {
        accounts.locked(user);
        var p = owned(user, id);
        require(repositorio.descansosActivos(user, clock.instant()).stream().noneMatch(
                r -> r.work.id.equals(p.work.id)), "No puedes retirar la obra de un descanso activo");
        repositorio.eliminar(p);
    }
}
