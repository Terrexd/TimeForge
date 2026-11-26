package es.timeforge.configuracion;

import es.timeforge.modelo.repositorio.Repositorio;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(Repositorio repositorio) {
        return username -> {
            var a = repositorio.buscarUsuario(username.toLowerCase(java.util.Locale.ROOT));
            if (a == null)
                throw new UsernameNotFoundException("Credenciales incorrectas");
            return User.withUsername(a.username).password(a.passwordHash).roles(a.role)
                    .disabled(!a.active || a.deleted).build();
        };
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/bbdd", "/bbdd.html", "/bbdd.js", "/estilos.css", "/comun.js", "/pantallas.js",
                        "/formularios.js", "/eventos.js", "/favicon.svg", "/api/auth/csrf",
                        "/api/auth/register", "/api/auth/login", "/error")
                .permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated())
                .csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository())).formLogin(
                        form -> form.loginProcessingUrl("/api/auth/login").successHandler((req, res, a) -> {
                            res.setContentType("application/json");
                            res.getWriter().write("{\"ok\":true}");
                        }).failureHandler((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter()
                                    .write("{\"message\":\"Credenciales incorrectas o cuenta suspendida\"}");
                        }))
                .logout(out -> out.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, a) -> res.setStatus(204)))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"message\":\"Inicia sesión para continuar\"}");
                }).accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write(
                            "{\"message\":\"Operación no autorizada o sesión caducada. Recarga la página\"}");
                })).headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: https://cdn.akamai.steamstatic.com https://shared.akamai.steamstatic.com https://images.igdb.com https://images.metahub.space https://cdn.myanimelist.net https://*.anilist.co; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'")));
        return http.build();
    }
}
