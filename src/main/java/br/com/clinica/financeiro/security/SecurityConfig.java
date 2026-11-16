package br.com.clinica.financeiro.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods=false)
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    JwtAuthenticationConverter keycloakAuthenticationConverter(
            @Value("${app.security.client-id:financeiro-api}") String clientId) {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakAuthoritiesConverter(clientId));
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        return http
            // Bearer tokens only: no cookies, login form or server-side session.
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/integracoes/estoque/compras")
                    .hasAnyRole("financeiro-admin", "integracao-estoque")
                .requestMatchers(HttpMethod.POST, "/api/v1/integracoes/atendimentos/faturamentos")
                    .hasAnyRole("financeiro-admin", "integracao-atendimento")
                .requestMatchers(HttpMethod.GET, "/api/v1/titulos", "/api/v1/titulos/**", "/api/v1/relatorios/**")
                    .hasAnyRole("financeiro-admin", "financeiro-leitura", "financeiro-operacao")
                .requestMatchers(HttpMethod.POST, "/api/v1/titulos", "/api/v1/titulos/*/pagamentos", "/api/v1/titulos/*/cancelamento")
                    .hasAnyRole("financeiro-admin", "financeiro-operacao")
                .requestMatchers(HttpMethod.GET, "/actuator/info").hasRole("financeiro-admin")
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
            .build();
    }
}
