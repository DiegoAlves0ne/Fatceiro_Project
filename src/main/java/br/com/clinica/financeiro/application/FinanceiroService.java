package br.com.clinica.financeiro.application;
import br.com.clinica.financeiro.domain.Titulo;
import br.com.clinica.financeiro.domain.FormaPagamento;
import br.com.clinica.financeiro.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
@Service
@Transactional
public class FinanceiroService {
    private final TituloRepository titulos;
    private final PagamentoRepository pagamentos;
    public FinanceiroService(TituloRepository titulos,PagamentoRepository pagamentos) {
        this.titulos=titulos; this.pagamentos=pagamentos;
    }
    public Titulo criar(Titulo titulo) {
        if (titulo.tipo == Titulo.Tipo.PAGAR)
            throw new IllegalArgumentException("Contas a pagar devem ser geradas pela integração de compras do estoque");
        return titulos.save(titulo);
    }
    public Titulo registrarCompra(UUID compraId,UUID fornecedorId,String descricao,BigDecimal valor,java.time.LocalDate vencimento) {
        Objects.requireNonNull(compraId,"Compra obrigatória");
        Objects.requireNonNull(fornecedorId,"Fornecedor obrigatório");
        var candidato = new Titulo(Titulo.Tipo.PAGAR,descricao,null,null,valor,vencimento);
        var existente = titulos.findByCompraEstoqueId(compraId);
        if (existente.isPresent()) {
            var t = existente.get();
            if (!Objects.equals(t.fornecedorId,fornecedorId) || !t.descricao.equals(descricao)
                || t.valor.compareTo(valor)!=0 || !t.vencimento.equals(vencimento))
                throw new IllegalStateException("Compra já recebida com outros dados");
            return t;
        }
        candidato.compraEstoqueId=compraId;
        candidato.fornecedorId=fornecedorId;
        return titulos.save(candidato);
    }
    @Transactional(readOnly=true)
    public Titulo buscar(UUID id) { return titulos.findById(id).orElseThrow(() -> new NoSuchElementException("Título não encontrado")); }
    public Pagamento pagar(UUID id,String chave,BigDecimal valor,FormaPagamento forma) {
        if (forma == null || forma == FormaPagamento.NAO_INFORMADA)
            throw new IllegalArgumentException("Informe a forma de pagamento");
        var existente=pagamentos.findByChaveIdempotencia(chave);
        if (existente.isPresent()) {
            var p=existente.get();
            if (!p.tituloId.equals(id) || p.valor.compareTo(valor)!=0 || p.forma!=forma) throw new IllegalStateException("Chave já utilizada com outros dados");
            return p;
        }
        buscar(id).liquidar(valor);
        return pagamentos.save(new Pagamento(id,chave,valor,forma));
    }
    public void cancelar(UUID id) { buscar(id).cancelar(); }
}
