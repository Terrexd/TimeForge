package es.timeforge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.*;

/** Entidades persistentes. Las asociaciones nunca se exponen como grafos recursivos. */
public final class Domain {
    private Domain() {}
    public enum TaskState { PENDING, RUNNING, PAUSED, COMPLETED }
    public enum MediaType { GAME, MANGA, COMIC, SERIES, ANIME, BOOK, MOVIE }
    public enum ProgressState { PENDING, IN_PROGRESS, PAUSED, COMPLETED }
    public enum FriendshipState { PENDING, ACCEPTED, REJECTED }
    @MappedSuperclass public static abstract class EntityBase {
        @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    }
    @Entity(name="Account") @Table(name="tf_user") public static class Account extends EntityBase {
        @Column(nullable=false, unique=true, length=40) public String username;
        @Column(nullable=false, unique=true, length=160) public String email;
        @JsonIgnore @Column(nullable=false) public String passwordHash;
        public String displayName="", avatar="⚒", bio="", role="USER";
        public boolean active=true, deleted=false;
        public int xp=0, streak=0;
        public LocalDate lastActiveDay;
        public Instant createdAt;
    }
    @Entity(name="Wallet") @Table(name="wallet") public static class Wallet extends EntityBase {
        @OneToOne(optional=false) @JoinColumn(unique=true) @JsonIgnore public Account owner;
        public int balance;
        public Instant updatedAt;
    }
    @Entity(name="Movement") @Table(name="wallet_movement") public static class Movement extends EntityBase {
        @ManyToOne(optional=false) @JsonIgnore public Wallet wallet;
        public String concept;
        public int amount;
        public Instant createdAt;
    }
    @Entity(name="Task") @Table(name="task", indexes=@Index(columnList="owner_id,state")) public static class Task extends EntityBase {
        @ManyToOne(optional=false) @JsonIgnore public Account owner;
        public String title;
        @Column(length=2000) public String description="";
        public String category, priority;
        public int estimatedMinutes, coins;
        public long elapsedSeconds;
        public boolean timed;
        @Enumerated(EnumType.STRING) public TaskState state=TaskState.PENDING;
        public Instant startedAt, createdAt, completedAt;
    }
    @Entity(name="Work") @Table(name="media_work", uniqueConstraints=@UniqueConstraint(columnNames={"canonicalTitle","type"})) public static class Work extends EntityBase {
        public String title, canonicalTitle;
        @Enumerated(EnumType.STRING) public MediaType type;
        @Column(length=8000) public String synopsis="";
        public String genres="", tags="", pace="Sin clasificar", externalId="", source="MANUAL";
        public int totalUnits, minutesPerUnit;
        public boolean quickSession;
        @Lob @JsonIgnore public byte[] cover;
        public String coverType;
    }
    @Entity(name="Progress") @Table(name="media_progress", uniqueConstraints=@UniqueConstraint(columnNames={"owner_id","work_id"})) public static class Progress extends EntityBase {
        @ManyToOne(optional=false) @JsonIgnore public Account owner;
        @ManyToOne(optional=false) public Work work;
        @Enumerated(EnumType.STRING) public ProgressState state=ProgressState.PENDING;
        public double currentUnits;
        public int volume, season;
        public String platform="";
        public Instant lastInteraction;
    }
    @Entity(name="Rest") @Table(name="rest_session") public static class Rest extends EntityBase {
        @ManyToOne(optional=false) @JsonIgnore public Account owner;
        @ManyToOne(optional=false) public Work work;
        public int minutes, cost;
        public Instant startedAt, endsAt;
        public boolean completed, cancelled;
        public Long partnerId;
    }
    @Entity(name="Recommendation") @Table(name="ai_recommendation") public static class Recommendation extends EntityBase {
        @OneToOne(optional=false) @JoinColumn(unique=true) @JsonIgnore public Rest rest;
        @ManyToOne @JsonIgnore public Task previousTask;
        @Column(length=4000) public String text;
        public String source="FALLBACK";
        public Instant createdAt;
    }
    @Entity(name="Mission") @Table(name="mission", uniqueConstraints=@UniqueConstraint(columnNames={"owner_id","work_id","weekStart"})) public static class Mission extends EntityBase {
        @ManyToOne(optional=false) @JsonIgnore public Account owner;
        @ManyToOne(optional=false) public Work work;
        public String description;
        public double baseline, target, progress;
        public int reward=40;
        public boolean claimed;
        public LocalDate weekStart;
        public Instant expiresAt;
    }
    @Entity(name="Friendship") @Table(name="friendship", uniqueConstraints=@UniqueConstraint(columnNames={"lowId","highId"})) public static class Friendship extends EntityBase {
        public long lowId, highId, senderId, receiverId;
        @Enumerated(EnumType.STRING) public FriendshipState state=FriendshipState.PENDING;
        public Instant createdAt;
    }
    @Entity(name="Badge") @Table(name="badge", uniqueConstraints=@UniqueConstraint(columnNames={"owner_id","code"})) public static class Badge extends EntityBase {
        @ManyToOne(optional=false) @JsonIgnore public Account owner;
        public String code, title;
        public Instant createdAt;
    }
    @Entity(name="CoopInvite") @Table(name="coop_invite") public static class CoopInvite extends EntityBase {
        public long senderId, receiverId;
        public String senderName, state="PENDING";
        public int minutes, cost;
        public Instant expiresAt;
    }
    @Entity(name="AiCall") @Table(name="ai_call") public static class AiCall extends EntityBase {
        public String operation, outcome;
        public long durationMs;
        public int inputTokens, outputTokens;
        public Instant createdAt;
    }
}
