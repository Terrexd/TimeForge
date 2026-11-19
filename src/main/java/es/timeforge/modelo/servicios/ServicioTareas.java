package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.DatosEntrada.DatosTarea;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
@Transactional
public class ServicioTareas {
    private final Repositorio repositorio;
    private final ServicioUsuarios usuarios;
    private final ServicioBilletera billeteras;
    private final ServicioLogros logros;
    private final RecursosUsuario recursos;
    private final Clock reloj;

    public ServicioTareas(Repositorio repositorio, ServicioUsuarios usuarios, ServicioBilletera billeteras,
            ServicioLogros logros, RecursosUsuario recursos, Clock reloj) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.billeteras = billeteras;
        this.logros = logros;
        this.recursos = recursos;
        this.reloj = reloj;
    }

    public Tarea crear(long idUsuario, DatosTarea datos) {
        var usuario = usuarios.locked(idUsuario);
        var tarea = new Tarea();
        tarea.owner = usuario;
        tarea.createdAt = reloj.instant();
        asignarDatos(tarea, datos);
        return repositorio.guardar(tarea);
    }

    public Tarea editar(long idUsuario, long id, DatosTarea datos) {
        usuarios.locked(idUsuario);
        var tarea = recursos.task(idUsuario, id);
        require(!tarea.timed && tarea.state == EstadoTarea.PENDING,
                "Solo puedes editar tareas que todavía no has iniciado");
        asignarDatos(tarea, datos);
        return tarea;
    }

    private void asignarDatos(Tarea tarea, DatosTarea datos) {
        require(!datos.title().isBlank(), "El título es obligatorio");
        tarea.title = datos.title().trim();
        tarea.description = datos.description() == null ? "" : datos.description();
        tarea.category = datos.category();
        tarea.priority = datos.priority();
        tarea.estimatedMinutes = datos.estimatedMinutes();
    }

    public void eliminar(long idUsuario, long id) {
        usuarios.locked(idUsuario);
        var tarea = recursos.task(idUsuario, id);
        require(tarea.state == EstadoTarea.PENDING && !tarea.timed,
                "Solo se pueden borrar tareas pendientes sin iniciar; el historial se conserva");
        repositorio.eliminar(tarea);
    }

    public Tarea aplicarAccion(long idUsuario, long id, String accion) {
        var usuario = usuarios.locked(idUsuario);
        var tarea = recursos.task(idUsuario, id);
        // Repetir una finalización nunca liquida monedas de nuevo.
        if (accion.equals("complete") && tarea.state == EstadoTarea.COMPLETED)
            return tarea;
        require(tarea.state != EstadoTarea.COMPLETED, "La tarea ya está completada");
        switch (accion) {
        case "start" -> iniciar(tarea, idUsuario);
        case "pause" -> pausar(tarea);
        case "cancel" -> cancelar(tarea);
        case "complete" -> completar(tarea, usuario);
        default -> throw new ErrorNegocio("Acción de tarea desconocida");
        }
        return tarea;
    }

    private void iniciar(Tarea tarea, long idUsuario) {
        require(tarea.state != EstadoTarea.RUNNING, "La tarea ya está en marcha");
        require(repositorio.tareas(idUsuario).stream().noneMatch(t -> t.state == EstadoTarea.RUNNING),
                "Ya tienes una tarea en marcha. Pausa o finaliza la anterior");
        require(repositorio.descansosActivos(idUsuario, reloj.instant()).isEmpty(),
                "Finaliza o cancela el descanso antes de trabajar");
        tarea.startedAt = reloj.instant();
        tarea.timed = true;
        tarea.state = EstadoTarea.RUNNING;
    }

    private void pausar(Tarea tarea) {
        require(tarea.state == EstadoTarea.RUNNING, "La tarea no está en marcha");
        acumularTiempo(tarea);
        tarea.state = EstadoTarea.PAUSED;
    }

    private void cancelar(Tarea tarea) {
        tarea.elapsedSeconds = 0;
        tarea.startedAt = null;
        tarea.timed = false;
        tarea.state = EstadoTarea.PENDING;
    }

    private void completar(Tarea tarea, Usuario usuario) {
        acumularTiempo(tarea);
        tarea.state = EstadoTarea.COMPLETED;
        tarea.completedAt = reloj.instant();
        Instant inicioDelDia = LocalDate.now(reloj.withZone(ServicioLogros.ZONE))
                .atStartOfDay(ServicioLogros.ZONE).toInstant();
        int monedasGanadas = repositorio.monedasDesde(usuario.id, inicioDelDia);
        tarea.coins = ReglasRecompensa.calcular(tarea.estimatedMinutes, tarea.elapsedSeconds, tarea.timed,
                monedasGanadas);
        billeteras.move(usuario.id, tarea.coins, "TAREA");
        if (tarea.timed && tarea.elapsedSeconds >= 300) {
            logros.activity(usuario, Math.max(10, tarea.coins));
        }
    }

    private void acumularTiempo(Tarea tarea) {
        if (tarea.startedAt == null)
            return;
        tarea.elapsedSeconds += Math.max(0, Duration.between(tarea.startedAt, reloj.instant()).toSeconds());
        tarea.startedAt = null;
    }

    public Map<String, Object> calcularMetricas(long idUsuario) {
        var completadas = repositorio.tareas(idUsuario).stream().filter(t -> t.state == EstadoTarea.COMPLETED)
                .toList();
        return Map.of("completed", completadas.size(), "focusSeconds",
                completadas.stream().mapToLong(t -> t.elapsedSeconds).sum(), "estimatedMinutes",
                completadas.stream().mapToInt(t -> t.estimatedMinutes).sum(), "coins",
                completadas.stream().mapToInt(t -> t.coins).sum(), "onTime",
                completadas.stream().filter(t -> t.timed && t.elapsedSeconds >= 300
                        && t.elapsedSeconds <= t.estimatedMinutes * 60L).count());
    }
}
