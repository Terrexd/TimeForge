package es.timeforge.modelo.servicios;

import es.timeforge.modelo.entidades.*;
import es.timeforge.modelo.repositorio.Repositorio;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static es.timeforge.modelo.servicios.ErrorNegocio.require;

@Service
public class ServicioBilletera {
    private final Repositorio repositorio;
    private final Clock clock;

    public ServicioBilletera(Repositorio repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    /** El llamante debe mantener el bloqueo del usuario y una transacción. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void move(long user, int amount, String concept) {
        var w = repositorio.billetera(user);
        require((long) w.balance + amount >= 0, "Saldo insuficiente. Completa tareas para ganar más monedas");
        require((long) w.balance + amount <= Integer.MAX_VALUE, "Límite de saldo alcanzado");
        w.balance += amount;
        w.updatedAt = clock.instant();
        var m = new Movimiento();
        m.wallet = w;
        m.amount = amount;
        m.concept = concept;
        m.createdAt = clock.instant();
        repositorio.guardar(m);
    }
}
