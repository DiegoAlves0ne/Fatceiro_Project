package br.com.clinica.financeiro.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface PagamentoRepository extends JpaRepository<Pagamento,UUID> {
    Optional<Pagamento> findByChaveIdempotencia(String chave);
    org.springframework.data.domain.Page<Pagamento> findByTituloId(UUID tituloId,org.springframework.data.domain.Pageable pageable);
}
