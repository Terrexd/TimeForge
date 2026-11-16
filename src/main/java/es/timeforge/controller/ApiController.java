package es.timeforge.controller;

import es.timeforge.model.Domain.*;
import es.timeforge.repository.Store;
import es.timeforge.service.*;
import es.timeforge.integration.*;
import es.timeforge.controller.Inputs.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class ApiController {
    private final Store store;private final AccountService accounts;private final TaskService tasks;private final CatalogService catalog;private final RestService rests;private final MissionService missions;private final SocialService social;private final OracleService oracle;private final ExternalCatalogService external;private final Clock clock;private final CoopService coop;
    public ApiController(Store s,AccountService a,TaskService t,CatalogService c,RestService r,MissionService m,SocialService f,OracleService o,ExternalCatalogService e,Clock clock,CoopService coop) {store=s;accounts=a;tasks=t;catalog=c;rests=r;missions=m;social=f;oracle=o;external=e;this.clock=clock;this.coop=coop;}
    private long user(Principal p) {if(p==null)throw new BusinessException(401,"Inicia sesiÃ³n");return accounts.current(p.getName()).id;}
    @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token) {return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> register(@Valid @RequestBody Register input) {var a=accounts.register(input);return Map.of("id",a.id,"username",a.username);}
    @GetMapping("/me") @Transactional public Map<String,Object> me(Principal p) {long id=user(p);var a=store.get(Account.class,id);var result=new HashMap<>(accounts.publicProfile(a));result.put("email",a.email);result.put("role",a.role);result.put("balance",store.wallet(id).balance);result.put("serverNow",clock.instant());return result;}
    @PutMapping("/me") public void profile(Principal p,@Valid @RequestBody Profile input) {accounts.profile(user(p),input);}
    @GetMapping("/tasks") @Transactional(readOnly=true) public List<Task> tasks(Principal p) {return store.tasks(user(p));}
    @GetMapping("/metrics") public Map<String,Object> metrics(Principal p) {return tasks.metrics(user(p));}
    @PostMapping("/tasks") public Task createTask(Principal p,@Valid @RequestBody TaskInput input) {return tasks.create(user(p),input);}
    @PutMapping("/tasks/{id}") public Task editTask(Principal p,@PathVariable long id,@Valid @RequestBody TaskInput input) {return tasks.edit(user(p),id,input);}
    @DeleteMapping("/tasks/{id}") public void deleteTask(Principal p,@PathVariable long id) {tasks.delete(user(p),id);}
    @PostMapping("/tasks/{id}/{action}") public Task taskAction(Principal p,@PathVariable long id,@PathVariable String action) {return tasks.action(user(p),id,action);}
    @GetMapping("/catalog") @Transactional(readOnly=true) public List<Progress> catalog(Principal p) {return store.backlog(user(p));}
    @PostMapping("/catalog") public Progress add(Principal p,@Valid @RequestBody WorkInput input) {return catalog.add(user(p),input);}
    @PutMapping("/catalog/{id}") public Progress progress(Principal p,@PathVariable long id,@Valid @RequestBody ProgressInput input) {return catalog.update(user(p),id,input);}
    @DeleteMapping("/catalog/{id}") public void remove(Principal p,@PathVariable long id) {catalog.delete(user(p),id);}
    @PostMapping("/catalog/{id}/classify") public OracleService.Answer classify(Principal p,@PathVariable long id) {return oracle.classify(user(p),id);}
    @GetMapping("/catalog/search") public List<Map<String,Object>> search(Principal p,@RequestParam String source,@RequestParam String q) {user(p);return external.search(source,q);}
    @PostMapping("/catalog/import") public Progress importWork(Principal p,@Valid @RequestBody ImportInput input) {return external.importWork(user(p),input.source(),input.externalId());}
    @GetMapping("/covers/{id}") @Transactional(readOnly=true) public ResponseEntity<byte[]> cover(Principal p,@PathVariable long id) {user(p);var w=store.get(Work.class,id);if(w==null||w.cover==null)return ResponseEntity.notFound().build();return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType(w.coverType)).cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePrivate()).body(w.cover);}
    @GetMapping("/wallet") @Transactional(readOnly=true) public Map<String,Object> wallet(Principal p) {long id=user(p);return Map.of("balance",store.wallet(id).balance,"movements",store.movements(id));}
    @GetMapping("/rests") public List<Rest> rests(Principal p) {return rests.history(user(p));}
    @PostMapping("/rests") public Rest spin(Principal p,@Valid @RequestBody RestInput input) {BusinessException.require(input.friendId()==null,"Las tiradas conjuntas requieren una invitación aceptada");return rests.spin(user(p),input.minutes(),null);}
    @GetMapping("/coop") public List<CoopInvite> invitations(Principal p) {return coop.list(user(p));}
    @PostMapping("/coop") public CoopInvite invite(Principal p,@Valid @RequestBody RestInput input) {BusinessException.require(input.friendId()!=null,"Selecciona un amigo");return coop.invite(user(p),input.friendId(),input.minutes());}
    @PostMapping("/coop/{id}") public void coopDecision(Principal p,@PathVariable long id,@RequestBody Decision input) {coop.decide(user(p),id,input.accept());}
    @PostMapping("/rests/{id}/{action}") public Rest finishRest(Principal p,@PathVariable long id,@PathVariable String action) {BusinessException.require(action.equals("cancel")||action.equals("finish"),"AcciÃ³n desconocida");return rests.finish(user(p),id,action.equals("cancel"));}
    @GetMapping("/rests/{id}/oracle") @Transactional(readOnly=true) public Recommendation oracle(Principal p,@PathVariable long id) {long user=user(p);var r=store.get(Rest.class,id);BusinessException.require(r!=null&&r.owner.id==user,"Descanso no disponible");return store.recommendation(id);}
    @GetMapping("/missions") public List<Mission> missions(Principal p) {return missions.generate(user(p));}
    @PostMapping("/missions/{id}/claim") public Mission claim(Principal p,@PathVariable long id) {return missions.claim(user(p),id);}
    @GetMapping("/users") public List<Map<String,Object>> users(Principal p,@RequestParam String q) {user(p);return accounts.search(q);}
    @GetMapping("/friends") public List<Map<String,Object>> friends(Principal p) {return social.list(user(p));}
    @PostMapping("/friends") public Friendship request(Principal p,@Valid @RequestBody FriendInput input) {return social.request(user(p),input.username());}
    @PostMapping("/friends/{id}") public void decide(Principal p,@PathVariable long id,@RequestBody Decision input) {social.decide(user(p),id,input.accept());}
    @DeleteMapping("/friends/{id}") public void removeFriend(Principal p,@PathVariable long id) {social.remove(user(p),id);}
    @GetMapping("/admin/users") @Transactional(readOnly=true) public List<Map<String,Object>> adminUsers(Principal p,@RequestParam(defaultValue="")String q) {user(p);return store.users(q).stream().map(a->Map.<String,Object>of("id",a.id,"username",a.username,"email",a.email,"role",a.role,"active",a.active)).toList();}
    @PostMapping("/admin/users/{id}") public void administer(Principal p,@PathVariable long id,@Valid @RequestBody AdminInput input) {accounts.administer(user(p),id,input.action());}
    @GetMapping("/admin/ai") @Transactional(readOnly=true) public Map<String,Object> ai(Principal p) {user(p);return Map.of("metrics",store.aiMetrics(),"calls",store.aiCalls());}
}

