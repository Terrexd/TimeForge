package es.timeforge.service;

import es.timeforge.model.Domain.*;
import es.timeforge.repository.Store;
import org.springframework.stereotype.Component;

/** Comprobaciones de pertenencia compartidas: nunca confían en un id enviado por el cliente. */
@Component
public class OwnedResources {
    private final Store store;
    public OwnedResources(Store store) {this.store=store;}
    public Task task(long user,long id) {
        var task=store.get(Task.class,id);
        if(task==null||task.owner.id!=user)throw new BusinessException(404,"La tarea ya no está disponible");
        return task;
    }
    public Progress progress(long user,long id) {
        var progress=store.get(Progress.class,id);
        if(progress==null||progress.owner.id!=user)throw new BusinessException(404,"La obra ya no está en tu catálogo");
        return progress;
    }
    public Rest rest(long user,long id) {
        var rest=store.get(Rest.class,id);
        BusinessException.require(rest!=null&&rest.owner.id==user,"Descanso no disponible");
        return rest;
    }
}
