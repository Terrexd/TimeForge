package es.timeforge.controlador;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.DatosEntrada.Perfil;
import es.timeforge.modelo.DatosEntrada.Registro;
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
public class ControladorUsuarios {

    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final Clock clock;

    public ControladorUsuarios(Repositorio repositorio, ServicioUsuarios accounts, Clock clock) {
        this.repositorio = repositorio;
        this.accounts = accounts;
        this.clock = clock;
    }

    private long user(Principal principal) {
        if (principal == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
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
    public Map<String, Object> registrar(@Valid @RequestBody Registro input) {
        var account = accounts.register(input);
        return Map.of("id", account.id, "username", account.username);
    }

    /** Obtiene los datos del perfil y saldo del usuario autenticado. */
    @GetMapping("/me")
    @Transactional
    public Map<String, Object> miPerfil(Principal principal) {
        long id = user(principal);
        var account = repositorio.buscarPorId(Usuario.class, id);
        var result = new HashMap<>(accounts.publicProfile(account));
        result.put("email", account.email);
        result.put("role", account.role);
        result.put("balance", repositorio.billetera(id).balance);
        result.put("serverNow", clock.instant());
        return result;
    }

    /** Actualiza la información personal del perfil (nombre para mostrar, avatar, biografía). */
    @PutMapping("/me")
    public void actualizarPerfil(Principal principal, @Valid @RequestBody Perfil input) {
        accounts.profile(user(principal), input);
    }
}
