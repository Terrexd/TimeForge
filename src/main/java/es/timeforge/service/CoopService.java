package es.timeforge.service;

import es.timeforge.model.Domain.*;
import es.timeforge.repository.Store;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.service.BusinessException.require;

/** El destinatario debe consentir el gasto: ser amigo no autoriza a gastar su saldo. */
@Service @Transactional
public class CoopService {
    private final Store store;private final AccountService accounts;private final RestService rests;private final Clock clock;
    public CoopService(Store s,AccountService a,RestService r,Clock c) {store=s;accounts=a;rests=r;clock=c;}
    public CoopInvite invite(long user,long friend,int minutes) {
        require(user!=friend,"Selecciona a un amigo");var sender=accounts.locked(user);var f=store.friendship(user,friend);require(f!=null&&f.state==FriendshipState.ACCEPTED,"Necesitáis una amistad aceptada");int cost=RoulettePolicy.cost(minutes);
        require(store.invitations(friend,clock.instant()).stream().noneMatch(i->i.senderId==user),"Ya has enviado una invitación pendiente a este amigo");
        var i=new CoopInvite();i.senderId=user;i.receiverId=friend;i.senderName=sender.displayName;i.minutes=minutes;i.cost=cost;i.expiresAt=clock.instant().plusSeconds(600);return store.save(i);
    }
    public List<CoopInvite> list(long user) {return store.invitations(user,clock.instant());}
    public void decide(long user,long id,boolean accept) {
        var i=store.lockInvitation(id);require(i!=null&&i.receiverId==user,"Invitación no disponible");require(i.state.equals("PENDING")&&clock.instant().isBefore(i.expiresAt),"La invitación ha caducado o ya se ha resuelto");
        if(accept)rests.spin(i.senderId,i.minutes,i.receiverId);
        i.state=accept?"ACCEPTED":"REJECTED";
    }
}
