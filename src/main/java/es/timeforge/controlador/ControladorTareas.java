package es.timeforge.controlador;

import es.timeforge.modelo.entidades.Tarea;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.servicios.ServicioTareas;
import es.timeforge.modelo.DatosEntrada.DatosTarea;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la gestión de tareas, control del temporizador de concentración y métricas de productividad.
 */
@RestController
@RequestMapping("/api")
public class ControladorTareas {

    private final Repositorio repositorio;
    private final ServicioUsuarios usuarios;
    private final ServicioTareas servicioTareas;

    public ControladorTareas(Repositorio repositorio, ServicioUsuarios usuarios,
            ServicioTareas servicioTareas) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.servicioTareas = servicioTareas;
    }

    private long idUsuario(Principal sesion) {
        if (sesion == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
        return usuarios.current(sesion.getName()).id;
    }

    /** Lista todas las tareas del usuario actual. */
    @GetMapping("/tasks")
    @Transactional(readOnly = true)
    public List<Tarea> listarTareas(Principal sesion) {
        return repositorio.tareas(idUsuario(sesion));
    }

    /** Obtiene métricas agregadas de productividad, horas y desviación temporal. */
    @GetMapping("/metrics")
    public Map<String, Object> calcularMetricas(Principal sesion) {
        return servicioTareas.calcularMetricas(idUsuario(sesion));
    }

    /** Crea una nueva tarea con tiempo estimado y prioridad. */
    @PostMapping("/tasks")
    public Tarea crearTarea(Principal sesion, @Valid @RequestBody DatosTarea datos) {
        return servicioTareas.crear(idUsuario(sesion), datos);
    }

    /** Modifica una tarea existente que aún no ha sido iniciada. */
    @PutMapping("/tasks/{id}")
    public Tarea editarTarea(Principal sesion, @PathVariable long id, @Valid @RequestBody DatosTarea datos) {
        return servicioTareas.editar(idUsuario(sesion), id, datos);
    }

    /** Elimina una tarea pendiente. */
    @DeleteMapping("/tasks/{id}")
    public void eliminarTarea(Principal sesion, @PathVariable long id) {
        servicioTareas.eliminar(idUsuario(sesion), id);
    }

    /** Acciones de ciclo de vida del cronómetro: start, pause, resume, cancel, complete. */
    @PostMapping("/tasks/{id}/{action}")
    public Tarea accionTarea(Principal sesion, @PathVariable long id, @PathVariable("action") String accion) {
        return servicioTareas.aplicarAccion(idUsuario(sesion), id, accion);
    }
}
