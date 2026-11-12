package br.com.clinica.financeiro.infrastructure;
import br.com.clinica.financeiro.domain.Titulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface TituloRepository extends JpaRepository<Titulo,UUID> {
    java.util.Optional<Titulo> findByCompraEstoqueId(UUID compraEstoqueId);
    java.util.Optional<Titulo> findByAtendimentoIdAndCategoriaIsNotNull(UUID atendimentoId);
}
