package es.timeforge.controller;

import es.timeforge.model.*;
import es.timeforge.repository.Store;
import es.timeforge.service.AccountService;
import es.timeforge.service.BusinessException;
import es.timeforge.controller.Inputs.Profile;
import es.timeforge.controller.Inputs.Register;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para la gestión de autenticación, tokens CSRF y perfil del usuario.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final Store store;
    private final AccountService accounts;
    private final Clock clock;

    public AuthController(Store store, AccountService accounts, Clock clock) {
        this.store = store;
        this.accounts = accounts;
        this.clock = clock;
    }

    private long user(Principal principal) {
        if (principal == null) throw new BusinessException(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Obtiene el token CSRF para proteger las peticiones POST/PUT/DELETE. */
    @GetMapping("/auth/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    /** Registro de un nuevo usuario en la plataforma. */
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody Register input) {
        var account = accounts.register(input);
        return Map.of("id", account.id, "username", account.username);
    }

    /** Obtiene los datos del perfil y saldo del usuario autenticado. */
    @GetMapping("/me")
    @Transactional
    public Map<String, Object> me(Principal principal) {
        long id = user(principal);
        var account = store.get(Account.class, id);
        var result = new HashMap<>(accounts.publicProfile(account));
        result.put("email", account.email);
        result.put("role", account.role);
        result.put("balance", store.wallet(id).balance);
        result.put("serverNow", clock.instant());
        return result;
    }

    /** Actualiza la información personal del perfil (nombre para mostrar, avatar, biografía). */
    @PutMapping("/me")
    public void updateProfile(Principal principal, @Valid @RequestBody Profile input) {
        accounts.profile(user(principal), input);
    }
}
