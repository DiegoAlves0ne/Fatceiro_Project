package br.com.clinica.financeiro.domain;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
class TituloTest {
    private Titulo titulo() { return new Titulo(Titulo.Tipo.RECEBER,"Consulta",null,null,new BigDecimal("150.00"),LocalDate.now()); }
    @Test void pagamentosParciaisQuitamComPrecisao() {
        var t=titulo(); t.liquidar(new BigDecimal("49.90"));
        assertEquals(Titulo.Status.PARCIAL,t.status); assertEquals(new BigDecimal("100.10"),t.saldo());
        t.liquidar(new BigDecimal("100.10")); assertEquals(Titulo.Status.QUITADO,t.status);
        assertThrows(IllegalStateException.class,() -> t.liquidar(BigDecimal.ONE));
    }
    @Test void rejeitaExcesso() {
        var t=titulo(); assertThrows(IllegalStateException.class,() -> t.liquidar(new BigDecimal("150.01")));
        assertEquals(Titulo.Status.ABERTO,t.status);
    }
    @Test void naoCancelaComPagamento() {
        var t=titulo(); t.liquidar(BigDecimal.ONE); assertThrows(IllegalStateException.class,t::cancelar);
    }
    @Test void canceladoNaoRecebe() {
        var t=titulo(); t.cancelar(); assertThrows(IllegalStateException.class,() -> t.liquidar(BigDecimal.ONE));
    }
    @Test void rejeitaValoresInvalidos() {
        for (var v : new String[]{"0","-1","1.001"})
            assertThrows(IllegalArgumentException.class,() -> titulo().liquidar(new BigDecimal(v)));
    }
}
