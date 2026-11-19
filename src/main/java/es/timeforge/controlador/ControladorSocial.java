package es.timeforge.controlador;

import es.timeforge.modelo.entidades.InvitacionCooperativa;
import es.timeforge.modelo.entidades.Amistad;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.servicios.ServicioCooperativo;
import es.timeforge.modelo.servicios.ServicioSocial;
import es.timeforge.modelo.DatosEntrada.Decision;
import es.timeforge.modelo.DatosEntrada.SolicitudAmistad;
import es.timeforge.modelo.DatosEntrada.DatosDescanso;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para las funciones sociales: búsqueda de usuarios, lista de amigos y tiradas cooperativas.
 */
@RestController
@RequestMapping("/api")
public class ControladorSocial {

    private final ServicioUsuarios accounts;
    private final ServicioSocial social;
    private final ServicioCooperativo coop;

    public ControladorSocial(ServicioUsuarios accounts, ServicioSocial social, ServicioCooperativo coop) {
        this.accounts = accounts;
        this.social = social;
        this.coop = coop;
    }

    private long user(Principal principal) {
        if (principal == null)
            throw new ErrorNegocio(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Busca usuarios por coincidencia en nombre de usuario o nombre para mostrar. */
    @GetMapping("/users")
    public List<Map<String, Object>> buscarUsuarios(Principal principal, @RequestParam String q) {
        user(principal);
        return accounts.search(q);
    }

    /** Obtiene la lista de amigos confirmados y solicitudes pendientes. */
    @GetMapping("/friends")
    public List<Map<String, Object>> listarAmigos(Principal principal) {
        return social.list(user(principal));
    }

    /** Envía una solicitud de amistad a otro usuario. */
    @PostMapping("/friends")
    public Amistad enviarSolicitud(Principal principal, @Valid @RequestBody SolicitudAmistad input) {
        return social.request(user(principal), input.username());
    }

    /** Acepta o rechaza una solicitud de amistad. */
    @PostMapping("/friends/{id}")
    public void responderSolicitud(Principal principal, @PathVariable long id, @RequestBody Decision input) {
        social.decide(user(principal), id, input.accept());
    }

    /** Elimina un amigo de la lista de contactos. */
    @DeleteMapping("/friends/{id}")
    public void eliminarAmigo(Principal principal, @PathVariable long id) {
        social.remove(user(principal), id);
    }

    /** Consulta las invitaciones a tiradas cooperativas pendientes. */
    @GetMapping("/coop")
    public List<InvitacionCooperativa> listarInvitaciones(Principal principal) {
        return coop.list(user(principal));
    }

    /** Envía una invitación a un amigo para una tirada de ruleta compartida. */
    @PostMapping("/coop")
    public InvitacionCooperativa enviarInvitacion(Principal principal,
            @Valid @RequestBody DatosDescanso input) {
        ErrorNegocio.require(input.friendId() != null, "Selecciona un amigo para la tirada cooperativa");
        return coop.invite(user(principal), input.friendId(), input.minutes());
    }

    /** Acepta o rechaza una invitación a una tirada cooperativa. */
    @PostMapping("/coop/{id}")
    public void responderInvitacion(Principal principal, @PathVariable long id, @RequestBody Decision input) {
        coop.decide(user(principal), id, input.accept());
    }
}
