package br.com.clinica.financeiro.api;
import br.com.clinica.financeiro.application.FinanceiroService;
import br.com.clinica.financeiro.infrastructure.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class CustoAtendimentoController {
    private final FinanceiroService financeiro;
    private final CustoItemRepository custos;
    public CustoAtendimentoController(FinanceiroService financeiro,CustoItemRepository custos) { this.financeiro=financeiro; this.custos=custos; }
    @GetMapping("/api/v1/titulos/{id}/custos")
    public List<CustoItem> listar(@PathVariable UUID id) {
        financeiro.buscar(id);
        return custos.findByTituloIdOrderByItemEstoqueId(id);
    }
}
