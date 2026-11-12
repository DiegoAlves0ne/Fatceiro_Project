package br.com.clinica.financeiro.application;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static br.com.clinica.financeiro.application.RelatorioFaturamentoService.*;
import java.time.LocalDate;
class RelatorioFaturamentoServiceTest {
    @Test void totalizaMedicosPreservandoResultadoNegativo() {
        var a=new Medico(java.util.UUID.randomUUID(),1,0,new java.math.BigDecimal("150.00"),new java.math.BigDecimal("50.00"),
            new java.math.BigDecimal("100.00"),new java.math.BigDecimal("20.00"),new java.math.BigDecimal("130.00"),
            new java.math.BigDecimal("45.00"),new java.math.BigDecimal("105.00"),new java.math.BigDecimal("85.00"));
        var b=new Medico(java.util.UUID.randomUUID(),0,1,new java.math.BigDecimal("10.00"),java.math.BigDecimal.ZERO,
            new java.math.BigDecimal("10.00"),new java.math.BigDecimal("30.00"),new java.math.BigDecimal("-20.00"),
            new java.math.BigDecimal("3.00"),new java.math.BigDecimal("7.00"),new java.math.BigDecimal("-23.00"));
        var total=totalizar(java.util.List.of(a,b));
        assertEquals(new java.math.BigDecimal("160.00"),total.faturamentoBruto());
        assertEquals(new java.math.BigDecimal("110.00"),total.resultadoAposMateriais());
        assertEquals(1,total.consultasFaturadas());
        assertEquals(1,total.examesFaturados());
    }
    @Test void mesAtualVaiAteHoje() {
        var hoje=LocalDate.of(2026,11,11);
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026,11,1),hoje},intervalo(Periodo.MES,hoje,hoje));
    }
    @Test void mesHistoricoRespeitaAnoBissexto() {
        assertArrayEquals(new LocalDate[]{LocalDate.of(2024,2,1),LocalDate.of(2024,2,29)},
            intervalo(Periodo.MES,LocalDate.of(2024,2,10),LocalDate.of(2026,11,11)));
    }
    @Test void anoAtualVaiAteHoje() {
        var hoje=LocalDate.of(2026,11,11);
        assertArrayEquals(new LocalDate[]{LocalDate.of(2026,1,1),hoje},intervalo(Periodo.ANO,hoje,hoje));
    }
    @Test void diaNaoIncluiOutrosDias() {
        var dia=LocalDate.of(2026,1,1);
        assertArrayEquals(new LocalDate[]{dia,dia},intervalo(Periodo.DIA,dia,LocalDate.of(2026,11,11)));
    }
    @Test void rejeitaFuturo() {
        var hoje=LocalDate.of(2026,11,11);
        assertThrows(IllegalArgumentException.class,() -> intervalo(Periodo.ANO,hoje.plusDays(1),hoje));
    }
}
