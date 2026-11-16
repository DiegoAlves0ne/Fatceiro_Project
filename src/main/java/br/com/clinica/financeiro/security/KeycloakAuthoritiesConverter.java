package br.com.clinica.financeiro.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;

/** Only roles belonging to this API grant access to financial data. */
public class KeycloakAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private static final Set<String> ROLES = Set.of(
        "financeiro-admin", "financeiro-leitura", "financeiro-operacao", "integracao-estoque", "integracao-atendimento");
    private final String clientId;

    public KeycloakAuthoritiesConverter(String clientId) { this.clientId = clientId; }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object access = jwt.getClaim("resource_access");
        if (!(access instanceof Map<?, ?> clients)
            || !(clients.get(clientId) instanceof Map<?, ?> client)
            || !(client.get("roles") instanceof Collection<?> roles)) return List.of();
        return roles.stream().filter(String.class::isInstance).map(String.class::cast)
            .filter(ROLES::contains).distinct()
            .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }
}
