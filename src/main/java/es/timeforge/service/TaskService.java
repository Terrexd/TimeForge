package es.timeforge.service;

import es.timeforge.model.*;
import es.timeforge.repository.Store;
import es.timeforge.controller.Inputs.TaskInput;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.service.BusinessException.require;

@Service
@Transactional
public class TaskService {
    private final Store store;
    private final AccountService accounts;
    private final WalletService wallets;
    private final AchievementService achievements;
    private final OwnedResources owned;
    private final Clock clock;

    public TaskService(Store store, AccountService accounts, WalletService wallets,
                       AchievementService achievements, OwnedResources owned, Clock clock) {
        this.store=store;
        this.accounts=accounts;
        this.wallets=wallets;
        this.achievements=achievements;
        this.owned=owned;
        this.clock=clock;
    }

    public Task create(long user, TaskInput input) {
        var account=accounts.locked(user);
        var task=new Task();
        task.owner=account;
        task.createdAt=clock.instant();
        assign(task,input);
        return store.save(task);
    }

    public Task edit(long user, long id, TaskInput input) {
        accounts.locked(user);
        var task=owned.task(user,id);
        require(!task.timed && task.state==TaskState.PENDING,
                "Solo puedes editar tareas que todavía no has iniciado");
        assign(task,input);
        return task;
    }

    private void assign(Task task, TaskInput input) {
        require(!input.title().isBlank(),"El título es obligatorio");
        task.title=input.title().trim();
        task.description=input.description()==null?"":input.description();
        task.category=input.category();
        task.priority=input.priority();
        task.estimatedMinutes=input.estimatedMinutes();
    }

    public void delete(long user, long id) {
        accounts.locked(user);
        var task=owned.task(user,id);
        require(task.state==TaskState.PENDING && !task.timed,
                "Solo se pueden borrar tareas pendientes sin iniciar; el historial se conserva");
        store.remove(task);
    }

    public Task action(long user, long id, String action) {
        var account=accounts.locked(user);
        var task=owned.task(user,id);
        // Repetir una finalización nunca liquida monedas de nuevo.
        if(action.equals("complete") && task.state==TaskState.COMPLETED) return task;
        require(task.state!=TaskState.COMPLETED,"La tarea ya está completada");
        switch(action) {
            case "start" -> start(task,user);
            case "pause" -> pause(task);
            case "cancel" -> cancel(task);
            case "complete" -> complete(task,account);
            default -> throw new BusinessException("Acción de tarea desconocida");
        }
        return task;
    }

    private void start(Task task,long user) {
        require(task.state!=TaskState.RUNNING,"La tarea ya está en marcha");
        require(store.tasks(user).stream().noneMatch(t->t.state==TaskState.RUNNING),
                "Ya tienes una tarea en marcha. Pausa o finaliza la anterior");
        require(store.activeRests(user,clock.instant()).isEmpty(),
                "Finaliza o cancela el descanso antes de trabajar");
        task.startedAt=clock.instant();
        task.timed=true;
        task.state=TaskState.RUNNING;
    }

    private void pause(Task task) {
        require(task.state==TaskState.RUNNING,"La tarea no está en marcha");
        accumulate(task);
        task.state=TaskState.PAUSED;
    }

    private void cancel(Task task) {
        task.elapsedSeconds=0;
        task.startedAt=null;
        task.timed=false;
        task.state=TaskState.PENDING;
    }

    private void complete(Task task,Account account) {
        accumulate(task);
        task.state=TaskState.COMPLETED;
        task.completedAt=clock.instant();
        Instant dayStart=LocalDate.now(clock.withZone(AchievementService.ZONE))
                .atStartOfDay(AchievementService.ZONE).toInstant();
        int alreadyEarned=store.earnedSince(account.id,dayStart);
        task.coins=RewardPolicy.calculate(task.estimatedMinutes,task.elapsedSeconds,
                task.timed,alreadyEarned);
        wallets.move(account.id,task.coins,"TAREA");
        if(task.timed && task.elapsedSeconds>=300) {
            achievements.activity(account,Math.max(10,task.coins));
        }
    }

    private void accumulate(Task task) {
        if(task.startedAt==null) return;
        task.elapsedSeconds+=Math.max(0,Duration.between(task.startedAt,clock.instant()).toSeconds());
        task.startedAt=null;
    }

    public Map<String,Object> metrics(long user) {
        var completed=store.tasks(user).stream().filter(t->t.state==TaskState.COMPLETED).toList();
        return Map.of("completed",completed.size(),
                "focusSeconds",completed.stream().mapToLong(t->t.elapsedSeconds).sum(),
                "estimatedMinutes",completed.stream().mapToInt(t->t.estimatedMinutes).sum(),
                "coins",completed.stream().mapToInt(t->t.coins).sum(),
                "onTime",completed.stream().filter(t->t.timed && t.elapsedSeconds>=300
                        && t.elapsedSeconds<=t.estimatedMinutes*60L).count());
    }
}
