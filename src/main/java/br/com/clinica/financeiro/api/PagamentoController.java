package br.com.clinica.financeiro.api;

import br.com.clinica.financeiro.application.FinanceiroService;
import br.com.clinica.financeiro.infrastructure.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/titulos/{id}/pagamentos")
public class PagamentoController {
    private final FinanceiroService service;
    private final PagamentoRepository pagamentos;
    public PagamentoController(FinanceiroService service,PagamentoRepository pagamentos) {
        this.service=service; this.pagamentos=pagamentos;
    }
    @GetMapping
    public Page<Pagamento> listar(@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        service.buscar(id);
        return pagamentos.findByTituloId(id,PageRequest.of(page,size,Sort.by("criadoEm","id")));
    }
}
