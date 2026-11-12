package br.com.clinica.financeiro.api;
import br.com.clinica.financeiro.application.FinanceiroService;
import br.com.clinica.financeiro.domain.Titulo;
import br.com.clinica.financeiro.domain.FormaPagamento;
import br.com.clinica.financeiro.infrastructure.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/titulos")
@Validated
public class TituloController {
    private final FinanceiroService service;
    private final TituloRepository repository;
    public TituloController(FinanceiroService service,TituloRepository repository) { this.service=service; this.repository=repository; }
    public record CriarTitulo(@NotNull Titulo.Tipo tipo,@NotBlank @Size(max=200) String descricao,
        UUID pacienteId,UUID atendimentoId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal valor,@NotNull LocalDate vencimento) {}
    public record CriarPagamento(@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal valor,
        @NotNull FormaPagamento forma) {}
    public record Resposta(UUID id,Titulo.Tipo tipo,Titulo.Status status,String descricao,UUID pacienteId,
        UUID atendimentoId,BigDecimal valor,BigDecimal liquidado,BigDecimal saldo,LocalDate vencimento,UUID compraEstoqueId,UUID fornecedorId,
        UUID medicoId,LocalDate dataCompetencia,Titulo.Categoria categoria,boolean custosInformados,BigDecimal percentualClinica) {
        public static Resposta de(Titulo t) { return new Resposta(t.id,t.tipo,t.status,t.descricao,t.pacienteId,t.atendimentoId,t.valor,t.liquidado,t.saldo(),t.vencimento,t.compraEstoqueId,t.fornecedorId,t.medicoId,t.dataCompetencia,t.categoria,t.custosInformados,t.percentualClinica); }
    }
    @PostMapping
    public ResponseEntity<Resposta> criar(@Valid @RequestBody CriarTitulo r) {
        var t=service.criar(new Titulo(r.tipo(),r.descricao(),r.pacienteId(),r.atendimentoId(),r.valor(),r.vencimento()));
        return ResponseEntity.created(URI.create("/api/v1/titulos/"+t.id)).body(Resposta.de(t));
    }
    @GetMapping("/{id}") public Resposta buscar(@PathVariable UUID id) { return Resposta.de(service.buscar(id)); }
    @GetMapping public Page<Resposta> listar(@RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return repository.findAll(PageRequest.of(page,size,Sort.by("vencimento","id"))).map(Resposta::de);
    }
    @PostMapping("/{id}/pagamentos")
    public Pagamento pagar(@PathVariable UUID id,@RequestHeader("Idempotency-Key") @NotBlank @Size(max=100) String chave,
        @Valid @RequestBody CriarPagamento r) { return service.pagar(id,chave,r.valor(),r.forma()); }
    @PostMapping("/{id}/cancelamento") public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        service.cancelar(id); return ResponseEntity.noContent().build();
    }
}
