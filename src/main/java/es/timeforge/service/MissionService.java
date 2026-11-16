package es.timeforge.service;

import es.timeforge.model.Domain.*;
import es.timeforge.repository.Store;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.service.BusinessException.require;

@Service @Transactional
public class MissionService {
    private final Store store;private final AccountService accounts;private final WalletService wallets;private final AchievementService achievements;private final Clock clock;
    public MissionService(Store s,AccountService a,WalletService w,AchievementService h,Clock c) {store=s;accounts=a;wallets=w;achievements=h;clock=c;}
    private LocalDate week() {return LocalDate.now(clock.withZone(AchievementService.ZONE)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));}
    public List<Mission> generate(long user) {
        var a=accounts.locked(user);var week=week();var existing=store.missions(user,week);
        if(existing.isEmpty()) {
            store.backlog(user).stream().filter(p->p.state!=ProgressState.COMPLETED && p.currentUnits<p.work.totalUnits).sorted(Comparator.comparing(p->p.lastInteraction)).limit(3).forEach(p->{
                var m=new Mission();m.owner=a;m.work=p.work;m.weekStart=week;m.baseline=p.currentUnits;m.target=Math.min(3,p.work.totalUnits-p.currentUnits);m.description="Retoma «"+p.work.title+"»: avanza "+m.target+" unidades";m.expiresAt=week.plusWeeks(1).atStartOfDay(AchievementService.ZONE).toInstant();store.save(m);
            });
        }
        return store.missions(user,week);
    }
    public void update(long user,Progress p) {
        for(var m:store.missions(user,week())) if(m.work.id.equals(p.work.id)&&clock.instant().isBefore(m.expiresAt)&&!m.claimed) m.progress=Math.max(m.progress,Math.max(0,p.currentUnits-m.baseline));
    }
    public Mission claim(long user,long id) {
        var a=accounts.locked(user);var m=store.get(Mission.class,id);require(m!=null&&m.owner.id==user,"Misión no disponible");require(clock.instant().isBefore(m.expiresAt),"La misión ha caducado");require(!m.claimed,"Ya has reclamado esta recompensa");require(m.progress>=m.target,"Todavía no has completado el objetivo");m.claimed=true;wallets.move(user,m.reward,"MISION");achievements.activity(a,40);return m;
    }
}
