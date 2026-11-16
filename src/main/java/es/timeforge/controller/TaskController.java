package es.timeforge.controller;

import es.timeforge.model.Task;
import es.timeforge.repository.Store;
import es.timeforge.service.AccountService;
import es.timeforge.service.BusinessException;
import es.timeforge.service.TaskService;
import es.timeforge.controller.Inputs.TaskInput;
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
public class TaskController {

    private final Store store;
    private final AccountService accounts;
    private final TaskService tasks;

    public TaskController(Store store, AccountService accounts, TaskService tasks) {
        this.store = store;
        this.accounts = accounts;
        this.tasks = tasks;
    }

    private long user(Principal principal) {
        if (principal == null) throw new BusinessException(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Lista todas las tareas del usuario actual. */
    @GetMapping("/tasks")
    @Transactional(readOnly = true)
    public List<Task> listTasks(Principal principal) {
        return store.tasks(user(principal));
    }

    /** Obtiene métricas agregadas de productividad, horas y desviación temporal. */
    @GetMapping("/metrics")
    public Map<String, Object> metrics(Principal principal) {
        return tasks.metrics(user(principal));
    }

    /** Crea una nueva tarea con tiempo estimado y prioridad. */
    @PostMapping("/tasks")
    public Task createTask(Principal principal, @Valid @RequestBody TaskInput input) {
        return tasks.create(user(principal), input);
    }

    /** Modifica una tarea existente que aún no ha sido iniciada. */
    @PutMapping("/tasks/{id}")
    public Task editTask(Principal principal, @PathVariable long id, @Valid @RequestBody TaskInput input) {
        return tasks.edit(user(principal), id, input);
    }

    /** Elimina una tarea pendiente. */
    @DeleteMapping("/tasks/{id}")
    public void deleteTask(Principal principal, @PathVariable long id) {
        tasks.delete(user(principal), id);
    }

    /** Acciones de ciclo de vida del cronómetro: start, pause, resume, cancel, complete. */
    @PostMapping("/tasks/{id}/{action}")
    public Task taskAction(Principal principal, @PathVariable long id, @PathVariable String action) {
        return tasks.action(user(principal), id, action);
    }
}
