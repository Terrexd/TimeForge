package es.timeforge.controller;

import es.timeforge.model.CoopInvite;
import es.timeforge.model.Friendship;
import es.timeforge.service.AccountService;
import es.timeforge.service.BusinessException;
import es.timeforge.service.CoopService;
import es.timeforge.service.SocialService;
import es.timeforge.controller.Inputs.Decision;
import es.timeforge.controller.Inputs.FriendInput;
import es.timeforge.controller.Inputs.RestInput;
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
public class SocialController {

    private final AccountService accounts;
    private final SocialService social;
    private final CoopService coop;

    public SocialController(AccountService accounts, SocialService social, CoopService coop) {
        this.accounts = accounts;
        this.social = social;
        this.coop = coop;
    }

    private long user(Principal principal) {
        if (principal == null) throw new BusinessException(401, "Inicia sesión para continuar");
        return accounts.current(principal.getName()).id;
    }

    /** Busca usuarios por coincidencia en nombre de usuario o nombre para mostrar. */
    @GetMapping("/users")
    public List<Map<String, Object>> searchUsers(Principal principal, @RequestParam String q) {
        user(principal);
        return accounts.search(q);
    }

    /** Obtiene la lista de amigos confirmados y solicitudes pendientes. */
    @GetMapping("/friends")
    public List<Map<String, Object>> listFriends(Principal principal) {
        return social.list(user(principal));
    }

    /** Envía una solicitud de amistad a otro usuario. */
    @PostMapping("/friends")
    public Friendship sendRequest(Principal principal, @Valid @RequestBody FriendInput input) {
        return social.request(user(principal), input.username());
    }

    /** Acepta o rechaza una solicitud de amistad. */
    @PostMapping("/friends/{id}")
    public void decideFriendship(Principal principal, @PathVariable long id, @RequestBody Decision input) {
        social.decide(user(principal), id, input.accept());
    }

    /** Elimina un amigo de la lista de contactos. */
    @DeleteMapping("/friends/{id}")
    public void removeFriend(Principal principal, @PathVariable long id) {
        social.remove(user(principal), id);
    }

    /** Consulta las invitaciones a tiradas cooperativas pendientes. */
    @GetMapping("/coop")
    public List<CoopInvite> listInvitations(Principal principal) {
        return coop.list(user(principal));
    }

    /** Envía una invitación a un amigo para una tirada de ruleta compartida. */
    @PostMapping("/coop")
    public CoopInvite sendInvitation(Principal principal, @Valid @RequestBody RestInput input) {
        BusinessException.require(input.friendId() != null, "Selecciona un amigo para la tirada cooperativa");
        return coop.invite(user(principal), input.friendId(), input.minutes());
    }

    /** Acepta o rechaza una invitación a una tirada cooperativa. */
    @PostMapping("/coop/{id}")
    public void decideCoop(Principal principal, @PathVariable long id, @RequestBody Decision input) {
        coop.decide(user(principal), id, input.accept());
    }
}
