package es.timeforge;

import com.fasterxml.jackson.databind.*;
import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.integraciones.ClienteJson;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.servicios.*;
import es.timeforge.modelo.DatosEntrada.*;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:timeforge_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "timeforge.gemini-key=", "timeforge.admin-user=",
        "timeforge.admin-password=",
        "IGDB_CLIENT_ID=cliente-prueba", "IGDB_CLIENT_SECRET=secreto-prueba" })
@AutoConfigureMockMvc
@Import(AplicacionIntegracionTest.TestTime.class)
class AplicacionIntegracionTest {
    static class MutableClock extends Clock {
        volatile Instant instant = Instant.parse("2026-11-16T09:00:00Z");

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId z) {
            return Clock.fixed(instant, z);
        }

        void advance(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }

    @TestConfiguration
    static class TestTime {
        @Bean
        @Primary
        ClienteJson catalogoSimulado(ObjectMapper json) {
            return new ClienteJson(json) {
                @Override
                public JsonNode tokenTwitch(String id, String secreto) throws Exception {
                    assertEquals("cliente-prueba", id);
                    assertEquals("secreto-prueba", secreto);
                    return json.readTree("{\"access_token\":\"token-prueba\",\"expires_in\":3600}");
                }

                @Override
                public JsonNode consultaIgdb(String recurso, String id, String token, String consulta) throws Exception {
                    assertEquals("token-prueba", token);
                    if (recurso.equals("game_time_to_beats")) {
                        assertTrue(consulta.contains("game_id = (101"));
                        return json.readTree("[{\"game_id\":101,\"hastily\":45000}]");
                    }
                    assertEquals("games", recurso);
                    String juego = "{\"id\":101,\"name\":\"Juego sin coincidencia local\",\"cover\":{\"image_id\":\"portada101\"}}";
                    if (consulta.contains("where id = 101;")) return json.readTree("[" + juego + "]");
                    if (consulta.contains("Cyberpunk")) throw new java.io.IOException("IGDB fuera de servicio");
                    assertTrue(consulta.contains("search \"Juego\";"));
                    return json.readTree("[" + juego + ",{\"id\":102,\"name\":\"Juego sin campaña\"}]");
                }

                @Override
                public JsonNode get(String url) throws Exception {
                    return switch (url) {
                    case "https://cinemeta-catalogs.strem.io/top/catalog/series/top.json" ->
                        json.readTree("""
                            {"metas":[{"id":"tt0012345","name":"Serie de ejemplo","year":"2024","runtime":"42 min"}]}
                            """);
                    case "https://v3-cinemeta.strem.io/meta/series/tt0012345.json" ->
                        json.readTree("""
                            {"meta":{"id":"tt0012345","name":"Serie de ejemplo","runtime":"42 min",
                                     "videos":[{"season":0,"episode":1},{"season":1,"episode":1},{"season":1,"episode":2}]}}
                            """);
                    case "https://store.steampowered.com/api/storesearch/?term=Cyberpunk+2077&l=english&cc=US" ->
                        json.readTree("""
                            {"items":[{"id":1091500,"name":"Cyberpunk 2077"}]}
                            """);
                    case "https://store.steampowered.com/api/storesearch/?term=Assassin%27s+Creed+Unity&l=english&cc=US" ->
                        json.readTree("""
                            {"items":[{"id":324331,"name":"Assassin’s Creed Unity Revolutionary Armaments Pack"},
                                      {"id":289650,"name":"Assassin's Creed® Unity"}]}
                            """);
                    case "https://store.steampowered.com/api/appdetails?appids=1091500&l=english&cc=US" ->
                        json.readTree("""
                            {"1091500":{"success":true,"data":{"name":"Cyberpunk 2077","genres":[{"description":"RPG"}]}}}
                            """);
                    default -> throw new AssertionError("Petición inesperada al catálogo de prueba");
                    };
                }

                @Override
                public byte[] cover(String url) {
                    assertTrue(url.equals("https://cdn.akamai.steamstatic.com/steam/apps/1091500/header.jpg")
                            || url.equals("https://cdn.akamai.steamstatic.com/steam/apps/289650/header.jpg")
                            || url.equals("https://images.igdb.com/igdb/image/upload/t_cover_big/portada101.jpg"));
                    return new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff};
                }
            };
        }

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    ServicioUsuarios accounts;
    @Autowired
    ServicioTareas tasks;
    @Autowired
    ServicioCatalogo catalog;
    @Autowired
    ServicioMisiones missions;
    @Autowired
    ServicioDescansos rests;
    @Autowired
    ServicioSocial social;
    @Autowired
    ServicioCooperativo coop;
    @Autowired
    Repositorio repositorio;
    @Autowired
    TransactionTemplate tx;
    @Autowired
    MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.instant = Instant.parse("2026-11-16T09:00:00Z");
    }

    private Usuario account() {
        String name = "u" + UUID.randomUUID().toString().replace("-", "");
        return accounts.register(new Registro(name, name + "@example.com", "Password-2026!"));
    }

    private Tarea task(long user, int minutes) {
        return tasks.crearTarea(user,
                new DatosTarea("Estudiar Java", "Servicios y persistencia", "ESTUDIO", "ALTA", minutes));
    }

    private Progreso work(long user, String title, int duration) {
        return catalog.agregarObra(user, new DatosObra(title, TipoMedio.MANGA, "Una historia de aventuras",
                "Aventura", 20, duration, false));
    }

    private int balance(long user) {
        return tx.execute(s -> repositorio.billetera(user).balance);
    }

    private void fund(long user) {
        var t = task(user, 120);
        tasks.aplicarAccion(user, t.id, "start");
        clock.advance(300);
        tasks.aplicarAccion(user, t.id, "complete");
    }

    private MockHttpSession login(Usuario a) throws Exception {
        return (MockHttpSession) mvc
                .perform(post("/api/auth/login").with(csrf()).param("username", a.username).param("password",
                        "Password-2026!"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void pasosEnLaTarjetaRespetanLimitesYConservanLosDetalles() throws Exception {
        var usuario = account();
        var sesion = login(usuario);
        var p = catalog.agregarObra(usuario.id, new DatosObra("Cowboy Bebop", TipoMedio.ANIME, "", "", 26, 24, false));
        catalog.actualizarProgreso(usuario.id, p.id, new DatosProgreso(EstadoProgreso.IN_PROGRESS, 25, 0, 1, "TV"));
        String ruta = "/api/catalog/" + p.id + "/paso";
        for (int i = 0; i < 2; i++) {
            mvc.perform(post(ruta).session(sesion).with(csrf()).param("paso", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.currentUnits").value(26))
                    .andExpect(jsonPath("$.state").value("COMPLETED"));
        }
        mvc.perform(post(ruta).session(sesion).with(csrf()).param("paso", "-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentUnits").value(25))
                .andExpect(jsonPath("$.state").value("IN_PROGRESS")).andExpect(jsonPath("$.season").value(1))
                .andExpect(jsonPath("$.platform").value("TV"));
    }

    @Test
    void explorarSeriesSinTituloYAnadirConDuracionYEpisodios() throws Exception {
        var sesion = login(account());
        mvc.perform(get("/api/catalog/search").session(sesion).param("source", "CINE_SERIES"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(12345))
                .andExpect(jsonPath("$[0].minutos").value(42));
        mvc.perform(post("/api/catalog/import").session(sesion).with(csrf()).contentType("application/json")
                .content("{\"source\":\"CINE_SERIES\",\"externalId\":12345}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.work.type").value("SERIES"))
                .andExpect(jsonPath("$.work.minutesPerUnit").value(42)).andExpect(jsonPath("$.work.totalUnits").value(2));
    }

    @Test
    void buscarVideojuegoEImportarloCuandoFallaIgdb() throws Exception {
        var sesion = login(account());
        var resultado = mvc.perform(get("/api/catalog/search").session(sesion)
                .param("source", "VIDEOJUEGOS").param("q", "Cyberpunk 2077"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Cyberpunk 2077"))
                .andExpect(jsonPath("$[0].imagen").value("https://cdn.akamai.steamstatic.com/steam/apps/1091500/header.jpg"))
                .andExpect(jsonPath("$[0].horasHistoriaPrincipal").value(26.0))
                .andReturn();
        var juego = body(resultado).get(0);
        String seleccion = json.writeValueAsString(Map.of("source", juego.get("source").asText(),
                "externalId", juego.get("id").asLong()));
        mvc.perform(post("/api/catalog/import").session(sesion).with(csrf())
                .contentType("application/json").content(seleccion)).andExpect(status().isOk());
        mvc.perform(get("/api/catalog").session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].work.title").value("Cyberpunk 2077"))
                .andExpect(jsonPath("$[0].work.source").value("STEAM"))
                .andExpect(jsonPath("$[0].work.type").value("GAME"))
                .andExpect(jsonPath("$[0].work.horasHistoriaPrincipal").value(26.0));
    }

    @Test
    void igdbAsociaHorasAlIdYEnriqueceUnaFichaGuardadaSinPerderProgreso() throws Exception {
        var usuario = account();
        var sesion = login(usuario);
        var progreso = catalog.agregarObra(usuario.id, new DatosObra("Juego sin coincidencia local", TipoMedio.GAME,
                "", "", 0, 60, false));
        catalog.actualizarProgreso(usuario.id, progreso.id, new DatosProgreso(EstadoProgreso.IN_PROGRESS, 4, 0, 0, ""));
        mvc.perform(get("/api/catalog/search").session(sesion).param("source", "VIDEOJUEGOS").param("q", "Juego"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].horasHistoriaPrincipal").value(12.5))
                .andExpect(jsonPath("$[1].horasHistoriaPrincipal").doesNotExist());
        mvc.perform(post("/api/catalog/import").session(sesion).with(csrf())
                .contentType("application/json").content("{\"source\":\"IGDB\",\"externalId\":101}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(progreso.id));
        mvc.perform(get("/api/catalog").session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].currentUnits").value(4))
                .andExpect(jsonPath("$[0].work.horasHistoriaPrincipal").value(12.5))
                .andExpect(jsonPath("$[0].work.source").value("IGDB"));
        mvc.perform(get("/api/covers/" + progreso.work.id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void unityImportadoMuestraDuracionYGuardaLaPortadaAlAbrirSuTarjeta() throws Exception {
        var usuario = account();
        var sesion = login(usuario);
        var anterior = catalog.guardarObraEnBiblioteca(usuario.id, new DatosObra("Assassin's Creed Unity", TipoMedio.GAME,
                "", "", 0, 60, false), "RAWG", "1970", null, null);
        long id = anterior.work.id;
        mvc.perform(get("/api/catalog").session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].work.horasHistoriaPrincipal").value(17.2));
        mvc.perform(get("/api/covers/" + id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        assertNotNull(tx.execute(s -> repositorio.buscarPorId(Obra.class, id).cover));
    }

    @Test
    void juegoManualConservaHorasJugadasYRecuperaPortada() throws Exception {
        var usuario = account();
        var sesion = login(usuario);
        var progreso = catalog.agregarObra(usuario.id, new DatosObra("Cyberpunk 2077", TipoMedio.GAME,
                "", "", 12, 15, false));
        catalog.actualizarProgreso(usuario.id, progreso.id, new DatosProgreso(EstadoProgreso.IN_PROGRESS, 32, 0, 0, ""));
        mvc.perform(get("/api/catalog").session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].work.horasHistoriaPrincipal").value(26.0))
                .andExpect(jsonPath("$[0].currentUnits").value(32));
        mvc.perform(get("/api/covers/" + progreso.work.id).session(sesion)).andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
        assertNotNull(tx.execute(s -> repositorio.buscarPorId(Obra.class, progreso.work.id).cover));
    }

    @Test
    void registrationHashesPasswordAndCreatesZeroWallet() {
        var a = account();
        assertNotEquals("Password-2026!", a.passwordHash);
        assertTrue(a.passwordHash.startsWith("$2"));
        assertEquals(0, balance(a.id));
        assertThrows(ErrorNegocio.class,
                () -> accounts.register(new Registro(a.username, a.email, "Password-2026!")));
    }

    @Test
    void loginAndProfileDoNotLeakSecrets() throws Exception {
        var a = account();
        var session = login(a);
        var result = body(
                mvc.perform(get("/api/me").session(session)).andExpect(status().isOk()).andReturn());
        assertFalse(result.has("passwordHash"));
        assertEquals(0, result.path("balance").asInt());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void authenticationCsrfValidationAndAdminBoundary() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        var a = account();
        var session = login(a);
        mvc.perform(post("/api/tasks").session(session).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tasks").session(session).with(csrf()).contentType("application/json").content(
                "{\"title\":\"\",\"category\":\"ESTUDIO\",\"priority\":\"ALTA\",\"estimatedMinutes\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/ai").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void timerPauseExcludesIdleTimeAndCompletionIsIdempotent() {
        var a = account();
        var t = task(a.id, 10);
        tasks.aplicarAccion(a.id, t.id, "start");
        clock.advance(180);
        tasks.aplicarAccion(a.id, t.id, "pause");
        clock.advance(600);
        tasks.aplicarAccion(a.id, t.id, "start");
        clock.advance(120);
        var done = tasks.aplicarAccion(a.id, t.id, "complete");
        assertEquals(300, done.elapsedSeconds);
        assertEquals(20, done.coins);
        tasks.aplicarAccion(a.id, t.id, "complete");
        assertEquals(20, balance(a.id));
        assertEquals(1, tx.execute(s -> repositorio.movimientos(a.id).size()).intValue());
    }

    @Test
    void manualCompletionAndCancellationDoNotMintCoins() {
        var a = account();
        var manual = task(a.id, 60);
        assertEquals(0, tasks.aplicarAccion(a.id, manual.id, "complete").coins);
        var t = task(a.id, 60);
        tasks.aplicarAccion(a.id, t.id, "start");
        clock.advance(600);
        tasks.aplicarAccion(a.id, t.id, "cancel");
        assertEquals(0, tasks.aplicarAccion(a.id, t.id, "complete").coins);
        assertEquals(0, balance(a.id));
    }

    @Test
    void ownersCannotAccessEachOthersTasks() throws Exception {
        var a = account();
        var b = account();
        var t = task(a.id, 20);
        var session = login(b);
        mvc.perform(post("/api/tasks/" + t.id + "/start").session(session).with(csrf()))
                .andExpect(status().isNotFound());
        assertThrows(ErrorNegocio.class,
                () -> tasks.editarTarea(b.id, t.id, new DatosTarea("Otro", "", "ESTUDIO", "ALTA", 10)));
    }

    @Test
    void estimatesCannotChangeAfterTimerStarts() {
        var a = account();
        var t = task(a.id, 20);
        tasks.aplicarAccion(a.id, t.id, "start");
        tasks.aplicarAccion(a.id, t.id, "pause");
        assertThrows(ErrorNegocio.class,
                () -> tasks.editarTarea(a.id, t.id, new DatosTarea("Trampa", "", "ESTUDIO", "ALTA", 720)));
    }

    @Test
    void activeTaskAndRestAreExclusive() {
        var a = account();
        fund(a.id);
        work(a.id, "Exclusividad" + a.id, 10);
        var t = task(a.id, 10);
        var second = task(a.id, 10);
        tasks.aplicarAccion(a.id, t.id, "start");
        assertThrows(ErrorNegocio.class, () -> tasks.aplicarAccion(a.id, second.id, "start"));
        assertThrows(ErrorNegocio.class, () -> rests.spin(a.id, 15, null));
        tasks.aplicarAccion(a.id, t.id, "pause");
        var r = rests.spin(a.id, 15, null);
        assertThrows(ErrorNegocio.class, () -> tasks.aplicarAccion(a.id, t.id, "start"));
        rests.finish(a.id, r.id, true);
        assertEquals(160, balance(a.id));
        tasks.aplicarAccion(a.id, t.id, "start");
    }

    @Test
    void insufficientOrIncompatibleCatalogNeverCharges() {
        var a = account();
        work(a.id, "Larga" + a.id, 45);
        assertThrows(ErrorNegocio.class, () -> rests.spin(a.id, 15, null));
        assertEquals(0, balance(a.id));
        fund(a.id);
        assertThrows(ErrorNegocio.class, () -> rests.spin(a.id, 15, null));
        assertEquals(240, balance(a.id));
    }

    @Test
    void dailyCapIsAppliedAcrossTasksAndResetsNextDay() {
        var a = account();
        for (int i = 0; i < 2; i++) {
            var t = task(a.id, 720);
            tasks.aplicarAccion(a.id, t.id, "start");
            clock.advance(300);
            tasks.aplicarAccion(a.id, t.id, "complete");
        }
        assertEquals(1500, balance(a.id));
        clock.advance(86400);
        fund(a.id);
        assertEquals(1740, balance(a.id));
    }

    @Test
    void progressBoundsDuplicatesAndAutomaticBadges() {
        var a = account();
        var p = work(a.id, "Libro" + a.id, 10);
        assertThrows(ErrorNegocio.class, () -> work(a.id, "Libro" + a.id, 10));
        assertThrows(ErrorNegocio.class, () -> catalog.actualizarProgreso(a.id, p.id,
                new DatosProgreso(EstadoProgreso.IN_PROGRESS, 21, 0, 0, "")));
        catalog.actualizarProgreso(a.id, p.id, new DatosProgreso(EstadoProgreso.COMPLETED, 20, 2, 0, ""));
        assertTrue(tx.<Boolean>execute(
                s -> repositorio.insignias(a.id).stream().anyMatch(b -> b.code.equals("FINISHER"))));
    }

    @Test
    void missionClaimIsOnceOnlyAndExpiresAtMadridWeekBoundary() {
        var a = account();
        var p = work(a.id, "Misión" + a.id, 10);
        var m = missions.generate(a.id).getFirst();
        catalog.actualizarProgreso(a.id, p.id, new DatosProgreso(EstadoProgreso.IN_PROGRESS, 3, 0, 0, ""));
        missions.claim(a.id, m.id);
        assertEquals(40, balance(a.id));
        assertThrows(ErrorNegocio.class, () -> missions.claim(a.id, m.id));
        var b = account();
        var p2 = work(b.id, "Caduca" + b.id, 10);
        var m2 = missions.generate(b.id).getFirst();
        catalog.actualizarProgreso(b.id, p2.id, new DatosProgreso(EstadoProgreso.IN_PROGRESS, 3, 0, 0, ""));
        clock.instant = m2.expiresAt;
        assertThrows(ErrorNegocio.class, () -> missions.claim(b.id, m2.id));
        assertEquals(0, balance(b.id));
    }

    @Test
    void cooperativesRequireConsentAndChargeBothAtomically() throws Exception {
        var a = account();
        var b = account();
        fund(a.id);
        fund(b.id);
        String title = "Compartido" + a.id;
        work(a.id, title, 10);
        work(b.id, title, 10);
        var f = social.request(a.id, b.username);
        social.decide(b.id, f.id, true);
        var i = coop.invite(a.id, b.id, 15);
        assertEquals(240, balance(a.id));
        assertEquals(240, balance(b.id));
        assertThrows(ErrorNegocio.class, () -> coop.decide(a.id, i.id, true));
        coop.decide(b.id, i.id, true);
        assertEquals(160, balance(a.id));
        assertEquals(160, balance(b.id));
        var ar = rests.history(a.id).getFirst();
        var br = rests.history(b.id).getFirst();
        assertEquals(ar.work.id, br.work.id);
        assertThrows(ErrorNegocio.class, () -> coop.decide(b.id, i.id, true));
        var session = login(a);
        mvc.perform(post("/api/rests").session(session).with(csrf()).contentType("application/json")
                .content("{\"minutes\":15,\"friendId\":" + b.id + "}")).andExpect(status().isBadRequest());
    }

    @Test
    void missingCommonCatalogRollsBackBothWallets() {
        var a = account();
        var b = account();
        fund(a.id);
        fund(b.id);
        work(a.id, "Solo A" + a.id, 10);
        work(b.id, "Solo B" + b.id, 10);
        var f = social.request(a.id, b.username);
        social.decide(b.id, f.id, true);
        var i = coop.invite(a.id, b.id, 15);
        assertThrows(ErrorNegocio.class, () -> coop.decide(b.id, i.id, true));
        assertEquals(240, balance(a.id));
        assertEquals(240, balance(b.id));
    }

    @Test
    void concurrentCompletionCannotDoubleCredit() throws Exception {
        var a = account();
        var t = task(a.id, 40);
        tasks.aplicarAccion(a.id, t.id, "start");
        clock.advance(300);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            var first = executor.submit(() -> {
                gate.await();
                return tasks.aplicarAccion(a.id, t.id, "complete");
            });
            var second = executor.submit(() -> {
                gate.await();
                return tasks.aplicarAccion(a.id, t.id, "complete");
            });
            gate.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertEquals(80, balance(a.id));
        assertEquals(1, tx.execute(s -> repositorio.movimientos(a.id).size()).intValue());
    }

    @Test
    void concurrentSpinsCannotOverspend() throws Exception {
        var a = account();
        fund(a.id);
        work(a.id, "Concurrencia" + a.id, 10);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var gate = new CountDownLatch(1);
            Callable<Boolean> spin = () -> {
                gate.await();
                try {
                    rests.spin(a.id, 15, null);
                    return true;
                } catch (ErrorNegocio e) {
                    return false;
                }
            };
            var first = executor.submit(spin);
            var second = executor.submit(spin);
            gate.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
        assertEquals(160, balance(a.id));
        assertEquals(1, rests.history(a.id).size());
    }

    @Test
    void suspendedAccountsLoseAccessImmediately() throws Exception {
        var admin = account();
        tx.executeWithoutResult(s -> repositorio.buscarPorId(Usuario.class, admin.id).role = "ADMIN");
        var a = account();
        var session = login(a);
        accounts.administer(admin.id, a.id, "SUSPEND");
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
        accounts.administer(admin.id, a.id, "REACTIVATE");
        mvc.perform(get("/api/me").session(session)).andExpect(status().isOk());
        accounts.administer(admin.id, a.id, "DELETE");
        assertThrows(ErrorNegocio.class, () -> accounts.administer(admin.id, a.id, "REACTIVATE"));
    }

    @Test
    void oracleFallbackExistsImmediatelyAndRestExpiryReleasesWork() {
        var a = account();
        fund(a.id);
        work(a.id, "Oráculo" + a.id, 10);
        var r = rests.spin(a.id, 15, null);
        assertTrue(tx.<Boolean>execute(s -> repositorio.recomendacion(r.id).text.contains("Oráculo")));
        clock.advance(900);
        var t = task(a.id, 10);
        tasks.aplicarAccion(a.id, t.id, "start");
        assertTrue(rests.history(a.id).getFirst().completed);
    }

    @Test
    void restCanBePausedAndResumed() {
        var a = account();
        fund(a.id);
        work(a.id, "Pausa" + a.id, 10);
        var r = rests.spin(a.id, 15, null);
        assertFalse(r.paused);
        assertEquals(900, r.remainingSeconds);

        // Avanzamos 300 segundos y pausamos
        clock.advance(300);
        var paused = rests.pause(a.id, r.id);
        assertTrue(paused.paused);
        assertEquals(600, paused.remainingSeconds);

        // El tiempo avanza mientras está pausado y no debe expirar
        clock.advance(1000);
        var history = rests.history(a.id);
        assertFalse(history.getFirst().completed);
        assertTrue(history.getFirst().paused);

        // Reanudar el descanso
        var resumed = rests.resume(a.id, r.id);
        assertFalse(resumed.paused);

        // Terminar el descanso restante
        clock.advance(600);
        assertTrue(rests.history(a.id).getFirst().completed);
    }
}
