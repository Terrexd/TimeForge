package es.timeforge.service;

import es.timeforge.model.Domain.*;
import es.timeforge.repository.Store;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static es.timeforge.service.BusinessException.require;

@Service
public class WalletService {
    private final Store store;private final Clock clock;
    public WalletService(Store store,Clock clock) { this.store=store;this.clock=clock; }
    /** El llamante debe mantener el bloqueo del usuario y una transacción. */
    @Transactional(propagation=Propagation.MANDATORY)
    public void move(long user,int amount,String concept) {
        var w=store.wallet(user);require((long)w.balance+amount>=0,"Saldo insuficiente. Completa tareas para ganar más monedas");
        require((long)w.balance+amount<=Integer.MAX_VALUE,"Límite de saldo alcanzado");
        w.balance+=amount;w.updatedAt=clock.instant();
        var m=new Movement();m.wallet=w;m.amount=amount;m.concept=concept;m.createdAt=clock.instant();store.save(m);
    }
}
