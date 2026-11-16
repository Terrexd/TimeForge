package es.timeforge.service;

import es.timeforge.model.*;
import java.time.*;
import java.util.*;
import java.util.random.RandomGenerator;

public final class RoulettePolicy {
    private RoulettePolicy() {}
    public static int cost(int minutes) { return switch(minutes) { case 15->80; case 30->150; case 60->280; default->throw new BusinessException("Elige una pausa de 15, 30 o 60 minutos"); }; }
    public static boolean eligible(Progress p,int minutes) {
        return (p.state==ProgressState.PENDING || p.state==ProgressState.IN_PROGRESS)
            && p.work.minutesPerUnit<=minutes
            && (p.work.type!=MediaType.GAME || minutes==60 || p.work.quickSession);
    }
    public static double weight(Progress p,Instant now) {
        long days=Math.max(0,Duration.between(p.lastInteraction,now).toDays());
        double completion=p.work.totalUnits>0?Math.min(1,p.currentUnits/p.work.totalUnits):0;
        return 1+Math.min(days,60)/7.0+completion*3;
    }
    public static Progress choose(List<Progress> options,Instant now,RandomGenerator random) {
        BusinessException.require(!options.isEmpty(),"No hay obras compatibles con esta pausa en el catálogo");
        double target=random.nextDouble()*options.stream().mapToDouble(p->weight(p,now)).sum();
        for(var p:options) { target-=weight(p,now); if(target<0) return p; }
        return options.getLast();
    }
}
