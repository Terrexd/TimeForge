package es.timeforge.service;

import es.timeforge.model.Domain.*;
import es.timeforge.repository.Store;
import java.time.*;
import org.springframework.stereotype.Service;

@Service
public class AchievementService {
    public static final ZoneId ZONE=ZoneId.of("Europe/Madrid");
    private final Store store;private final Clock clock;
    public AchievementService(Store store,Clock clock) { this.store=store;this.clock=clock; }
    public void activity(Account a,int xp) {
        LocalDate today=LocalDate.now(clock.withZone(ZONE));
        if(!today.equals(a.lastActiveDay)) { a.streak=today.minusDays(1).equals(a.lastActiveDay)?a.streak+1:1;a.lastActiveDay=today; }
        a.xp+=xp;evaluate(a);
    }
    public void evaluate(Account a) {
        var tasks=store.tasks(a.id).stream().filter(t->t.state==TaskState.COMPLETED && t.timed && t.elapsedSeconds>=300).toList();
        if(!tasks.isEmpty()) award(a,"FIRST_FOCUS","Primer paso: primera sesión de concentración");
        if(tasks.stream().filter(t->t.category.equals("ESTUDIO")).count()>=5) award(a,"SCHOLAR","Aprendiz constante: 5 sesiones de estudio");
        if(a.streak>=3) award(a,"STREAK_3","En racha: 3 días de actividad");
        if(tasks.size()>=3 && tasks.stream().limit(3).allMatch(t->t.elapsedSeconds<=t.estimatedMinutes*60L)) award(a,"ON_TIME","A tiempo: 3 tareas puntuales consecutivas");
        if(store.backlog(a.id).stream().anyMatch(p->p.state==ProgressState.COMPLETED)) award(a,"FINISHER","Hasta el final: obra completa");
        if(store.backlog(a.id).stream().filter(p->p.state==ProgressState.COMPLETED).count()>=3) award(a,"COLLECTOR","Coleccionista: 3 obras completas");
    }
    private void award(Account a,String code,String title) {
        if(store.badges(a.id).stream().anyMatch(b->b.code.equals(code))) return;
        var b=new Badge();b.owner=a;b.code=code;b.title=title;b.createdAt=clock.instant();store.save(b);
    }
}
