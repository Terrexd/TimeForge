package es.timeforge.config;

import es.timeforge.repository.Store;
import es.timeforge.service.AccountService;
import es.timeforge.controller.Inputs.Register;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final Store store;private final AccountService accounts;private final String username,password;
    public BootstrapAdmin(Store s,AccountService a,@Value("${timeforge.admin-user}")String u,@Value("${timeforge.admin-password}")String p){store=s;accounts=a;username=u;password=p;}
    @Override @Transactional public void run(ApplicationArguments args) {
        if(username.isBlank()||password.isBlank())return;
        if(!username.matches("[a-zA-Z0-9_]{3,40}")||password.length()<10)throw new IllegalStateException("ADMIN_USER o ADMIN_PASSWORD no válidos");
        // Nunca eleva automáticamente una cuenta registrada previamente.
        if(store.account(username.toLowerCase(java.util.Locale.ROOT))==null) {var a=accounts.register(new Register(username,username+"@timeforge.local",password));a.role="ADMIN";}
    }
}
