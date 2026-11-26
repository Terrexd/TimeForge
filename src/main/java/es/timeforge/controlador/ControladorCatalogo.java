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
    private final ServicioUsuarios usuarios;
    private final ServicioCatalogo servicioCatalogo;
    private final Oraculo oraculo;
    private final CatalogoExterno catalogoExterno;

    public ControladorCatalogo(Repositorio repositorio, ServicioUsuarios usuarios, ServicioCatalogo servicioCatalogo,
            Oraculo oraculo, CatalogoExterno catalogoExterno) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.servicioCatalogo = servicioCatalogo;
        this.oraculo = oraculo;
        this.catalogoExterno = catalogoExterno;
    }

    private long idUsuario(Principal sesion) {
        if (sesion == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
        return usuarios.current(sesion.getName()).id;
    }

    /** Obtiene el backlog completo del usuario con su progreso individual. */
    @GetMapping("/catalog")
    @Transactional(readOnly = true)
    public List<Progreso> listarCatalogo(Principal sesion) {
        return repositorio.catalogoPersonal(idUsuario(sesion));
    }

    /** Añade manualmente una nueva obra al catálogo personal. */
    @PostMapping("/catalog")
    public Progreso agregarObra(Principal sesion, @Valid @RequestBody DatosObra datos) {
        return servicioCatalogo.agregarObra(idUsuario(sesion), datos);
    }

    /** Actualiza el estado y progreso (capítulos, horas) de una obra. */
    @PutMapping("/catalog/{id}")
    public Progreso actualizarProgreso(Principal sesion, @PathVariable long id,
            @Valid @RequestBody DatosProgreso datos) {
        return servicioCatalogo.actualizarProgreso(idUsuario(sesion), id, datos);
    }

    @PostMapping("/catalog/{id}/paso")
    public Progreso ajustarCapitulo(Principal sesion, @PathVariable long id, @RequestParam int paso) {
        return servicioCatalogo.ajustarCapitulo(idUsuario(sesion), id, paso);
    }

    /** Elimina una obra del backlog personal del usuario. */
    @DeleteMapping("/catalog/{id}")
    public void eliminarObra(Principal sesion, @PathVariable long id) {
        servicioCatalogo.eliminarObra(idUsuario(sesion), id);
    }

    /** Clasifica automáticamente una obra mediante Inteligencia Artificial a partir de su sinopsis. */
    @PostMapping("/catalog/{id}/classify")
    public Oraculo.Answer clasificarObra(Principal sesion, @PathVariable long id) {
        return oraculo.classify(idUsuario(sesion), id);
    }

    /** Busca obras en catálogos remotos (IGDB o Steam para juegos, Jikan/AniList para manga y anime). */
    @GetMapping("/catalog/search")
    public List<Map<String, Object>> buscarObrasExternas(Principal sesion, @RequestParam String source,
            @RequestParam(defaultValue = "") String q) {
        idUsuario(sesion);
        return catalogoExterno.search(source, q);
    }

    /** Importa una obra remota al catálogo del usuario con su portada e información. */
    @PostMapping("/catalog/import")
    public Progreso importarObra(Principal sesion, @Valid @RequestBody DatosImportacion datos) {
        return catalogoExterno.importWork(idUsuario(sesion), datos.source(), datos.externalId());
    }

    /** Sirve la portada almacenada de una obra multimedia. */
    @GetMapping("/covers/{id}")
    public ResponseEntity<byte[]> obtenerPortada(Principal sesion, @PathVariable long id) {
        idUsuario(sesion);
        var obra = repositorio.buscarPorId(Obra.class, id);
        if (obra == null)
            return ResponseEntity.notFound().build();
        if (obra.cover == null && obra.type == es.timeforge.modelo.entidades.TipoMedio.GAME) {
            obra.cover = catalogoExterno.portadaJuego(obra);
            if (obra.cover != null) {
                obra.coverType = es.timeforge.modelo.integraciones.ClienteJson.tipoImagen(obra.cover);
                repositorio.guardarPortadaSiFalta(id, obra.cover, obra.coverType);
            }
        }
        if (obra.cover == null)
            return ResponseEntity.status(404).cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate()).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(obra.coverType))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate()).body(obra.cover);
    }
}
