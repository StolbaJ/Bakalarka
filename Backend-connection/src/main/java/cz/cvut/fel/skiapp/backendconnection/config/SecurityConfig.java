package cz.cvut.fel.skiapp.backendconnection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Pro REST API vypnuto
                .cors(AbstractHttpConfigurer::disable) // Nastav dle potřeby frontendu
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Přihlašovací endpointy tvého backendu (pokud máš vlastní auth)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Veškerá ostatní API komunikace vyžaduje autorizaci
                        // (Protože stroj už tebe nevolá, nemusíme mu nic povolovat)
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}