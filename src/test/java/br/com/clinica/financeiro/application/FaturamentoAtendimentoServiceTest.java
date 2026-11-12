package br.com.clinica.financeiro.application;

import br.com.clinica.financeiro.api.FaturamentoAtendimentoController.*;
import br.com.clinica.financeiro.domain.Titulo;
import br.com.clinica.financeiro.infrastructure.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FaturamentoAtendimentoServiceTest {
    private final TituloRepository titulos=mock(TituloRepository.class);
    private final CustoItemRepository custos=mock(CustoItemRepository.class);
    private final FaturamentoAtendimentoService service=new FaturamentoAtendimentoService(titulos,custos);
    private Faturamento request(List<Item> itens) {
        return new Faturamento(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),Titulo.Categoria.CONSULTA,
            "Consulta",new BigDecimal("150.00"),LocalDate.now(),LocalDate.now(),itens);
    }
    @Test void registraCustoHistoricoESemDuplicacaoNoReenvio() {
        var r=request(List.of(new Item(UUID.randomUUID(),"Material",new BigDecimal("2.500"),new BigDecimal("3.15"))));
        when(titulos.findByAtendimentoIdAndCategoriaIsNotNull(r.atendimentoId())).thenReturn(Optional.empty());
        var resposta=service.registrar(r);
        var custo=resposta.itens().getFirst();
        assertEquals(new BigDecimal("7.88"),custo.custoTotal);
        assertEquals(r.medicoId(),resposta.titulo().medicoId());
        assertTrue(resposta.titulo().custosInformados());
        var captor=org.mockito.ArgumentCaptor.forClass(Titulo.class);
        verify(titulos).saveAndFlush(captor.capture());
        when(titulos.findByAtendimentoIdAndCategoriaIsNotNull(r.atendimentoId())).thenReturn(Optional.of(captor.getValue()));
        when(custos.findByTituloIdOrderByItemEstoqueId(captor.getValue().id)).thenReturn(resposta.itens());
        assertEquals(resposta.titulo().id(),service.registrar(r).titulo().id());
        verify(titulos,times(1)).saveAndFlush(any());
        verify(custos,times(1)).saveAll(any());
        var alterado=new Faturamento(r.atendimentoId(),r.medicoId(),r.pacienteId(),r.categoria(),r.descricao(),
            new BigDecimal("160.00"),r.dataCompetencia(),r.vencimento(),r.itens());
        assertThrows(IllegalStateException.class,() -> service.registrar(alterado));
    }
    @Test void rejeitaItemRepetido() {
        var item=new Item(UUID.randomUUID(),"Material",BigDecimal.ONE,BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class,() -> service.registrar(request(List.of(item,item))));
        verifyNoInteractions(titulos,custos);
    }
    @Test void listaVaziaConfirmaAtendimentoSemMateriais() {
        var r=request(List.of());
        when(titulos.findByAtendimentoIdAndCategoriaIsNotNull(r.atendimentoId())).thenReturn(Optional.empty());
        var resposta=service.registrar(r);
        assertTrue(resposta.itens().isEmpty());
        assertTrue(resposta.titulo().custosInformados());
    }
    @Test void exameProprioNaoTemMedicoNemTaxaDeRepasse() {
        var base=request(List.of(new Item(UUID.randomUUID(),"Reagente",BigDecimal.ONE,BigDecimal.TEN)));
        var exame=new Faturamento(base.atendimentoId(),null,base.pacienteId(),Titulo.Categoria.EXAME,
            "Exame da clínica",base.valor(),base.dataCompetencia(),base.vencimento(),base.itens());
        when(titulos.findByAtendimentoIdAndCategoriaIsNotNull(exame.atendimentoId())).thenReturn(Optional.empty());
        var resposta=service.registrar(exame);
        assertNull(resposta.titulo().medicoId());
        assertEquals(new BigDecimal("0.0000"),resposta.titulo().percentualClinica());
        assertEquals(BigDecimal.TEN.setScale(2),resposta.itens().getFirst().custoTotal);
    }
    @Test void consultaGuardaPercentualDaClinica() {
        var r=request(List.of());
        when(titulos.findByAtendimentoIdAndCategoriaIsNotNull(r.atendimentoId())).thenReturn(Optional.empty());
        assertEquals(new BigDecimal("0.3000"),service.registrar(r).titulo().percentualClinica());
    }
    @Test void rejeitaConsultaSemMedicoEExameComMedico() {
        var base=request(List.of());
        var consulta=new Faturamento(base.atendimentoId(),null,base.pacienteId(),Titulo.Categoria.CONSULTA,
            base.descricao(),base.valor(),base.dataCompetencia(),base.vencimento(),base.itens());
        var exame=new Faturamento(base.atendimentoId(),base.medicoId(),base.pacienteId(),Titulo.Categoria.EXAME,
            base.descricao(),base.valor(),base.dataCompetencia(),base.vencimento(),base.itens());
        assertThrows(IllegalArgumentException.class,() -> service.registrar(consulta));
        assertThrows(IllegalArgumentException.class,() -> service.registrar(exame));
        verifyNoInteractions(titulos,custos);
    }
}
