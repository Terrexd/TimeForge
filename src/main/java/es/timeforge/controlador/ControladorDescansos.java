package es.timeforge.controlador;

import es.timeforge.modelo.entidades.Recomendacion;
import es.timeforge.modelo.entidades.Descanso;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.servicios.ServicioDescansos;
import es.timeforge.modelo.DatosEntrada.DatosDescanso;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la economía de ocio: Billetera de monedas virtuales, ruleta de descansos y El Oráculo (IA).
 */
@RestController
@RequestMapping("/api")
public class ControladorDescansos {

    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final ServicioDescansos rests;

    public ControladorDescansos(Repositorio repositorio, ServicioUsuarios accounts, ServicioDescansos rests) {
        this.repositorio = repositorio;
        this.accounts = accounts;
        this.rests = rests;
    }

    private long user(Principal principal) {
        if (principal == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Consulta el balance de monedas y el historial de movimientos de la billetera. */
    @GetMapping("/wallet")
    @Transactional(readOnly = true)
    public Map<String, Object> consultarBilletera(Principal principal) {
        long id = user(principal);
        return Map.of("balance", repositorio.billetera(id).balance, "movements", repositorio.movimientos(id));
    }

    /** Obtiene el historial de sesiones de descanso desbloqueadas. */
    @GetMapping("/rests")
    public List<Descanso> listarDescansos(Principal principal) {
        return rests.history(user(principal));
    }

    /** Ejecuta una tirada en la ruleta de descanso canjeando monedas. */
    @PostMapping("/rests")
    public Descanso girarRuleta(Principal principal, @Valid @RequestBody DatosDescanso input) {
        ErrorNegocio.require(input.friendId() == null,
                "Las tiradas conjuntas requieren una invitación aceptada");
        return rests.spin(user(principal), input.minutes(), null);
    }

    /** Finaliza o cancela una sesión de descanso en curso. */
    @PostMapping("/rests/{id}/{action}")
    public Descanso finalizarDescanso(Principal principal, @PathVariable long id, @PathVariable String action) {
        ErrorNegocio.require(action.equals("cancel") || action.equals("finish"), "Acción desconocida");
        return rests.finish(user(principal), id, action.equals("cancel"));
    }

    /** Consulta la justificación contextual generada por «El Oráculo» (IA) para un descanso. */
    @GetMapping("/rests/{id}/oracle")
    @Transactional(readOnly = true)
    public Recomendacion consultarOraculo(Principal principal, @PathVariable long id) {
        long userId = user(principal);
        var rest = repositorio.buscarPorId(Descanso.class, id);
        ErrorNegocio.require(rest != null && rest.owner.id == userId, "Descanso no disponible");
        return repositorio.recomendacion(id);
    }
}
