package es.timeforge.repository;

import es.timeforge.model.*;
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
public class Store {

    @PersistenceContext
    private EntityManager em;

    public <T> T save(T value) {
        em.persist(value);
        return value;
    }

    public void remove(Object value) {
        em.remove(value);
    }

    public <T> T get(Class<T> type, long id) {
        return em.find(type, id);
    }

    private <T> List<T> query(Class<T> type, String jpql, Object... params) {
        var q = em.createQuery(jpql, type);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q.getResultList();
    }

    public Account account(String username) {
        return query(Account.class, "from Account where username = ?1", username)
                .stream().findFirst().orElse(null);
    }

    public boolean emailExists(String email) {
        return !query(Account.class, "from Account where email = ?1", email).isEmpty();
    }

    public List<Account> users(String search) {
        return em.createQuery("from Account where deleted = false and (username like :q or displayName like :q) order by username", Account.class)
                .setParameter("q", "%" + search + "%")
                .setMaxResults(50)
                .getResultList();
    }

    /** Bloqueo pesimista para serializar transacciones críticas de saldo y tiempo. */
    public Account lock(long id) {
        return em.find(Account.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public Wallet wallet(long user) {
        return query(Wallet.class, "from Wallet where owner.id = ?1", user).getFirst();
    }

    public List<Movement> movements(long user) {
        return em.createQuery("from Movement where wallet.owner.id = :u order by id desc", Movement.class)
                .setParameter("u", user)
                .setMaxResults(100)
                .getResultList();
    }

    public int earnedSince(long user, Instant since) {
        return em.createQuery("select coalesce(sum(amount), 0) from Movement where wallet.owner.id = ?1 and concept = 'TAREA' and createdAt >= ?2", Long.class)
                .setParameter(1, user)
                .setParameter(2, since)
                .getSingleResult().intValue();
    }

    public List<Task> tasks(long user) {
        return query(Task.class, "from Task where owner.id = ?1 order by id desc", user);
    }

    public Task lastTask(long user) {
        return em.createQuery("from Task where owner.id = :u and state = es.timeforge.model.TaskState.COMPLETED order by completedAt desc", Task.class)
                .setParameter("u", user)
                .setMaxResults(1)
                .getResultStream().findFirst().orElse(null);
    }

    public List<Progress> backlog(long user) {
        return query(Progress.class, "from Progress p join fetch p.work where p.owner.id = ?1 order by p.id desc", user);
    }

    public Work work(String title, MediaType type) {
        return query(Work.class, "from Work where canonicalTitle = ?1 and type = ?2", title, type)
                .stream().findFirst().orElse(null);
    }

    public List<Rest> rests(long user) {
        return em.createQuery("from Rest r join fetch r.work where r.owner.id = :u order by r.id desc", Rest.class)
                .setParameter("u", user)
                .setMaxResults(100)
                .getResultList();
    }

    public List<Rest> activeRests(long user, Instant now) {
        return query(Rest.class, "from Rest r join fetch r.work where r.owner.id = ?1 and r.cancelled = false and r.completed = false and r.endsAt > ?2", user, now);
    }

    public Recommendation recommendation(long rest) {
        return query(Recommendation.class, "from Recommendation where rest.id = ?1", rest)
                .stream().findFirst().orElse(null);
    }

    public List<Mission> missions(long user, LocalDate week) {
        return query(Mission.class, "from Mission m join fetch m.work where m.owner.id = ?1 and m.weekStart = ?2 order by m.id", user, week);
    }

    public List<Friendship> friends(long user) {
        return query(Friendship.class, "from Friendship where lowId = ?1 or highId = ?1", user);
    }

    public Friendship friendship(long a, long b) {
        return query(Friendship.class, "from Friendship where lowId = ?1 and highId = ?2", Math.min(a, b), Math.max(a, b))
                .stream().findFirst().orElse(null);
    }

    public List<Badge> badges(long user) {
        return query(Badge.class, "from Badge where owner.id = ?1 order by id", user);
    }

    public List<CoopInvite> invitations(long user, Instant now) {
        return query(CoopInvite.class, "from CoopInvite where receiverId = ?1 and state = 'PENDING' and expiresAt > ?2", user, now);
    }

    public CoopInvite lockInvitation(long id) {
        return em.find(CoopInvite.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    public List<AiCall> aiCalls() {
        return em.createQuery("from AiCall order by id desc", AiCall.class)
                .setMaxResults(200)
                .getResultList();
    }

    public Map<String, Object> aiMetrics() {
        return Map.of(
                "requests", em.createQuery("select count(a) from AiCall a where outcome <> 'DISABLED'", Long.class).getSingleResult(),
                "errors", em.createQuery("select count(a) from AiCall a where outcome = 'ERROR'", Long.class).getSingleResult(),
                "averageMs", Optional.ofNullable(em.createQuery("select avg(durationMs) from AiCall where outcome <> 'DISABLED'", Double.class).getSingleResult()).orElse(0.0),
                "inputTokens", em.createQuery("select coalesce(sum(inputTokens), 0) from AiCall", Long.class).getSingleResult(),
                "outputTokens", em.createQuery("select coalesce(sum(outputTokens), 0) from AiCall", Long.class).getSingleResult()
        );
    }
}
