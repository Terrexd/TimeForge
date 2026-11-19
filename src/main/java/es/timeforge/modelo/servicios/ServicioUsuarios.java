package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.DatosEntrada.*;
import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
@Transactional
public class ServicioUsuarios {
    private final Repositorio repositorio;
    private final PasswordEncoder passwords;
    private final Clock clock;

    public ServicioUsuarios(Repositorio repositorio, PasswordEncoder passwords, Clock clock) {
        this.repositorio = repositorio;
        this.passwords = passwords;
        this.clock = clock;
    }

    public Usuario register(Registro input) {
        String name = input.username().trim().toLowerCase(Locale.ROOT),
                email = input.email().trim().toLowerCase(Locale.ROOT);
        require(repositorio.buscarUsuario(name) == null && !repositorio.existeCorreo(email),
                "El correo electrónico o nombre de usuario ya está en uso");
        require(input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
                "La contraseña no puede superar 72 bytes UTF-8");
        var a = new Usuario();
        a.username = name;
        a.email = email;
        a.passwordHash = passwords.encode(input.password());
        a.displayName = input.username();
        a.createdAt = clock.instant();
        repositorio.guardar(a);
        var w = new Billetera();
        w.owner = a;
        w.updatedAt = clock.instant();
        repositorio.guardar(w);
        return a;
    }

    public Usuario current(String username) {
        Usuario a = repositorio.buscarUsuario(username);
        if (a == null || !a.active || a.deleted)
            throw new ErrorNegocio(401, "La sesión no está activa. Inicia sesión de nuevo");
        return a;
    }

    public Usuario locked(long id) {
        Usuario a = repositorio.bloquearUsuario(id);
        if (a == null || !a.active || a.deleted)
            throw new ErrorNegocio(401, "Cuenta no disponible");
        return a;
    }

    public Map<String, Object> publicProfile(Usuario a) {
        return Map.of("id", a.id, "username", a.username, "displayName", a.displayName, "avatar", a.avatar,
                "bio", a.bio, "level", 1 + a.xp / 200, "xp", a.xp, "streak", a.streak, "badges",
                repositorio.insignias(a.id));
    }

    public void profile(long user, Perfil p) {
        var a = locked(user);
        a.displayName = p.displayName().trim();
        a.avatar = p.avatar();
        a.bio = p.bio() == null ? "" : p.bio();
    }

    public List<Map<String, Object>> search(String q) {
        require(q.length() >= 2 && q.length() <= 80, "Escribe entre 2 y 80 caracteres");
        return repositorio.buscarUsuarios(q.toLowerCase(Locale.ROOT)).stream().filter(a -> a.active)
                .map(this::publicProfile).toList();
    }

    public void administer(long admin, long target, String action) {
        var operator = locked(admin);
        if (!operator.role.equals("ADMIN"))
            throw new ErrorNegocio(403, "Acceso reservado a administradores");
        require(admin != target, "No puedes modificar tu propia cuenta desde administración");
        var a = repositorio.bloquearUsuario(target);
        require(a != null, "El usuario no existe");
        require(!a.role.equals("ADMIN"), "No se modifican otros administradores desde esta pantalla");
        switch (action) {
        case "SUSPEND" -> a.active = false;
        case "REACTIVATE" -> {
            require(!a.deleted, "Una cuenta dada de baja no puede reactivarse");
            a.active = true;
        }
        case "DELETE" -> {
            a.active = false;
            a.deleted = true;
            a.email = "deleted-" + a.id + "@invalid.local";
            a.displayName = "Cuenta eliminada";
            a.bio = "";
            a.avatar = "×";
            a.passwordHash = passwords.encode(UUID.randomUUID().toString());
        }
        default -> throw new ErrorNegocio("Operación desconocida");
        }
        // La suspensión congela trabajo y cancela ocio, sin devolver monedas.
        if (!a.active) {
            for (var t : repositorio.tareas(target))
                if (t.state == EstadoTarea.RUNNING) {
                    t.elapsedSeconds += Math.max(0,
                            Duration.between(t.startedAt, clock.instant()).toSeconds());
                    t.startedAt = null;
                    t.state = EstadoTarea.PAUSED;
                }
            for (var r : repositorio.descansosActivos(target, clock.instant()))
                r.cancelled = true;
        }
    }
}
