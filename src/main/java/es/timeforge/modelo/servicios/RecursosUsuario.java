package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import org.springframework.stereotype.Component;

/** Comprobaciones de pertenencia compartidas: nunca confían en un id enviado por el cliente. */
@Component
public class RecursosUsuario {
    private final Repositorio repositorio;

    public RecursosUsuario(Repositorio repositorio) {
        this.repositorio = repositorio;
    }

    public Tarea task(long user, long id) {
        var task = repositorio.buscarPorId(Tarea.class, id);
        if (task == null || task.owner.id != user)
            throw new ErrorNegocio(404, "La tarea ya no está disponible");
        return task;
    }

    public Progreso progress(long user, long id) {
        var progress = repositorio.buscarPorId(Progreso.class, id);
        if (progress == null || progress.owner.id != user)
            throw new ErrorNegocio(404, "La obra ya no está en tu catálogo");
        return progress;
    }

    public Descanso rest(long user, long id) {
        var rest = repositorio.buscarPorId(Descanso.class, id);
        ErrorNegocio.require(rest != null && rest.owner.id == user, "Descanso no disponible");
        return rest;
    }
}
