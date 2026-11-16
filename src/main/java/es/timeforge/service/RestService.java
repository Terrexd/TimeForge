package es.timeforge.service;

import es.timeforge.model.*;
import es.timeforge.repository.Store;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.service.BusinessException.require;

@Service @Transactional
public class RestService {
    public record RestCreated(long restId) {}
    private final Store store;private final AccountService accounts;private final WalletService wallets;private final Clock clock;private final ApplicationEventPublisher events;private final OwnedResources owned;
    public RestService(Store s,AccountService a,WalletService w,Clock c,ApplicationEventPublisher e,OwnedResources owned) {store=s;accounts=a;wallets=w;clock=c;events=e;this.owned=owned;}
    public Rest spin(long user,int minutes,Long friend) {
        int cost=RoulettePolicy.cost(minutes);require(friend==null||friend!=user,"Selecciona a un amigo distinto de ti");
        // Orden global de bloqueos para evitar interbloqueos entre tiradas cruzadas.
        if(friend!=null) {accounts.locked(Math.min(user,friend));accounts.locked(Math.max(user,friend));var f=store.friendship(user,friend);require(f!=null&&f.state==FriendshipState.ACCEPTED,"La tirada cooperativa requiere una amistad aceptada");}
        else accounts.locked(user);
        available(user,cost);if(friend!=null) available(friend,cost);
        List<Progress> options=store.backlog(user).stream().filter(p->RoulettePolicy.eligible(p,minutes)).toList();
        if(friend!=null) {Set<Long> common=new HashSet<>();store.backlog(friend).stream().filter(p->RoulettePolicy.eligible(p,minutes)).forEach(p->common.add(p.work.id));options=options.stream().filter(p->common.contains(p.work.id)).toList();}
        var chosen=RoulettePolicy.choose(options,clock.instant(),ThreadLocalRandom.current());
        var rest=create(user,chosen.work,minutes,cost,friend);
        if(friend!=null) create(friend,chosen.work,minutes,cost,user);
        return rest;
    }
    private void available(long user,int cost) {require(store.wallet(user).balance>=cost,"Saldo insuficiente en una de las billeteras. Completa tareas para ganar monedas");require(store.tasks(user).stream().noneMatch(t->t.state==TaskState.RUNNING),"Pausa la tarea activa antes del descanso (ambos usuarios en modo cooperativo)");require(store.activeRests(user,clock.instant()).isEmpty(),"Ya existe un descanso activo para uno de los participantes");}
    private Rest create(long user,Work work,int minutes,int cost,Long partner) {
        wallets.move(user,-cost,"RULETA");var r=new Rest();r.owner=store.get(Account.class,user);r.work=work;r.minutes=minutes;r.cost=cost;r.startedAt=clock.instant();r.endsAt=r.startedAt.plusSeconds(minutes*60L);r.partnerId=partner;store.save(r);
        var rec=new Recommendation();rec.rest=r;rec.previousTask=store.lastTask(user);rec.text="¡Gran trabajo! Disfruta de «"+work.title+"» durante estos "+minutes+" minutos. Tu siguiente objetivo puede esperar.";rec.createdAt=clock.instant();store.save(rec);events.publishEvent(new RestCreated(r.id));return r;
    }
    public Rest finish(long user,long id,boolean cancel) {accounts.locked(user);var r=owned.rest(user,id);if(r.cancelled||r.completed)return r;require(cancel||!clock.instant().isBefore(r.endsAt),"El descanso todavía no ha terminado");r.cancelled=cancel;r.completed=!cancel;return r;}
    public List<Rest> history(long user) {accounts.locked(user);var rests=store.rests(user);for(var r:rests)if(!r.cancelled&&!clock.instant().isBefore(r.endsAt))r.completed=true;return rests;}
}
