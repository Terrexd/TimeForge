package es.timeforge.controller;

import es.timeforge.model.Mission;
import es.timeforge.service.AccountService;
import es.timeforge.service.BusinessException;
import es.timeforge.service.MissionService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la gestión y reclamo de misiones dinámicas semanales.
 */
@RestController
@RequestMapping("/api")
public class MissionController {

    private final AccountService accounts;
    private final MissionService missions;

    public MissionController(AccountService accounts, MissionService missions) {
        this.accounts = accounts;
        this.missions = missions;
    }

    private long user(Principal principal) {
        if (principal == null) throw new BusinessException(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Lista o genera las misiones activas de la semana para el usuario. */
    @GetMapping("/missions")
    public List<Mission> listMissions(Principal principal) {
        return missions.generate(user(principal));
    }

    /** Reclama la recompensa de monedas de una misión completada. */
    @PostMapping("/missions/{id}/claim")
    public Mission claimReward(Principal principal, @PathVariable long id) {
        return missions.claim(user(principal), id);
    }
}
