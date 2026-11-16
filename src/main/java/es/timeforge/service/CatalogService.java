package es.timeforge.service;

import es.timeforge.model.*;
import es.timeforge.repository.Store;
import es.timeforge.controller.Inputs.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.service.BusinessException.require;

@Service @Transactional
public class CatalogService {
    private final Store store;private final AccountService accounts;private final MissionService missions;private final AchievementService achievements;private final Clock clock;private final OwnedResources owned;
    public CatalogService(Store s,AccountService a,MissionService m,AchievementService h,Clock c,OwnedResources owned) {store=s;accounts=a;missions=m;achievements=h;clock=c;this.owned=owned;}
    public Progress add(long user,WorkInput input) {return addImported(user,input,"MANUAL","",null,null);}
    public Progress addImported(long user,WorkInput input,String source,String externalId,byte[] cover,String coverType) {
        var a=accounts.locked(user);String normalized=input.title().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
        Work w=store.work(normalized,input.type());
        if(w==null) {w=new Work();w.title=input.title().trim();w.canonicalTitle=normalized;w.type=input.type();w.synopsis=input.synopsis()==null?"":input.synopsis();w.genres=input.genres()==null?"":input.genres();w.totalUnits=input.totalUnits();w.minutesPerUnit=input.minutesPerUnit();w.quickSession=input.quickSession();w.source=source;w.externalId=externalId;w.cover=cover;w.coverType=coverType;store.save(w);}
        else if(!source.equals("MANUAL")) {w.synopsis=input.synopsis();w.genres=input.genres();w.source=source;w.externalId=externalId;if(cover!=null){w.cover=cover;w.coverType=coverType;}}
        long workId=w.id;require(store.backlog(user).stream().noneMatch(p->p.work.id==workId),"Esta obra ya existe en tu catálogo. Edita su estado o progreso desde tu lista");
        var p=new Progress();p.owner=a;p.work=w;p.lastInteraction=clock.instant();store.save(p);missions.generate(user);return p;
    }
    public Progress update(long user,long id,ProgressInput input) {
        var a=accounts.locked(user);var p=owned(user,id);require(Double.isFinite(input.currentUnits()) && input.currentUnits()<=p.work.totalUnits,"El progreso actual no puede superar el total de la obra");
        require(input.state()!=ProgressState.COMPLETED||input.currentUnits()==p.work.totalUnits,"Para completar la obra debes alcanzar su total de unidades");
        p.state=input.state();p.currentUnits=input.currentUnits();p.volume=input.volume();p.season=input.season();p.platform=input.platform()==null?"":input.platform();p.lastInteraction=clock.instant();missions.update(user,p);achievements.evaluate(a);return p;
    }
    public Progress owned(long user,long id) {return owned.progress(user,id);}
    public void delete(long user,long id) {accounts.locked(user);var p=owned(user,id);require(store.activeRests(user,clock.instant()).stream().noneMatch(r->r.work.id.equals(p.work.id)),"No puedes retirar la obra de un descanso activo");store.remove(p);}
}
