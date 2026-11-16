package es.timeforge.service;

import es.timeforge.model.*;
import es.timeforge.repository.Store;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.service.BusinessException.require;

@Service @Transactional
public class SocialService {
    private final Store store;private final AccountService accounts;private final Clock clock;
    public SocialService(Store s,AccountService a,Clock c) {store=s;accounts=a;clock=c;}
    public Friendship request(long user,String username) {
        var target=store.account(username.trim().toLowerCase(Locale.ROOT));require(target!=null&&target.active&&!target.deleted,"Usuario no disponible");require(target.id!=user,"No puedes enviarte una solicitud");
        accounts.locked(Math.min(user,target.id));accounts.locked(Math.max(user,target.id));
        var f=store.friendship(user,target.id);require(f==null||f.state==FriendshipState.REJECTED,"Ya existe una amistad o solicitud entre ambos");
        if(f==null) {f=new Friendship();f.lowId=Math.min(user,target.id);f.highId=Math.max(user,target.id);store.save(f);}
        f.senderId=user;f.receiverId=target.id;f.createdAt=clock.instant();f.state=FriendshipState.PENDING;return f;
    }
    public void decide(long user,long id,boolean accept) {
        accounts.locked(user);var f=store.get(Friendship.class,id);require(f!=null&&f.receiverId==user&&f.state==FriendshipState.PENDING,"La solicitud no está disponible");f.state=accept?FriendshipState.ACCEPTED:FriendshipState.REJECTED;
    }
    public void remove(long user,long id) {accounts.locked(user);var f=store.get(Friendship.class,id);require(f!=null&&(f.lowId==user||f.highId==user),"Amistad no disponible");f.state=FriendshipState.REJECTED;}
    public List<Map<String,Object>> list(long user) {
        return store.friends(user).stream().filter(f->f.state!=FriendshipState.REJECTED).map(f->{Account other=store.get(Account.class,f.lowId==user?f.highId:f.lowId);return Map.<String,Object>of("id",f.id,"state",f.state,"incoming",f.receiverId==user,"profile",accounts.publicProfile(other),"available",other.active&&!other.deleted);}).toList();
    }
}
