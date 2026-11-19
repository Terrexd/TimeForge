package es.timeforge.controlador;

import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.DatosEntrada.AccionAdministrativa;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de Administración técnica: gestión de usuarios, auditoría y monitorización de IA.
 * Requiere rol 'ROLE_ADMIN'.
 */
@RestController
@RequestMapping("/api/admin")
public class ControladorAdministracion {

    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;

    public ControladorAdministracion(Repositorio repositorio, ServicioUsuarios accounts) {
        this.repositorio = repositorio;
        this.accounts = accounts;
    }

    private long user(Principal principal) {
        if (principal == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Lista todos los usuarios registrados con opción de filtrado para administración. */
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarUsuarios(Principal principal,
            @RequestParam(defaultValue = "") String q) {
        user(principal);
        return repositorio.buscarUsuarios(q).stream()
                .map(account -> Map.<String, Object>of("id", account.id, "username", account.username,
                        "email", account.email, "role", account.role, "active", account.active))
                .toList();
    }

    /** Ejecuta acciones administrativas sobre una cuenta: SUSPEND, REACTIVATE, DELETE. */
    @PostMapping("/users/{id}")
    public void administrarUsuario(Principal principal, @PathVariable long id,
            @Valid @RequestBody AccionAdministrativa input) {
        accounts.administer(user(principal), id, input.action());
    }

    /** Obtiene métricas agregadas de llamadas, tiempos y tokens del servicio de Inteligencia Artificial. */
    @GetMapping("/ai")
    @Transactional(readOnly = true)
    public Map<String, Object> consultarMetricasIA(Principal principal) {
        user(principal);
        return Map.of("metrics", repositorio.metricasIA(), "calls", repositorio.llamadasIA());
    }
}
