package br.com.clinica.financeiro.application;

import br.com.clinica.financeiro.api.FaturamentoAtendimentoController.*;
import br.com.clinica.financeiro.api.TituloController;
import br.com.clinica.financeiro.domain.Titulo;
import br.com.clinica.financeiro.infrastructure.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class FaturamentoAtendimentoService {
    private final TituloRepository titulos;
    private final CustoItemRepository custos;
    public FaturamentoAtendimentoService(TituloRepository titulos,CustoItemRepository custos) { this.titulos=titulos; this.custos=custos; }
    @Transactional
    public Resposta registrar(Faturamento r) {
        if (r.categoria()==Titulo.Categoria.CONSULTA && r.medicoId()==null)
            throw new IllegalArgumentException("Consulta exige médico");
        if (r.categoria()==Titulo.Categoria.EXAME && r.medicoId()!=null)
            throw new IllegalArgumentException("Exame próprio da clínica não deve informar médico");
        if (r.itens().stream().map(Item::itemEstoqueId).distinct().count()!=r.itens().size())
            throw new IllegalArgumentException("Agrupe quantidades do mesmo item em uma única linha");
        var existente=titulos.findByAtendimentoIdAndCategoriaIsNotNull(r.atendimentoId());
        if (existente.isPresent()) {
            var t=existente.get();
            var itens=custos.findByTituloIdOrderByItemEstoqueId(t.id);
            boolean iguais=Objects.equals(t.medicoId,r.medicoId()) && Objects.equals(t.pacienteId,r.pacienteId())
                && t.categoria==r.categoria() && t.descricao.equals(r.descricao()) && t.valor.compareTo(r.valor())==0
                && t.dataCompetencia.equals(r.dataCompetencia()) && t.vencimento.equals(r.vencimento())
                && itens.size()==r.itens().size() && r.itens().stream().allMatch(i -> itens.stream().anyMatch(c ->
                    c.itemEstoqueId.equals(i.itemEstoqueId()) && c.descricao.equals(i.descricao())
                    && c.quantidade.compareTo(i.quantidade())==0 && c.custoUnitario.compareTo(i.custoUnitario())==0));
            if (!iguais) throw new IllegalStateException("Atendimento já faturado com outros dados");
            return new Resposta(TituloController.Resposta.de(t),itens);
        }
        var t=new Titulo(Titulo.Tipo.RECEBER,r.descricao(),r.pacienteId(),r.atendimentoId(),r.valor(),r.vencimento());
        t.medicoId=r.medicoId(); t.dataCompetencia=r.dataCompetencia(); t.categoria=r.categoria(); t.custosInformados=true;
        t.percentualClinica=r.categoria()==Titulo.Categoria.CONSULTA ? new java.math.BigDecimal("0.3000") : new java.math.BigDecimal("0.0000");
        titulos.saveAndFlush(t);
        var itens=r.itens().stream().map(i -> new CustoItem(t.id,i.itemEstoqueId(),i.descricao(),i.quantidade(),i.custoUnitario())).toList();
        custos.saveAll(itens);
        return new Resposta(TituloController.Resposta.de(t),itens);
    }
}
