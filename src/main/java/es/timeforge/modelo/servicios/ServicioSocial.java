package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
@Transactional
public class ServicioSocial {
    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final Clock clock;

    public ServicioSocial(Repositorio s, ServicioUsuarios a, Clock c) {
        repositorio = s;
        accounts = a;
        clock = c;
    }

    public Amistad request(long user, String username) {
        var target = repositorio.buscarUsuario(username.trim().toLowerCase(Locale.ROOT));
        require(target != null && target.active && !target.deleted, "Usuario no disponible");
        require(target.id != user, "No puedes enviarte una solicitud");
        accounts.locked(Math.min(user, target.id));
        accounts.locked(Math.max(user, target.id));
        var f = repositorio.amistad(user, target.id);
        require(f == null || f.state == EstadoAmistad.REJECTED,
                "Ya existe una amistad o solicitud entre ambos");
        if (f == null) {
            f = new Amistad();
            f.lowId = Math.min(user, target.id);
            f.highId = Math.max(user, target.id);
            repositorio.guardar(f);
        }
        f.senderId = user;
        f.receiverId = target.id;
        f.createdAt = clock.instant();
        f.state = EstadoAmistad.PENDING;
        return f;
    }

    public void decide(long user, long id, boolean accept) {
        accounts.locked(user);
        var f = repositorio.buscarPorId(Amistad.class, id);
        require(f != null && f.receiverId == user && f.state == EstadoAmistad.PENDING,
                "La solicitud no está disponible");
        f.state = accept ? EstadoAmistad.ACCEPTED : EstadoAmistad.REJECTED;
    }

    public void remove(long user, long id) {
        accounts.locked(user);
        var f = repositorio.buscarPorId(Amistad.class, id);
        require(f != null && (f.lowId == user || f.highId == user), "Amistad no disponible");
        f.state = EstadoAmistad.REJECTED;
    }

    public List<Map<String, Object>> list(long user) {
        return repositorio.amistades(user).stream().filter(f -> f.state != EstadoAmistad.REJECTED).map(f -> {
            Usuario other = repositorio.buscarPorId(Usuario.class, f.lowId == user ? f.highId : f.lowId);
            return Map.<String, Object>of("id", f.id, "state", f.state, "incoming", f.receiverId == user,
                    "profile", accounts.publicProfile(other), "available", other.active && !other.deleted);
        }).toList();
    }
}
