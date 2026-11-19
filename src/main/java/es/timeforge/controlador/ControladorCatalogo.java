package es.timeforge.controlador;

import es.timeforge.modelo.entidades.Progreso;
import es.timeforge.modelo.entidades.Obra;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.servicios.ServicioCatalogo;
import es.timeforge.modelo.integraciones.CatalogoExterno;
import es.timeforge.modelo.integraciones.Oraculo;
import es.timeforge.modelo.DatosEntrada.DatosImportacion;
import es.timeforge.modelo.DatosEntrada.DatosProgreso;
import es.timeforge.modelo.DatosEntrada.DatosObra;
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
public class ControladorCatalogo {

    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final ServicioCatalogo catalog;
    private final Oraculo oracle;
    private final CatalogoExterno external;

    public ControladorCatalogo(Repositorio repositorio, ServicioUsuarios accounts, ServicioCatalogo catalog,
            Oraculo oracle, CatalogoExterno external) {
        this.repositorio = repositorio;
        this.accounts = accounts;
        this.catalog = catalog;
        this.oracle = oracle;
        this.external = external;
    }

    private long user(Principal principal) {
        if (principal == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Obtiene el backlog completo del usuario con su progreso individual. */
    @GetMapping("/catalog")
    @Transactional(readOnly = true)
    public List<Progreso> listarCatalogo(Principal principal) {
        return repositorio.catalogoPersonal(user(principal));
    }

    /** Añade manualmente una nueva obra al catálogo personal. */
    @PostMapping("/catalog")
    public Progreso agregarObra(Principal principal, @Valid @RequestBody DatosObra input) {
        return catalog.add(user(principal), input);
    }

    /** Actualiza el estado y progreso (capítulos, horas) de una obra. */
    @PutMapping("/catalog/{id}")
    public Progreso actualizarProgreso(Principal principal, @PathVariable long id,
            @Valid @RequestBody DatosProgreso input) {
        return catalog.update(user(principal), id, input);
    }

    /** Elimina una obra del backlog personal del usuario. */
    @DeleteMapping("/catalog/{id}")
    public void eliminarObra(Principal principal, @PathVariable long id) {
        catalog.delete(user(principal), id);
    }

    /** Clasifica automáticamente una obra mediante Inteligencia Artificial a partir de su sinopsis. */
    @PostMapping("/catalog/{id}/classify")
    public Oraculo.Answer clasificarObra(Principal principal, @PathVariable long id) {
        return oracle.classify(user(principal), id);
    }

    /** Busca obras en catálogos remotos (RAWG para juegos, Jikan/AniList para manga y anime). */
    @GetMapping("/catalog/search")
    public List<Map<String, Object>> buscarObrasExternas(Principal principal, @RequestParam String source,
            @RequestParam String q) {
        user(principal);
        return external.search(source, q);
    }

    /** Importa una obra remota al catálogo del usuario con su portada e información. */
    @PostMapping("/catalog/import")
    public Progreso importarObra(Principal principal, @Valid @RequestBody DatosImportacion input) {
        return external.importWork(user(principal), input.source(), input.externalId());
    }

    /** Sirve la portada almacenada de una obra multimedia. */
    @GetMapping("/covers/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> obtenerPortada(Principal principal, @PathVariable long id) {
        user(principal);
        var work = repositorio.buscarPorId(Obra.class, id);
        if (work == null || work.cover == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(work.coverType))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate()).body(work.cover);
    }
}
