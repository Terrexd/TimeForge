package es.timeforge.modelo.repositorio;

import es.timeforge.modelo.entidades.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Capa de acceso a datos (Patrón DAO / Repositorio).
 * Centraliza y encapsula todas las operaciones de persistencia y consultas JPQL parametrizadas.
 */
@Repository
public class Repositorio {

    @PersistenceContext
    private EntityManager em;

    public <T> T guardar(T value) {
        em.persist(value);
        return value;
    }

    public void eliminar(Object value) {
        em.remove(value);
    }

    public <T> T buscarPorId(Class<T> type, long id) {
        return em.find(type, id);
    }

    private <T> List<T> consultar(Class<T> type, String jpql, Object... params) {
        var q = em.createQuery(jpql, type);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q.getResultList();
    }

    public Usuario buscarUsuario(String username) {
        return consultar(Usuario.class, "from Usuario where username = ?1", username).stream().findFirst()
                .orElse(null);
    }

    public boolean existeCorreo(String email) {
        return !consultar(Usuario.class, "from Usuario where email = ?1", email).isEmpty();
    }

    public List<Usuario> buscarUsuarios(String search) {
        return em.createQuery(
                "from Usuario where deleted = false and (username like :q or displayName like :q) order by username",
                Usuario.class).setParameter("q", "%" + search + "%").setMaxResults(50).getResultList();
    }

    /** Bloqueo pesimista para serializar transacciones críticas de saldo y tiempo. */
    public Usuario bloquearUsuario(long id) {
        return em.find(Usuario.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public Billetera billetera(long user) {
        return consultar(Billetera.class, "from Billetera where owner.id = ?1", user).getFirst();
    }

    public List<Movimiento> movimientos(long user) {
        return em.createQuery("from Movimiento where wallet.owner.id = :u order by id desc", Movimiento.class)
                .setParameter("u", user).setMaxResults(100).getResultList();
    }

    public int monedasDesde(long user, Instant since) {
        return em.createQuery(
                "select coalesce(sum(amount), 0) from Movimiento where wallet.owner.id = ?1 and concept = 'TAREA' and createdAt >= ?2",
                Long.class).setParameter(1, user).setParameter(2, since).getSingleResult().intValue();
    }

    public List<Tarea> tareas(long user) {
        return consultar(Tarea.class, "from Tarea where owner.id = ?1 order by id desc", user);
    }

    public Tarea ultimaTarea(long user) {
        return em.createQuery(
                "from Tarea where owner.id = :u and state = es.timeforge.modelo.entidades.EstadoTarea.COMPLETED order by completedAt desc",
                Tarea.class).setParameter("u", user).setMaxResults(1).getResultStream().findFirst()
                .orElse(null);
    }

    public List<Progreso> catalogoPersonal(long user) {
        return consultar(Progreso.class,
                "from Progreso p join fetch p.work where p.owner.id = ?1 order by p.id desc", user);
    }

    public Obra buscarObra(String title, TipoMedio type) {
        return consultar(Obra.class, "from Obra where canonicalTitle = ?1 and type = ?2", title, type)
                .stream().findFirst().orElse(null);
    }

    public List<Descanso> descansos(long user) {
        return em.createQuery("from Descanso r join fetch r.work where r.owner.id = :u order by r.id desc",
                Descanso.class).setParameter("u", user).setMaxResults(100).getResultList();
    }

    public List<Descanso> descansosActivos(long user, Instant now) {
        return consultar(Descanso.class,
                "from Descanso r join fetch r.work where r.owner.id = ?1 and r.cancelled = false and r.completed = false and r.endsAt > ?2",
                user, now);
    }

    public Recomendacion recomendacion(long rest) {
        return consultar(Recomendacion.class, "from Recomendacion where rest.id = ?1", rest).stream()
                .findFirst().orElse(null);
    }

    public List<Mision> misiones(long user, LocalDate week) {
        return consultar(Mision.class,
                "from Mision m join fetch m.work where m.owner.id = ?1 and m.weekStart = ?2 order by m.id",
                user, week);
    }

    public List<Amistad> amistades(long user) {
        return consultar(Amistad.class, "from Amistad where lowId = ?1 or highId = ?1", user);
    }

    public Amistad amistad(long a, long b) {
        return consultar(Amistad.class, "from Amistad where lowId = ?1 and highId = ?2", Math.min(a, b),
                Math.max(a, b)).stream().findFirst().orElse(null);
    }

    public List<Insignia> insignias(long user) {
        return consultar(Insignia.class, "from Insignia where owner.id = ?1 order by id", user);
    }

    public List<InvitacionCooperativa> invitaciones(long user, Instant now) {
        return consultar(InvitacionCooperativa.class,
                "from InvitacionCooperativa where receiverId = ?1 and state = 'PENDING' and expiresAt > ?2",
                user, now);
    }

    public InvitacionCooperativa bloquearInvitacion(long id) {
        return em.find(InvitacionCooperativa.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<LlamadaIA> llamadasIA() {
        return em.createQuery("from LlamadaIA order by id desc", LlamadaIA.class).setMaxResults(200)
                .getResultList();
    }

    public Map<String, Object> metricasIA() {
        return Map.of("requests",
                em.createQuery("select count(a) from LlamadaIA a where outcome <> 'DISABLED'", Long.class)
                        .getSingleResult(),
                "errors",
                em.createQuery("select count(a) from LlamadaIA a where outcome = 'ERROR'", Long.class)
                        .getSingleResult(),
                "averageMs",
                Optional.ofNullable(
                        em.createQuery("select avg(durationMs) from LlamadaIA where outcome <> 'DISABLED'",
                                Double.class).getSingleResult())
                        .orElse(0.0),
                "inputTokens",
                em.createQuery("select coalesce(sum(inputTokens), 0) from LlamadaIA", Long.class)
                        .getSingleResult(),
                "outputTokens",
                em.createQuery("select coalesce(sum(outputTokens), 0) from LlamadaIA", Long.class)
                        .getSingleResult());
    }
}
