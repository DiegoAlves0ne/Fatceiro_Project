package br.com.clinica.financeiro.application;

import br.com.clinica.financeiro.domain.Titulo;
import br.com.clinica.financeiro.domain.FormaPagamento;
import br.com.clinica.financeiro.infrastructure.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FinanceiroServiceTest {
    private final TituloRepository titulos=mock(TituloRepository.class);
    private final PagamentoRepository pagamentos=mock(PagamentoRepository.class);
    private final FinanceiroService service=new FinanceiroService(titulos,pagamentos);

    @Test void repeticaoNaoLiquidaNovamente() {
        var id=UUID.randomUUID();
        var original=new Pagamento(id,"chave-1",new BigDecimal("50.00"),FormaPagamento.PIX);
        when(pagamentos.findByChaveIdempotencia("chave-1")).thenReturn(Optional.of(original));
        assertSame(original,service.pagar(id,"chave-1",new BigDecimal("50.0"),FormaPagamento.PIX));
        verifyNoInteractions(titulos);
        verify(pagamentos,never()).save(any());
    }
    @Test void rejeitaReutilizacaoComOutrosDados() {
        var id=UUID.randomUUID();
        when(pagamentos.findByChaveIdempotencia("chave-1"))
            .thenReturn(Optional.of(new Pagamento(id,"chave-1",new BigDecimal("50.00"),FormaPagamento.PIX)));
        assertThrows(IllegalStateException.class,() -> service.pagar(id,"chave-1",new BigDecimal("60.00"),FormaPagamento.PIX));
        assertThrows(IllegalStateException.class,() -> service.pagar(UUID.randomUUID(),"chave-1",new BigDecimal("50.00"),FormaPagamento.PIX));
        verifyNoInteractions(titulos);
    }
    @Test void pagamentoNovoAlteraSaldoERegistraHistorico() {
        var titulo=new Titulo(Titulo.Tipo.RECEBER,"Consulta",null,null,new BigDecimal("150.00"),LocalDate.now());
        when(pagamentos.findByChaveIdempotencia("chave-1")).thenReturn(Optional.empty());
        when(titulos.findById(titulo.id)).thenReturn(Optional.of(titulo));
        when(pagamentos.save(any())).thenAnswer(i -> i.getArgument(0));
        var pagamento=service.pagar(titulo.id,"chave-1",new BigDecimal("50.00"),FormaPagamento.PIX);
        assertEquals(titulo.id,pagamento.tituloId);
        assertEquals(new BigDecimal("100.00"),titulo.saldo());
        assertEquals(Titulo.Status.PARCIAL,titulo.status);
    }
    @Test void guardaValoresSeparadosPorForma() {
        var titulo=new Titulo(Titulo.Tipo.RECEBER,"Consulta",null,null,new BigDecimal("150.00"),LocalDate.now());
        when(pagamentos.findByChaveIdempotencia(anyString())).thenReturn(Optional.empty());
        when(titulos.findById(titulo.id)).thenReturn(Optional.of(titulo));
        when(pagamentos.save(any())).thenAnswer(i -> i.getArgument(0));
        var pix=service.pagar(titulo.id,"pix-1",new BigDecimal("50.00"),FormaPagamento.PIX);
        var dinheiro=service.pagar(titulo.id,"dinheiro-1",new BigDecimal("100.00"),FormaPagamento.DINHEIRO);
        assertEquals(FormaPagamento.PIX,pix.forma);
        assertEquals(new BigDecimal("50.00"),pix.valor);
        assertEquals(FormaPagamento.DINHEIRO,dinheiro.forma);
        assertEquals(new BigDecimal("100.00"),dinheiro.valor);
        assertEquals(Titulo.Status.QUITADO,titulo.status);
    }
    @Test void mesmaChaveComOutraFormaEhConflito() {
        var id=UUID.randomUUID();
        when(pagamentos.findByChaveIdempotencia("chave"))
            .thenReturn(Optional.of(new Pagamento(id,"chave",BigDecimal.TEN,FormaPagamento.PIX)));
        assertThrows(IllegalStateException.class,() -> service.pagar(id,"chave",BigDecimal.TEN,FormaPagamento.DINHEIRO));
    }
    @Test void compraGeraContaAPagarEReenvioRetornaOriginal() {
        var compraId=UUID.randomUUID(); var fornecedorId=UUID.randomUUID(); var vencimento=LocalDate.now();
        when(titulos.findByCompraEstoqueId(compraId)).thenReturn(Optional.empty());
        when(titulos.save(any())).thenAnswer(i -> i.getArgument(0));
        var titulo=service.registrarCompra(compraId,fornecedorId,"Equipamento",new BigDecimal("500.00"),vencimento);
        assertEquals(Titulo.Tipo.PAGAR,titulo.tipo);
        assertEquals(compraId,titulo.compraEstoqueId);
        assertEquals(fornecedorId,titulo.fornecedorId);
        when(titulos.findByCompraEstoqueId(compraId)).thenReturn(Optional.of(titulo));
        assertSame(titulo,service.registrarCompra(compraId,fornecedorId,"Equipamento",new BigDecimal("500.00"),vencimento));
        verify(titulos,times(1)).save(any());
        assertThrows(IllegalStateException.class,() -> service.registrarCompra(compraId,fornecedorId,"Equipamento",new BigDecimal("600.00"),vencimento));
    }
    @Test void bloqueiaContaAPagarManual() {
        var titulo=new Titulo(Titulo.Tipo.PAGAR,"Compra",null,null,BigDecimal.TEN,LocalDate.now());
        assertThrows(IllegalArgumentException.class,() -> service.criar(titulo));
        verifyNoInteractions(titulos);
    }
    @Test void naoPermitePagamentoSemForma() {
        assertThrows(IllegalArgumentException.class,() -> service.pagar(UUID.randomUUID(),"chave",BigDecimal.TEN,null));
        assertThrows(IllegalArgumentException.class,() -> service.pagar(UUID.randomUUID(),"chave",BigDecimal.TEN,FormaPagamento.NAO_INFORMADA));
        verifyNoInteractions(titulos,pagamentos);
    }
}
