package es.timeforge.controller;

import es.timeforge.repository.Store;
import es.timeforge.service.AccountService;
import es.timeforge.service.BusinessException;
import es.timeforge.controller.Inputs.AdminInput;
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
public class AdminController {

    private final Store store;
    private final AccountService accounts;

    public AdminController(Store store, AccountService accounts) {
        this.store = store;
        this.accounts = accounts;
    }

    private long user(Principal principal) {
        if (principal == null) throw new BusinessException(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Lista todos los usuarios registrados con opción de filtrado para administración. */
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminUsers(Principal principal, @RequestParam(defaultValue = "") String q) {
        user(principal);
        return store.users(q).stream().map(account -> Map.<String, Object>of(
                "id", account.id,
                "username", account.username,
                "email", account.email,
                "role", account.role,
                "active", account.active
        )).toList();
    }

    /** Ejecuta acciones administrativas sobre una cuenta: SUSPEND, REACTIVATE, DELETE. */
    @PostMapping("/users/{id}")
    public void administerUser(Principal principal, @PathVariable long id, @Valid @RequestBody AdminInput input) {
        accounts.administer(user(principal), id, input.action());
    }

    /** Obtiene métricas agregadas de llamadas, tiempos y tokens del servicio de Inteligencia Artificial. */
    @GetMapping("/ai")
    @Transactional(readOnly = true)
    public Map<String, Object> aiMetrics(Principal principal) {
        user(principal);
        return Map.of("metrics", store.aiMetrics(), "calls", store.aiCalls());
    }
}
