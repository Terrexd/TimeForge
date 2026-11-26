package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import es.timeforge.modelo.DatosEntrada.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
@Transactional
public class ServicioCatalogo {
    private final Repositorio repositorio;
    private final ServicioUsuarios usuarios;
    private final ServicioMisiones misiones;
    private final ServicioLogros logros;
    private final Clock reloj;
    private final RecursosUsuario recursos;

    public ServicioCatalogo(Repositorio repositorio, ServicioUsuarios usuarios, ServicioMisiones misiones,
            ServicioLogros logros, Clock reloj, RecursosUsuario recursos) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.misiones = misiones;
        this.logros = logros;
        this.reloj = reloj;
        this.recursos = recursos;
    }

    public Progreso agregarObra(long idUsuario, DatosObra datos) {
        return guardarObraEnBiblioteca(idUsuario, datos, "MANUAL", "", null, null);
    }

    public Progreso guardarObraEnBiblioteca(long idUsuario, DatosObra datos, String fuente, String idExterno, byte[] portada,
            String tipoPortada) {
        return guardarObraEnBiblioteca(idUsuario, datos, fuente, idExterno, portada, tipoPortada, null);
    }

    public Progreso guardarObraEnBiblioteca(long idUsuario, DatosObra datos, String fuente, String idExterno, byte[] portada,
            String tipoPortada, Double horasCampana) {
        var usuario = usuarios.locked(idUsuario);
        String tituloNormalizado = datos.title().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        boolean externa = !"MANUAL".equals(fuente);
        Obra obra = externa ? repositorio.buscarObraExterna(fuente, idExterno) : null;
        if (obra == null) {
            obra = repositorio.buscarObra(tituloNormalizado, datos.type());
            if (externa && obra != null && fuente.equals(obra.source) && !idExterno.equals(obra.externalId)) {
                tituloNormalizado += " [" + fuente.toLowerCase(Locale.ROOT) + " " + idExterno + "]";
                obra = null;
            }
        }
        if (obra == null) {
            obra = new Obra();
            obra.title = datos.title().trim();
            obra.canonicalTitle = tituloNormalizado;
            obra.type = datos.type();
            obra.synopsis = datos.synopsis() == null ? "" : datos.synopsis();
            obra.genres = datos.genres() == null ? "" : datos.genres();
            obra.totalUnits = datos.totalUnits();
            obra.minutesPerUnit = datos.minutesPerUnit();
            obra.quickSession = datos.quickSession();
            obra.source = fuente;
            obra.externalId = idExterno;
            obra.cover = portada;
            obra.coverType = tipoPortada;
            repositorio.guardar(obra);
        } else if (!fuente.equals("MANUAL") && ("IGDB".equals(fuente) || !"IGDB".equals(obra.source))) {
            obra.synopsis = datos.synopsis();
            obra.genres = datos.genres();
            obra.source = fuente;
            obra.externalId = idExterno;
            if (portada != null) {
                obra.cover = portada;
                obra.coverType = tipoPortada;
            }
        }
        // La ficha es compartida. Cada usuario guarda su propia entrada de progreso.
        long idObra = obra.id;
        if (horasCampana != null) obra.horasCampanaImportadas = horasCampana;
        var existente = repositorio.catalogoPersonal(idUsuario).stream().filter(progreso -> progreso.work.id == idObra).findFirst();
        if (externa && existente.isPresent()) return existente.get();
        require(existente.isEmpty(),
                "Esta obra ya existe en tu catálogo. Edita su estado o progreso desde tu lista");
        var progreso = new Progreso();
        progreso.owner = usuario;
        progreso.work = obra;
        progreso.lastInteraction = reloj.instant();
        repositorio.guardar(progreso);
        misiones.generate(idUsuario);
        return progreso;
    }

    public Progreso actualizarProgreso(long idUsuario, long id, DatosProgreso datos) {
        var usuario = usuarios.locked(idUsuario);
        var progreso = buscarProgresoDelUsuario(idUsuario, id);
        boolean juego = progreso.work.type == TipoMedio.GAME;
        require(Double.isFinite(datos.currentUnits()) && (juego || datos.currentUnits() <= progreso.work.totalUnits),
                "El progreso actual no puede superar el total de la obra");
        require(juego || datos.state() != EstadoProgreso.COMPLETED || datos.currentUnits() == progreso.work.totalUnits,
                "Para completar la obra debes alcanzar su total de unidades");
        progreso.state = datos.state();
        progreso.currentUnits = datos.currentUnits();
        progreso.volume = datos.volume();
        progreso.season = datos.season();
        progreso.platform = datos.platform() == null ? "" : datos.platform();
        return registrarProgreso(usuario, progreso);
    }

    public Progreso ajustarCapitulo(long idUsuario, long id, int paso) {
        require(paso == 1 || paso == -1, "Solo puedes sumar o restar una unidad");
        var usuario = usuarios.locked(idUsuario);
        var progreso = buscarProgresoDelUsuario(idUsuario, id);
        require(progreso.work.type != TipoMedio.GAME && progreso.work.type != TipoMedio.MOVIE,
                "Esta obra no se registra por capítulos o episodios");
        double cantidad = Math.clamp(progreso.currentUnits + paso, 0, progreso.work.totalUnits);
        if (cantidad == progreso.currentUnits) return progreso;
        progreso.currentUnits = cantidad;
        if (cantidad == progreso.work.totalUnits) progreso.state = EstadoProgreso.COMPLETED;
        else if (cantidad == 0) progreso.state = EstadoProgreso.PENDING;
        else if (progreso.state != EstadoProgreso.PAUSED) progreso.state = EstadoProgreso.IN_PROGRESS;
        return registrarProgreso(usuario, progreso);
    }

    private Progreso registrarProgreso(Usuario usuario, Progreso progreso) {
        progreso.lastInteraction = reloj.instant();
        misiones.update(usuario.id, progreso);
        logros.evaluate(usuario);
        return progreso;
    }

    public Progreso buscarProgresoDelUsuario(long idUsuario, long id) {
        return recursos.progress(idUsuario, id);
    }

    public void eliminarObra(long idUsuario, long id) {
        usuarios.locked(idUsuario);
        var progreso = buscarProgresoDelUsuario(idUsuario, id);
        require(repositorio.descansosActivos(idUsuario, reloj.instant()).stream().noneMatch(
                descanso -> descanso.work.id.equals(progreso.work.id)), "No puedes retirar la obra de un descanso activo");
        repositorio.eliminar(progreso);
    }
}
