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
        return addImported(user, input, source, externalId, cover, coverType, null);
    }

    public Progreso addImported(long user, DatosObra input, String source, String externalId, byte[] cover,
            String coverType, Double horasCampana) {
        var a = accounts.locked(user);
        String normalized = input.title().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        boolean externa = !"MANUAL".equals(source);
        Obra w = externa ? repositorio.buscarObraExterna(source, externalId) : null;
        if (w == null) {
            w = repositorio.buscarObra(normalized, input.type());
            if (externa && w != null && source.equals(w.source) && !externalId.equals(w.externalId)) {
                normalized += " [" + source.toLowerCase(Locale.ROOT) + " " + externalId + "]";
                w = null;
            }
        }
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
        } else if (!source.equals("MANUAL") && ("IGDB".equals(source) || !"IGDB".equals(w.source))) {
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
        if (horasCampana != null) w.horasCampanaImportadas = horasCampana;
        var existente = repositorio.catalogoPersonal(user).stream().filter(p -> p.work.id == workId).findFirst();
        if (externa && existente.isPresent()) return existente.get();
        require(existente.isEmpty(),
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
        boolean juego = p.work.type == TipoMedio.GAME;
        require(Double.isFinite(input.currentUnits()) && (juego || input.currentUnits() <= p.work.totalUnits),
                "El progreso actual no puede superar el total de la obra");
        require(juego || input.state() != EstadoProgreso.COMPLETED || input.currentUnits() == p.work.totalUnits,
                "Para completar la obra debes alcanzar su total de unidades");
        p.state = input.state();
        p.currentUnits = input.currentUnits();
        p.volume = input.volume();
        p.season = input.season();
        p.platform = input.platform() == null ? "" : input.platform();
        return registrarProgreso(a, p);
    }

    public Progreso ajustarCapitulo(long user, long id, int paso) {
        require(paso == 1 || paso == -1, "Solo puedes sumar o restar una unidad");
        var a = accounts.locked(user);
        var p = owned(user, id);
        require(p.work.type != TipoMedio.GAME && p.work.type != TipoMedio.MOVIE,
                "Esta obra no se registra por capítulos o episodios");
        double cantidad = Math.clamp(p.currentUnits + paso, 0, p.work.totalUnits);
        if (cantidad == p.currentUnits) return p;
        p.currentUnits = cantidad;
        if (cantidad == p.work.totalUnits) p.state = EstadoProgreso.COMPLETED;
        else if (cantidad == 0) p.state = EstadoProgreso.PENDING;
        else if (p.state != EstadoProgreso.PAUSED) p.state = EstadoProgreso.IN_PROGRESS;
        return registrarProgreso(a, p);
    }

    private Progreso registrarProgreso(Usuario a, Progreso p) {
        p.lastInteraction = clock.instant();
        missions.update(a.id, p);
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
