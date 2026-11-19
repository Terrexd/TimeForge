package es.timeforge.controlador;

import es.timeforge.modelo.entidades.Mision;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.servicios.ServicioMisiones;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la gestión y reclamo de misiones dinámicas semanales.
 */
@RestController
@RequestMapping("/api")
public class ControladorMisiones {

    private final ServicioUsuarios accounts;
    private final ServicioMisiones missions;

    public ControladorMisiones(ServicioUsuarios accounts, ServicioMisiones missions) {
        this.accounts = accounts;
        this.missions = missions;
    }

    private long user(Principal principal) {
        if (principal == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Lista o genera las misiones activas de la semana para el usuario. */
    @GetMapping("/missions")
    public List<Mision> listarMisiones(Principal principal) {
        return missions.generate(user(principal));
    }

    /** Reclama la recompensa de monedas de una misión completada. */
    @PostMapping("/missions/{id}/claim")
    public Mision reclamarRecompensa(Principal principal, @PathVariable long id) {
        return missions.claim(user(principal), id);
    }
}
