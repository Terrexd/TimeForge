package es.timeforge.controller;

import es.timeforge.model.Progress;
import es.timeforge.model.Work;
import es.timeforge.repository.Store;
import es.timeforge.service.AccountService;
import es.timeforge.service.BusinessException;
import es.timeforge.service.CatalogService;
import es.timeforge.integration.ExternalCatalogService;
import es.timeforge.integration.OracleService;
import es.timeforge.controller.Inputs.ImportInput;
import es.timeforge.controller.Inputs.ProgressInput;
import es.timeforge.controller.Inputs.WorkInput;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la gestión del catálogo de ocio multimedia (backlog), búsqueda e importación externa.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final Store store;
    private final AccountService accounts;
    private final CatalogService catalog;
    private final OracleService oracle;
    private final ExternalCatalogService external;

    public CatalogController(Store store, AccountService accounts, CatalogService catalog,
                             OracleService oracle, ExternalCatalogService external) {
        this.store = store;
        this.accounts = accounts;
        this.catalog = catalog;
        this.oracle = oracle;
        this.external = external;
    }

    private long user(Principal principal) {
        if (principal == null) throw new BusinessException(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Obtiene el backlog completo del usuario con su progreso individual. */
    @GetMapping("/catalog")
    @Transactional(readOnly = true)
    public List<Progress> getBacklog(Principal principal) {
        return store.backlog(user(principal));
    }

    /** Añade manualmente una nueva obra al catálogo personal. */
    @PostMapping("/catalog")
    public Progress addWork(Principal principal, @Valid @RequestBody WorkInput input) {
        return catalog.add(user(principal), input);
    }

    /** Actualiza el estado y progreso (capítulos, horas) de una obra. */
    @PutMapping("/catalog/{id}")
    public Progress updateProgress(Principal principal, @PathVariable long id, @Valid @RequestBody ProgressInput input) {
        return catalog.update(user(principal), id, input);
    }

    /** Elimina una obra del backlog personal del usuario. */
    @DeleteMapping("/catalog/{id}")
    public void removeWork(Principal principal, @PathVariable long id) {
        catalog.delete(user(principal), id);
    }

    /** Clasifica automáticamente una obra mediante Inteligencia Artificial a partir de su sinopsis. */
    @PostMapping("/catalog/{id}/classify")
    public OracleService.Answer classifyWork(Principal principal, @PathVariable long id) {
        return oracle.classify(user(principal), id);
    }

    /** Busca obras en catálogos remotos (RAWG para juegos, Jikan/AniList para manga y anime). */
    @GetMapping("/catalog/search")
    public List<Map<String, Object>> searchExternal(Principal principal,
                                                    @RequestParam String source,
                                                    @RequestParam String q) {
        user(principal);
        return external.search(source, q);
    }

    /** Importa una obra remota al catálogo del usuario con su portada e información. */
    @PostMapping("/catalog/import")
    public Progress importExternal(Principal principal, @Valid @RequestBody ImportInput input) {
        return external.importWork(user(principal), input.source(), input.externalId());
    }

    /** Sirve la portada almacenada de una obra multimedia. */
    @GetMapping("/covers/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getCover(Principal principal, @PathVariable long id) {
        user(principal);
        var work = store.get(Work.class, id);
        if (work == null || work.cover == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(work.coverType))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(work.cover);
    }
}
