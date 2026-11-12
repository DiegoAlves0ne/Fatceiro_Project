package br.com.clinica.financeiro.api;

import br.com.clinica.financeiro.application.FinanceiroService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integracoes/estoque/compras")
public class CompraEstoqueController {
    private final FinanceiroService service;
    public CompraEstoqueController(FinanceiroService service) { this.service=service; }
    public record Compra(@NotNull UUID compraId,@NotNull UUID fornecedorId,
        @NotBlank @Size(max=200) String descricao,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal valorTotal,
        @NotNull LocalDate vencimento) {}
    @PostMapping
    public TituloController.Resposta registrar(@Valid @RequestBody Compra compra) {
        return TituloController.Resposta.de(service.registrarCompra(compra.compraId(),compra.fornecedorId(),
            compra.descricao(),compra.valorTotal(),compra.vencimento()));
    }
}
