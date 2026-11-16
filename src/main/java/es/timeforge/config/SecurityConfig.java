package es.timeforge.config;

import es.timeforge.repository.Store;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder(12);}
    @Bean UserDetailsService userDetailsService(Store store) {return username->{var a=store.account(username.toLowerCase(java.util.Locale.ROOT));if(a==null)throw new UsernameNotFoundException("Credenciales incorrectas");return User.withUsername(a.username).password(a.passwordHash).roles(a.role).disabled(!a.active||a.deleted).build();};}
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth->auth.requestMatchers("/","/index.html","/style.css","/app.js","/favicon.svg","/api/auth/csrf","/api/auth/register","/api/auth/login","/error").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
            .csrf(csrf->csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
            .formLogin(form->form.loginProcessingUrl("/api/auth/login").successHandler((req,res,a)->{res.setContentType("application/json");res.getWriter().write("{\"ok\":true}");}).failureHandler((req,res,e)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Credenciales incorrectas o cuenta suspendida\"}");}))
            .logout(out->out.logoutUrl("/api/auth/logout").logoutSuccessHandler((req,res,a)->res.setStatus(204)))
            .exceptionHandling(errors->errors.authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Inicia sesión para continuar\"}");}).accessDeniedHandler((req,res,e)->{res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"Operación no autorizada o sesión caducada. Recarga la página\"}");}))
            .headers(headers->headers.contentSecurityPolicy(csp->csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'")));
        return http.build();
    }
}
