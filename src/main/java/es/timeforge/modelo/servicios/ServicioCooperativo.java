package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

/** El destinatario debe consentir el gasto: ser amigo no autoriza a gastar su saldo. */
@Service
@Transactional
public class ServicioCooperativo {
    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final ServicioDescansos rests;
    private final Clock clock;

    public ServicioCooperativo(Repositorio s, ServicioUsuarios a, ServicioDescansos r, Clock c) {
        repositorio = s;
        accounts = a;
        rests = r;
        clock = c;
    }

    public InvitacionCooperativa invite(long user, long friend, int minutes) {
        require(user != friend, "Selecciona a un amigo");
        var sender = accounts.locked(user);
        var f = repositorio.amistad(user, friend);
        require(f != null && f.state == EstadoAmistad.ACCEPTED, "Necesitáis una amistad aceptada");
        int cost = ReglasRuleta.cost(minutes);
        require(repositorio.invitaciones(friend, clock.instant()).stream().noneMatch(i -> i.senderId == user),
                "Ya has enviado una invitación pendiente a este amigo");
        var i = new InvitacionCooperativa();
        i.senderId = user;
        i.receiverId = friend;
        i.senderName = sender.displayName;
        i.minutes = minutes;
        i.cost = cost;
        i.expiresAt = clock.instant().plusSeconds(600);
        return repositorio.guardar(i);
    }

    public List<InvitacionCooperativa> list(long user) {
        return repositorio.invitaciones(user, clock.instant());
    }

    public void decide(long user, long id, boolean accept) {
        var i = repositorio.bloquearInvitacion(id);
        require(i != null && i.receiverId == user, "Invitación no disponible");
        require(i.state.equals("PENDING") && clock.instant().isBefore(i.expiresAt),
                "La invitación ha caducado o ya se ha resuelto");
        if (accept)
            rests.spin(i.senderId, i.minutes, i.receiverId);
        i.state = accept ? "ACCEPTED" : "REJECTED";
    }
}
