package br.com.clinica.financeiro.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/** Real signed JWTs and the production security chain; no database or Keycloak required. */
class SecurityConfigTest {
    static final String ISSUER = "http://localhost:8081/realms/clinica";
    static RSAKey key;
    static HttpServer jwks;
    static AnnotationConfigWebApplicationContext context;
    static MockMvc mvc;

    @BeforeAll static void setup() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("test-key").generate();
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwks.createContext("/certs", exchange -> {
            byte[] body = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) { out.write(body); }
        });
        jwks.start();
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri", ISSUER,
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "http://127.0.0.1:" + jwks.getAddress().getPort() + "/certs",
            "spring.security.oauth2.resourceserver.jwt.audiences[0]", "financeiro-api")));
        context.register(SecurityConfig.class, OAuth2ResourceServerAutoConfiguration.class, Endpoints.class);
        context.refresh();
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll static void cleanup() {
        if (context != null) context.close();
        if (jwks != null) jwks.stop(0);
    }

    static String token(String issuer, String audience, Instant expires, Map<String, Object> claims, RSAKey signer) throws Exception {
        var builder = new JWTClaimsSet.Builder().subject("test-service").issuer(issuer)
            .audience(audience).issueTime(Date.from(Instant.now().minusSeconds(10)))
            .expirationTime(Date.from(expires));
        claims.forEach(builder::claim);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), builder.build());
        jwt.sign(new RSASSASigner(signer));
        return jwt.serialize();
    }

    static Map<String, Object> roles(String client, String... roles) {
        return Map.of("resource_access", Map.of(client, Map.of("roles", List.of(roles))));
    }

    static String token(String... roles) throws Exception {
        return token(ISSUER, "financeiro-api", Instant.now().plusSeconds(300), roles("financeiro-api", roles), key);
    }

    static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    @Test void exigeTokenEDeixaSomenteSaudePublica() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/titulos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/integracoes/estoque/compras")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
    }

    @Test void leituraNaoPodeAlterarFinanceiro() throws Exception {
        String jwt = token("financeiro-leitura");
        mvc.perform(bearer(get("/api/v1/titulos"), jwt)).andExpect(status().isOk());
        mvc.perform(bearer(get("/api/v1/titulos/123/custos"), jwt)).andExpect(status().isOk());
        mvc.perform(bearer(get("/api/v1/relatorios/faturamento"), jwt)).andExpect(status().isOk());
        mvc.perform(bearer(post("/api/v1/titulos"), jwt)).andExpect(status().isForbidden());
        mvc.perform(bearer(post("/api/v1/titulos/123/pagamentos"), jwt)).andExpect(status().isForbidden());
    }

    @Test void operadorPodeOperarMasNaoIntegrarNemAdministrar() throws Exception {
        String jwt = token("financeiro-operacao");
        for (String path : List.of("/api/v1/titulos", "/api/v1/titulos/123/pagamentos", "/api/v1/titulos/123/cancelamento"))
            mvc.perform(bearer(post(path), jwt)).andExpect(status().isOk());
        mvc.perform(bearer(post("/api/v1/integracoes/estoque/compras"), jwt)).andExpect(status().isForbidden());
        mvc.perform(bearer(get("/actuator/info"), jwt)).andExpect(status().isForbidden());
    }

    @Test void isolaAsIntegracoes() throws Exception {
        String estoque = token("integracao-estoque"), atendimento = token("integracao-atendimento");
        mvc.perform(bearer(post("/api/v1/integracoes/estoque/compras"), estoque)).andExpect(status().isOk());
        mvc.perform(bearer(post("/api/v1/integracoes/atendimentos/faturamentos"), estoque)).andExpect(status().isForbidden());
        mvc.perform(bearer(get("/api/v1/titulos"), estoque)).andExpect(status().isForbidden());
        mvc.perform(bearer(post("/api/v1/integracoes/atendimentos/faturamentos"), atendimento)).andExpect(status().isOk());
        mvc.perform(bearer(post("/api/v1/integracoes/estoque/compras"), atendimento)).andExpect(status().isForbidden());
    }

    @Test void adminPodeAcessarMasRotasNovasContinuamBloqueadas() throws Exception {
        String jwt = token("financeiro-admin");
        mvc.perform(bearer(get("/actuator/info"), jwt)).andExpect(status().isOk());
        mvc.perform(bearer(post("/api/v1/integracoes/estoque/compras"), jwt)).andExpect(status().isOk());
        mvc.perform(bearer(get("/rota-nova"), jwt)).andExpect(status().isForbidden());
    }

    @Test void rejeitaTokenSemRolesOuComRolesDeOutroCliente() throws Exception {
        for (Map<String, Object> claims : List.of(Map.<String, Object>of(), roles("outro-cliente", "financeiro-admin"),
                Map.<String, Object>of("realm_access", Map.of("roles", List.of("financeiro-admin"))),
                Map.<String, Object>of("resource_access", "malformado"))) {
            String jwt = token(ISSUER, "financeiro-api", Instant.now().plusSeconds(300), claims, key);
            mvc.perform(bearer(get("/api/v1/titulos"), jwt)).andExpect(status().isForbidden());
        }
    }

    @Test void rejeitaEmissorAudienceExpiracaoEAssinaturaInvalidos() throws Exception {
        var claims = roles("financeiro-api", "financeiro-admin");
        for (String jwt : List.of(
                token("http://outro-emissor", "financeiro-api", Instant.now().plusSeconds(300), claims, key),
                token(ISSUER, "outra-api", Instant.now().plusSeconds(300), claims, key),
                token(ISSUER, "financeiro-api", Instant.now().minusSeconds(300), claims, key),
                token(ISSUER, "financeiro-api", Instant.now().plusSeconds(300), claims, new RSAKeyGenerator(2048).generate()),
                "token-invalido")) {
            mvc.perform(bearer(get("/api/v1/titulos"), jwt)).andExpect(status().isUnauthorized());
        }
    }

    @Configuration(proxyBeanMethods=false)
    @EnableWebMvc
    static class Endpoints {
        @Bean Probe probe() { return new Probe(); }
    }

    @RestController
    static class Probe {
        @RequestMapping("/**") String ok() { return "ok"; }
    }
}
