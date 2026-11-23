package es.timeforge.modelo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Estimaciones de HowLongToBeat por título. Los tiempos se expresan en horas. */
public final class HistoriasPrincipales {
    private static final Map<String, Double> HORAS = cargar();

    private HistoriasPrincipales() {}

    public static Double horas(String titulo) {
        return titulo == null ? null : HORAS.get(normalizar(titulo));
    }

    public static String normalizar(String titulo) {
        return titulo.toLowerCase(Locale.ROOT).replace("™", "").replace("®", "")
                .replace('’', '\'').replace(':', ' ').replaceAll("\\s+", " ").strip();
    }

    private static Map<String, Double> cargar() {
        Map<String, Double> horas = new HashMap<>();
        Set<String> ambiguos = new HashSet<>();
        var archivo = HistoriasPrincipales.class.getResourceAsStream("/historias-principales.tsv");
        try (var lector = new BufferedReader(new InputStreamReader(Objects.requireNonNull(archivo), StandardCharsets.UTF_8))) {
            for (String fila; (fila = lector.readLine()) != null;) {
                int separador = fila.lastIndexOf('\t');
                String titulo = normalizar(fila.substring(0, separador));
                double valor = Double.parseDouble(fila.substring(separador + 1));
                if (horas.put(titulo, Math.max(0.1, Math.round(valor * 10) / 10.0)) != null)
                    ambiguos.add(titulo);
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        ambiguos.forEach(horas::remove);
        return horas;
    }
}
