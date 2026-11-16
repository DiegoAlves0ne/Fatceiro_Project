package br.com.clinica.financeiro.domain.contas;

import br.com.clinica.financeiro.domain.Titulo;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModeloContasMappingTest {
    @Test void mapeiaRelacionamentosSemConflitarComPagamentoLegado() {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl")
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        try {
            var sources = new MetadataSources(registry);
            for (var type : new Class<?>[]{ContaReceber.class, ItemContaReceber.class,
                    Recebimento.class, AplicacaoRecebimento.class, ContaPagar.class,
                    ItemContaPagar.class, Pagamento.class, AplicacaoPagamento.class,
                    FaturaConvenio.class, ItemFaturaConvenio.class, Titulo.class,
                    br.com.clinica.financeiro.infrastructure.Pagamento.class}) {
                sources.addAnnotatedClass(type);
            }
            var metadata = (MetadataImplementor) sources.buildMetadata();
            metadata.validate();
            assertEquals(12, metadata.getEntityBindings().size());
            assertEquals("pagamento_conta", metadata.getEntityBinding(Pagamento.class.getName()).getTable().getName());
            assertEquals("pagamento", metadata.getEntityBinding(
                    br.com.clinica.financeiro.infrastructure.Pagamento.class.getName()).getTable().getName());
            var item = metadata.getEntityBinding(ItemFaturaConvenio.class.getName());
            assertNotNull(item.getProperty("itemContaReceber"));
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
