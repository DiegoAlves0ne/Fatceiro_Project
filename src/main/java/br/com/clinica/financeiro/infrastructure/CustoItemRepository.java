package br.com.clinica.financeiro.infrastructure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CustoItemRepository extends JpaRepository<CustoItem,UUID> {
    List<CustoItem> findByTituloIdOrderByItemEstoqueId(UUID tituloId);
}
