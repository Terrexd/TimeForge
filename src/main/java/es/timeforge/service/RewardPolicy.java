package es.timeforge.service;

/** RN-02: bloques completos del 5% de demora; redondeo final hacia abajo. */
public final class RewardPolicy {
    private RewardPolicy() {}
    public static int calculate(int estimatedMinutes,long seconds,boolean timed,int earnedToday) {
        if(!timed || seconds<300 || estimatedMinutes<=0) return 0;
        long estimatedSeconds=estimatedMinutes*60L;
        long excess=Math.max(0,seconds-estimatedSeconds);
        long blocks=excess*20/estimatedSeconds;
        long percent=Math.max(20,100-2*blocks);
        int reward=(int)(estimatedMinutes*2L*percent/100);
        return Math.max(0,Math.min(reward,1500-earnedToday));
    }
}
