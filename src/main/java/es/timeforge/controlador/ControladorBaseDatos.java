package es.timeforge.controlador;

import java.util.ArrayList;
import java.util.List;
import es.timeforge.modelo.servicios.ErrorNegocio;
import es.timeforge.modelo.servicios.ServicioUsuarios;
import java.security.Principal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ControladorBaseDatos {
    private static final List<String> VISTAS = List.of("tareas", "misiones", "usuarios", "billeteras",
            "movimientos", "obras", "progresos", "descansos", "insignias", "amistades",
            "invitaciones", "recomendaciones", "llamadas_ia");
    private static final int FILAS_POR_PAGINA = 50;
    private final JdbcTemplate jdbc;
    private final ServicioUsuarios usuarios;

    public ControladorBaseDatos(JdbcTemplate jdbc, ServicioUsuarios usuarios) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
    }

    @GetMapping("/bbdd")
    public String abrir() {
        return "forward:/bbdd.html";
    }

    @GetMapping("/api/admin/bbdd")
    @ResponseBody
    public List<String> vistas(Principal principal) {
        usuarios.current(principal.getName());
        return VISTAS;
    }

    public record Pagina(List<String> columnas, List<List<Object>> filas, boolean hayMas) {}

    @GetMapping("/api/admin/bbdd/{vista}")
    @ResponseBody
    public Pagina consultar(Principal principal, @PathVariable String vista, @RequestParam(defaultValue = "1") int pagina) {
        usuarios.current(principal.getName());
        if (!VISTAS.contains(vista)) throw new ErrorNegocio(404, "Tabla desconocida");
        if (pagina < 1) throw new ErrorNegocio(400, "La página debe ser positiva");
        return jdbc.query("SELECT * FROM " + vista + " ORDER BY id LIMIT ? OFFSET ?", resultado -> {
            var columnas = new ArrayList<String>();
            var metadatos = resultado.getMetaData();
            for (int i = 1; i <= metadatos.getColumnCount(); i++) columnas.add(metadatos.getColumnLabel(i));
            var filas = new ArrayList<List<Object>>();
            while (filas.size() < FILAS_POR_PAGINA && resultado.next()) {
                var fila = new ArrayList<Object>();
                for (int i = 1; i <= columnas.size(); i++) fila.add(resultado.getObject(i));
                filas.add(fila);
            }
            return new Pagina(columnas, filas, filas.size() == FILAS_POR_PAGINA && resultado.next());
        }, FILAS_POR_PAGINA + 1, (pagina - 1L) * FILAS_POR_PAGINA);
    }
}
