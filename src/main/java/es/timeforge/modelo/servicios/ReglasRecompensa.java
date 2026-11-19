package es.timeforge.modelo.servicios;

/** RN-02: bloques completos del 5% de demora; redondeo final hacia abajo. */
public final class ReglasRecompensa {
    private ReglasRecompensa() {
    }

    public static int calcular(int minutosEstimados, long segundos, boolean cronometrada, int monedasDeHoy) {
        if (!cronometrada || segundos < 300 || minutosEstimados <= 0)
            return 0;
        long segundosEstimados = minutosEstimados * 60L;
        long retraso = Math.max(0, segundos - segundosEstimados);
        long bloques = retraso * 20 / segundosEstimados;
        long porcentaje = Math.max(20, 100 - 2 * bloques);
        int recompensa = (int) (minutosEstimados * 2L * porcentaje / 100);
        return Math.max(0, Math.min(recompensa, 1500 - monedasDeHoy));
    }
}
