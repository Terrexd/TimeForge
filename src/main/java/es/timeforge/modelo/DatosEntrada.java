package es.timeforge.modelo;

import es.timeforge.modelo.entidades.*;
import jakarta.validation.constraints.*;

public final class DatosEntrada {
    private DatosEntrada() {
    }

    public record Registro(@NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,40}") String username,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 10, max = 72) String password) {
    }

    public record Perfil(@NotBlank @Size(max = 80) String displayName, @NotBlank @Size(max = 8) String avatar,
            @Size(max = 250) String bio) {
    }

    public record DatosTarea(@NotBlank @Size(max = 150) String title, @Size(max = 2000) String description,
            @Pattern(regexp = "ESTUDIO|TRABAJO|PERSONAL") @NotNull String category,
            @Pattern(regexp = "ALTA|MEDIA|BAJA") @NotNull String priority,
            @Min(1) @Max(720) int estimatedMinutes) {
    }

    public record DatosObra(@NotBlank @Size(max = 150) String title, @NotNull TipoMedio type,
            @Size(max = 8000) String synopsis, @Size(max = 250) String genres,
            @Min(1) @Max(100000) int totalUnits, @Min(1) @Max(600) int minutesPerUnit, boolean quickSession) {
    }

    public record DatosProgreso(@NotNull EstadoProgreso state,
            @DecimalMin("0") @DecimalMax("100000") double currentUnits, @Min(0) @Max(10000) int volume,
            @Min(0) @Max(10000) int season, @Size(max = 150) String platform) {
    }

    public record DatosDescanso(int minutes, Long friendId) {
    }

    public record SolicitudAmistad(@NotBlank @Size(max = 40) String username) {
    }

    public record Decision(boolean accept) {
    }

    public record AccionAdministrativa(
            @Pattern(regexp = "SUSPEND|REACTIVATE|DELETE") @NotNull String action) {
    }

    public record DatosImportacion(@Pattern(regexp = "JIKAN_ANIME|JIKAN_MANGA|RAWG") @NotNull String source,
            @Min(1) long externalId) {
    }
}
