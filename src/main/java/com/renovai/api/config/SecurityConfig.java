package com.renovai.api.config;

import com.renovai.api.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Admin Site — acesso total
                .requestMatchers("/admin/**").hasRole("ADMIN_SITE")

                // Admin Cooperativa
                .requestMatchers("/cooperativas/**").hasAnyRole("ADMIN_SITE", "ADMIN_COOPERATIVA")

                // Gestor pode gerenciar pedidos, estoque, rateio
                .requestMatchers("/pedidos/**").hasAnyRole("ADMIN_SITE", "ADMIN_COOPERATIVA", "GESTOR_COOPERATIVA", "GESTOR_EMPRESA")
                .requestMatchers("/estoques/**").hasAnyRole("ADMIN_SITE", "ADMIN_COOPERATIVA", "GESTOR_COOPERATIVA")
                .requestMatchers("/rateios/**").hasAnyRole("ADMIN_SITE", "GESTOR_COOPERATIVA")

                // Funcionário pode registrar coletas e triagens
                .requestMatchers("/coletas/**").hasAnyRole("ADMIN_SITE", "GESTOR_COOPERATIVA", "FUNCIONARIO_COOPERATIVA")
                .requestMatchers("/triagens/**").hasAnyRole("ADMIN_SITE", "GESTOR_COOPERATIVA", "FUNCIONARIO_COOPERATIVA")

                // Consultas públicas (GET) para empresas
                .requestMatchers(HttpMethod.GET, "/materiais/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/empresas/**").authenticated()

                // Restante requer autenticação
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
