package es.timeforge.controller;

import es.timeforge.model.Recommendation;
import es.timeforge.model.Rest;
import es.timeforge.repository.Store;
import es.timeforge.service.AccountService;
import es.timeforge.service.BusinessException;
import es.timeforge.service.RestService;
import es.timeforge.controller.Inputs.RestInput;
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
public class RestSessionController {

    private final Store store;
    private final AccountService accounts;
    private final RestService rests;

    public RestSessionController(Store store, AccountService accounts, RestService rests) {
        this.store = store;
        this.accounts = accounts;
        this.rests = rests;
    }

    private long user(Principal principal) {
        if (principal == null) throw new BusinessException(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Consulta el balance de monedas y el historial de movimientos de la billetera. */
    @GetMapping("/wallet")
    @Transactional(readOnly = true)
    public Map<String, Object> getWallet(Principal principal) {
        long id = user(principal);
        return Map.of("balance", store.wallet(id).balance, "movements", store.movements(id));
    }

    /** Obtiene el historial de sesiones de descanso desbloqueadas. */
    @GetMapping("/rests")
    public List<Rest> listRests(Principal principal) {
        return rests.history(user(principal));
    }

    /** Ejecuta una tirada en la ruleta de descanso canjeando monedas. */
    @PostMapping("/rests")
    public Rest spinWheel(Principal principal, @Valid @RequestBody RestInput input) {
        BusinessException.require(input.friendId() == null,
                "Las tiradas conjuntas requieren una invitación aceptada");
        return rests.spin(user(principal), input.minutes(), null);
    }

    /** Finaliza o cancela una sesión de descanso en curso. */
    @PostMapping("/rests/{id}/{action}")
    public Rest finishRest(Principal principal, @PathVariable long id, @PathVariable String action) {
        BusinessException.require(action.equals("cancel") || action.equals("finish"), "Acción desconocida");
        return rests.finish(user(principal), id, action.equals("cancel"));
    }

    /** Consulta la justificación contextual generada por «El Oráculo» (IA) para un descanso. */
    @GetMapping("/rests/{id}/oracle")
    @Transactional(readOnly = true)
    public Recommendation getOracleRecommendation(Principal principal, @PathVariable long id) {
        long userId = user(principal);
        var rest = store.get(Rest.class, id);
        BusinessException.require(rest != null && rest.owner.id == userId, "Descanso no disponible");
        return store.recommendation(id);
    }
}
