package br.com.clinica.financeiro.api;

import br.com.clinica.financeiro.application.FaturamentoAtendimentoService;
import br.com.clinica.financeiro.domain.Titulo;
import br.com.clinica.financeiro.infrastructure.CustoItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/integracoes/atendimentos/faturamentos")
public class FaturamentoAtendimentoController {
    private final FaturamentoAtendimentoService service;
    public FaturamentoAtendimentoController(FaturamentoAtendimentoService service) { this.service=service; }
    public record Item(@NotNull UUID itemEstoqueId,@NotBlank @Size(max=200) String descricao,
        @NotNull @DecimalMin("0.001") @Digits(integer=9,fraction=3) BigDecimal quantidade,
        @NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal custoUnitario) {}
    public record Faturamento(@NotNull UUID atendimentoId,UUID medicoId,@NotNull UUID pacienteId,
        @NotNull Titulo.Categoria categoria,@NotBlank @Size(max=200) String descricao,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal valor,
        @NotNull @PastOrPresent LocalDate dataCompetencia,@NotNull LocalDate vencimento,
        @NotNull @Size(max=500) List<@NotNull @Valid Item> itens) {}
    public record Resposta(TituloController.Resposta titulo,List<CustoItem> itens) {}
    @PostMapping
    public Resposta registrar(@Valid @RequestBody Faturamento request) { return service.registrar(request); }
}
