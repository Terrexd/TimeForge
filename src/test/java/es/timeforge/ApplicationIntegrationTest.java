package es.timeforge;

import com.fasterxml.jackson.databind.*;
import es.timeforge.model.*;
import es.timeforge.repository.Store;
import es.timeforge.service.*;
import es.timeforge.controller.Inputs.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:timeforge_test;MODE=MySQL;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","timeforge.gemini-key=","timeforge.admin-user=","timeforge.admin-password="})
@AutoConfigureMockMvc
@Import(ApplicationIntegrationTest.TestTime.class)
class ApplicationIntegrationTest {
    static class MutableClock extends Clock {
        volatile Instant instant=Instant.parse("2026-11-16T09:00:00Z");
        @Override public Instant instant(){return instant;}
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId z){return Clock.fixed(instant,z);}
        void advance(long seconds){instant=instant.plusSeconds(seconds);}
    }
    @TestConfiguration static class TestTime {@Bean @Primary MutableClock testClock(){return new MutableClock();}}
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired AccountService accounts;@Autowired TaskService tasks;@Autowired CatalogService catalog;@Autowired MissionService missions;@Autowired RestService rests;@Autowired SocialService social;@Autowired CoopService coop;@Autowired Store store;@Autowired TransactionTemplate tx;@Autowired MutableClock clock;
    @BeforeEach void resetClock(){clock.instant=Instant.parse("2026-11-16T09:00:00Z");}
    private Account account(){String name="u"+UUID.randomUUID().toString().replace("-","");return accounts.register(new Register(name,name+"@example.com","Password-2026!"));}
    private Task task(long user,int minutes){return tasks.create(user,new TaskInput("Estudiar Java","Servicios y persistencia","ESTUDIO","ALTA",minutes));}
    private Progress work(long user,String title,int duration){return catalog.add(user,new WorkInput(title,MediaType.MANGA,"Una historia de aventuras","Aventura",20,duration,false));}
    private int balance(long user){return tx.execute(s->store.wallet(user).balance);}
    private void fund(long user){var t=task(user,120);tasks.action(user,t.id,"start");clock.advance(300);tasks.action(user,t.id,"complete");}
    private MockHttpSession login(Account a)throws Exception{return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).param("username",a.username).param("password","Password-2026!")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
    private JsonNode body(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString());}

    @Test void registrationHashesPasswordAndCreatesZeroWallet(){var a=account();assertNotEquals("Password-2026!",a.passwordHash);assertTrue(a.passwordHash.startsWith("$2"));assertEquals(0,balance(a.id));assertThrows(BusinessException.class,()->accounts.register(new Register(a.username,a.email,"Password-2026!")));}
    @Test void loginAndProfileDoNotLeakSecrets()throws Exception{var a=account();var session=login(a);var result=body(mvc.perform(get("/api/me").session(session)).andExpect(status().isOk()).andReturn());assertFalse(result.has("passwordHash"));assertEquals(0,result.path("balance").asInt());mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());}
    @Test void authenticationCsrfValidationAndAdminBoundary()throws Exception{mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());var a=account();var session=login(a);mvc.perform(post("/api/tasks").session(session).contentType("application/json").content("{}")).andExpect(status().isForbidden());mvc.perform(post("/api/tasks").session(session).with(csrf()).contentType("application/json").content("{\"title\":\"\",\"category\":\"ESTUDIO\",\"priority\":\"ALTA\",\"estimatedMinutes\":0}")).andExpect(status().isBadRequest());mvc.perform(get("/api/admin/ai").session(session)).andExpect(status().isForbidden());}
    @Test void timerPauseExcludesIdleTimeAndCompletionIsIdempotent(){var a=account();var t=task(a.id,10);tasks.action(a.id,t.id,"start");clock.advance(180);tasks.action(a.id,t.id,"pause");clock.advance(600);tasks.action(a.id,t.id,"start");clock.advance(120);var done=tasks.action(a.id,t.id,"complete");assertEquals(300,done.elapsedSeconds);assertEquals(20,done.coins);tasks.action(a.id,t.id,"complete");assertEquals(20,balance(a.id));assertEquals(1,tx.execute(s->store.movements(a.id).size()).intValue());}
    @Test void manualCompletionAndCancellationDoNotMintCoins(){var a=account();var manual=task(a.id,60);assertEquals(0,tasks.action(a.id,manual.id,"complete").coins);var t=task(a.id,60);tasks.action(a.id,t.id,"start");clock.advance(600);tasks.action(a.id,t.id,"cancel");assertEquals(0,tasks.action(a.id,t.id,"complete").coins);assertEquals(0,balance(a.id));}
    @Test void ownersCannotAccessEachOthersTasks()throws Exception{var a=account();var b=account();var t=task(a.id,20);var session=login(b);mvc.perform(post("/api/tasks/"+t.id+"/start").session(session).with(csrf())).andExpect(status().isNotFound());assertThrows(BusinessException.class,()->tasks.edit(b.id,t.id,new TaskInput("Otro","","ESTUDIO","ALTA",10)));}
    @Test void estimatesCannotChangeAfterTimerStarts(){var a=account();var t=task(a.id,20);tasks.action(a.id,t.id,"start");tasks.action(a.id,t.id,"pause");assertThrows(BusinessException.class,()->tasks.edit(a.id,t.id,new TaskInput("Trampa","","ESTUDIO","ALTA",720)));}
    @Test void activeTaskAndRestAreExclusive(){var a=account();fund(a.id);work(a.id,"Exclusividad"+a.id,10);var t=task(a.id,10);var second=task(a.id,10);tasks.action(a.id,t.id,"start");assertThrows(BusinessException.class,()->tasks.action(a.id,second.id,"start"));assertThrows(BusinessException.class,()->rests.spin(a.id,15,null));tasks.action(a.id,t.id,"pause");var r=rests.spin(a.id,15,null);assertThrows(BusinessException.class,()->tasks.action(a.id,t.id,"start"));rests.finish(a.id,r.id,true);assertEquals(160,balance(a.id));tasks.action(a.id,t.id,"start");}
    @Test void insufficientOrIncompatibleCatalogNeverCharges(){var a=account();work(a.id,"Larga"+a.id,45);assertThrows(BusinessException.class,()->rests.spin(a.id,15,null));assertEquals(0,balance(a.id));fund(a.id);assertThrows(BusinessException.class,()->rests.spin(a.id,15,null));assertEquals(240,balance(a.id));}
    @Test void dailyCapIsAppliedAcrossTasksAndResetsNextDay(){var a=account();for(int i=0;i<2;i++){var t=task(a.id,720);tasks.action(a.id,t.id,"start");clock.advance(300);tasks.action(a.id,t.id,"complete");}assertEquals(1500,balance(a.id));clock.advance(86400);fund(a.id);assertEquals(1740,balance(a.id));}
    @Test void progressBoundsDuplicatesAndAutomaticBadges(){var a=account();var p=work(a.id,"Libro"+a.id,10);assertThrows(BusinessException.class,()->work(a.id,"Libro"+a.id,10));assertThrows(BusinessException.class,()->catalog.update(a.id,p.id,new ProgressInput(ProgressState.IN_PROGRESS,21,0,0,"")));catalog.update(a.id,p.id,new ProgressInput(ProgressState.COMPLETED,20,2,0,""));assertTrue(tx.<Boolean>execute(s->store.badges(a.id).stream().anyMatch(b->b.code.equals("FINISHER"))));}
    @Test void missionClaimIsOnceOnlyAndExpiresAtMadridWeekBoundary(){var a=account();var p=work(a.id,"Misión"+a.id,10);var m=missions.generate(a.id).getFirst();catalog.update(a.id,p.id,new ProgressInput(ProgressState.IN_PROGRESS,3,0,0,""));missions.claim(a.id,m.id);assertEquals(40,balance(a.id));assertThrows(BusinessException.class,()->missions.claim(a.id,m.id));var b=account();var p2=work(b.id,"Caduca"+b.id,10);var m2=missions.generate(b.id).getFirst();catalog.update(b.id,p2.id,new ProgressInput(ProgressState.IN_PROGRESS,3,0,0,""));clock.instant=m2.expiresAt;assertThrows(BusinessException.class,()->missions.claim(b.id,m2.id));assertEquals(0,balance(b.id));}
    @Test void cooperativesRequireConsentAndChargeBothAtomically()throws Exception{var a=account();var b=account();fund(a.id);fund(b.id);String title="Compartido"+a.id;work(a.id,title,10);work(b.id,title,10);var f=social.request(a.id,b.username);social.decide(b.id,f.id,true);var i=coop.invite(a.id,b.id,15);assertEquals(240,balance(a.id));assertEquals(240,balance(b.id));assertThrows(BusinessException.class,()->coop.decide(a.id,i.id,true));coop.decide(b.id,i.id,true);assertEquals(160,balance(a.id));assertEquals(160,balance(b.id));var ar=rests.history(a.id).getFirst();var br=rests.history(b.id).getFirst();assertEquals(ar.work.id,br.work.id);assertThrows(BusinessException.class,()->coop.decide(b.id,i.id,true));var session=login(a);mvc.perform(post("/api/rests").session(session).with(csrf()).contentType("application/json").content("{\"minutes\":15,\"friendId\":"+b.id+"}")).andExpect(status().isBadRequest());}
    @Test void missingCommonCatalogRollsBackBothWallets(){var a=account();var b=account();fund(a.id);fund(b.id);work(a.id,"Solo A"+a.id,10);work(b.id,"Solo B"+b.id,10);var f=social.request(a.id,b.username);social.decide(b.id,f.id,true);var i=coop.invite(a.id,b.id,15);assertThrows(BusinessException.class,()->coop.decide(b.id,i.id,true));assertEquals(240,balance(a.id));assertEquals(240,balance(b.id));}
    @Test void concurrentCompletionCannotDoubleCredit()throws Exception{var a=account();var t=task(a.id,40);tasks.action(a.id,t.id,"start");clock.advance(300);try(var executor=Executors.newFixedThreadPool(2)){var gate=new CountDownLatch(1);var first=executor.submit(()->{gate.await();return tasks.action(a.id,t.id,"complete");});var second=executor.submit(()->{gate.await();return tasks.action(a.id,t.id,"complete");});gate.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);}assertEquals(80,balance(a.id));assertEquals(1,tx.execute(s->store.movements(a.id).size()).intValue());}
    @Test void concurrentSpinsCannotOverspend()throws Exception{var a=account();fund(a.id);work(a.id,"Concurrencia"+a.id,10);try(var executor=Executors.newFixedThreadPool(2)){var gate=new CountDownLatch(1);Callable<Boolean> spin=()->{gate.await();try{rests.spin(a.id,15,null);return true;}catch(BusinessException e){return false;}};var first=executor.submit(spin);var second=executor.submit(spin);gate.countDown();assertNotEquals(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS));}assertEquals(160,balance(a.id));assertEquals(1,rests.history(a.id).size());}
    @Test void suspendedAccountsLoseAccessImmediately()throws Exception{var admin=account();tx.executeWithoutResult(s->store.get(Account.class,admin.id).role="ADMIN");var a=account();var session=login(a);accounts.administer(admin.id,a.id,"SUSPEND");mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());accounts.administer(admin.id,a.id,"REACTIVATE");mvc.perform(get("/api/me").session(session)).andExpect(status().isOk());accounts.administer(admin.id,a.id,"DELETE");assertThrows(BusinessException.class,()->accounts.administer(admin.id,a.id,"REACTIVATE"));}
    @Test void oracleFallbackExistsImmediatelyAndRestExpiryReleasesWork(){var a=account();fund(a.id);work(a.id,"Oráculo"+a.id,10);var r=rests.spin(a.id,15,null);assertTrue(tx.<Boolean>execute(s->store.recommendation(r.id).text.contains("Oráculo")));clock.advance(900);var t=task(a.id,10);tasks.action(a.id,t.id,"start");assertTrue(rests.history(a.id).getFirst().completed);}
}
