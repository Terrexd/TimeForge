package es.timeforge.service;

import es.timeforge.model.*;
import es.timeforge.repository.Store;
import es.timeforge.controller.Inputs.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.service.BusinessException.require;

@Service @Transactional
public class AccountService {
    private final Store store; private final PasswordEncoder passwords; private final Clock clock;
    public AccountService(Store store,PasswordEncoder passwords,Clock clock) { this.store=store;this.passwords=passwords;this.clock=clock; }
    public Account register(Register input) {
        String name=input.username().trim().toLowerCase(Locale.ROOT),email=input.email().trim().toLowerCase(Locale.ROOT);
        require(store.account(name)==null && !store.emailExists(email),"El correo electrónico o nombre de usuario ya está en uso");
        require(input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72,"La contraseña no puede superar 72 bytes UTF-8");
        var a=new Account();a.username=name;a.email=email;a.passwordHash=passwords.encode(input.password());a.displayName=input.username();a.createdAt=clock.instant();store.save(a);
        var w=new Wallet();w.owner=a;w.updatedAt=clock.instant();store.save(w);return a;
    }
    public Account current(String username) {
        Account a=store.account(username);
        if(a==null || !a.active || a.deleted) throw new BusinessException(401,"La sesión no está activa. Inicia sesión de nuevo");
        return a;
    }
    public Account locked(long id) {
        Account a=store.lock(id);
        if(a==null || !a.active || a.deleted) throw new BusinessException(401,"Cuenta no disponible");
        return a;
    }
    public Map<String,Object> publicProfile(Account a) {
        return Map.of("id",a.id,"username",a.username,"displayName",a.displayName,"avatar",a.avatar,"bio",a.bio,"level",1+a.xp/200,"xp",a.xp,"streak",a.streak,"badges",store.badges(a.id));
    }
    public void profile(long user,Profile p) { var a=locked(user);a.displayName=p.displayName().trim();a.avatar=p.avatar();a.bio=p.bio()==null?"":p.bio(); }
    public List<Map<String,Object>> search(String q) { require(q.length()>=2 && q.length()<=80,"Escribe entre 2 y 80 caracteres");return store.users(q.toLowerCase(Locale.ROOT)).stream().filter(a->a.active).map(this::publicProfile).toList(); }
    public void administer(long admin,long target,String action) {
        var operator=locked(admin);if(!operator.role.equals("ADMIN")) throw new BusinessException(403,"Acceso reservado a administradores");
        require(admin!=target,"No puedes modificar tu propia cuenta desde administración");
        var a=store.lock(target);require(a!=null,"El usuario no existe");require(!a.role.equals("ADMIN"),"No se modifican otros administradores desde esta pantalla");
        switch(action) { case "SUSPEND"->a.active=false;case "REACTIVATE"->{require(!a.deleted,"Una cuenta dada de baja no puede reactivarse");a.active=true;}case "DELETE"->{a.active=false;a.deleted=true;a.email="deleted-"+a.id+"@invalid.local";a.displayName="Cuenta eliminada";a.bio="";a.avatar="×";a.passwordHash=passwords.encode(UUID.randomUUID().toString());}default->throw new BusinessException("Operación desconocida"); }
        // La suspensión congela trabajo y cancela ocio, sin devolver monedas.
        if(!a.active) {
            for(var t:store.tasks(target)) if(t.state==TaskState.RUNNING) { t.elapsedSeconds+=Math.max(0,Duration.between(t.startedAt,clock.instant()).toSeconds());t.startedAt=null;t.state=TaskState.PAUSED; }
            for(var r:store.activeRests(target,clock.instant())) r.cancelled=true;
        }
    }
}
