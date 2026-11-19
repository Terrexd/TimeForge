package es.timeforge.configuracion;

import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import es.timeforge.modelo.DatosEntrada.Registro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final Repositorio repositorio;
    private final ServicioUsuarios accounts;
    private final String username, password;

    public AdministradorInicial(Repositorio s, ServicioUsuarios a, @Value("${timeforge.admin-user}") String u,
            @Value("${timeforge.admin-password}") String p) {
        repositorio = s;
        accounts = a;
        username = u;
        password = p;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank())
            return;
        if (!username.matches("[a-zA-Z0-9_]{3,40}") || password.length() < 10)
            throw new IllegalStateException("ADMIN_USER o ADMIN_PASSWORD no válidos");
        // Nunca eleva automáticamente una cuenta registrada previamente.
        if (repositorio.buscarUsuario(username.toLowerCase(java.util.Locale.ROOT)) == null) {
            var a = accounts.register(new Registro(username, username + "@timeforge.local", password));
            a.role = "ADMIN";
        }
    }
}
