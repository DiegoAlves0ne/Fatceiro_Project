package br.com.clinica.financeiro.application;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import java.sql.ResultSet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RelatorioParticipacaoTest {
    private ResultSet linha(UUID medico,long consultas,long exames,String bruto,String materiais,String taxa) throws Exception {
        var rs=mock(ResultSet.class);
        when(rs.getObject("medico_id",UUID.class)).thenReturn(medico);
        when(rs.getLong("consultas")).thenReturn(consultas); when(rs.getLong("exames")).thenReturn(exames);
        when(rs.getBigDecimal("bruto")).thenReturn(new BigDecimal(bruto));
        when(rs.getBigDecimal("materiais")).thenReturn(new BigDecimal(materiais));
        when(rs.getBigDecimal("taxa")).thenReturn(new BigDecimal(taxa));
        when(rs.getBigDecimal("recebido")).thenReturn(BigDecimal.ZERO);
        when(rs.getBigDecimal("saldo")).thenReturn(new BigDecimal(bruto));
        return rs;
    }
    @Test void separaExamesDaClinicaDasConsultasDoMedico() throws Exception {
        var jdbc=mock(NamedParameterJdbcTemplate.class);
        var medicoId=UUID.randomUUID();
        var consulta=linha(medicoId,1,0,"200.00","30.00","60.00");
        var exame=linha(null,0,1,"100.00","20.00","0.00");
        when(jdbc.query(anyString(),any(SqlParameterSource.class),org.mockito.ArgumentMatchers.<RowMapper<RelatorioFaturamentoService.Medico>>any()))
            .thenAnswer(invocation -> {
                RowMapper<RelatorioFaturamentoService.Medico> mapper=invocation.getArgument(2);
                return List.of(mapper.mapRow(consulta,0),mapper.mapRow(exame,1));
            });
        var r=new RelatorioFaturamentoService(jdbc).consultar(RelatorioFaturamentoService.Periodo.DIA,LocalDate.of(2020,1,1),null);
        assertEquals(1,r.medicos().size());
        var m=r.medicos().getFirst();
        assertEquals(new BigDecimal("60.00"),m.taxaClinica());
        assertEquals(new BigDecimal("140.00"),m.repasseMedicoAntesMateriais());
        assertEquals(new BigDecimal("110.00"),m.repasseMedicoAposMateriais());
        assertEquals(new BigDecimal("30.00"),m.custoMateriais());
        assertEquals(0,m.examesFaturados());
        assertEquals(new BigDecimal("20.00"),r.clinica().custoMateriaisExames());
        assertEquals(new BigDecimal("140.00"),r.clinica().resultadoClinicaAntesOutrasDespesas());
        assertEquals(new BigDecimal("300.00"),r.totais().faturamentoBruto());
        // Resultado global após materiais = repasse médico + resultado da clínica.
        assertEquals(r.totais().resultadoAposMateriais(),m.repasseMedicoAposMateriais().add(r.clinica().resultadoClinicaAntesOutrasDespesas()));
    }
}
