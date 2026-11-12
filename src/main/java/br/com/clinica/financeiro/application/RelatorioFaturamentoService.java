package br.com.clinica.financeiro.application;

import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class RelatorioFaturamentoService {
    public enum Periodo { DIA, MES, ANO }
    public record Medico(UUID medicoId,long consultasFaturadas,long examesFaturados,BigDecimal faturamentoBruto,
        BigDecimal recebidoNoPeriodo,BigDecimal saldoAReceberDaProducao,BigDecimal custoMateriais,
        BigDecimal resultadoAposMateriais,BigDecimal taxaClinica,BigDecimal repasseMedicoAntesMateriais,
        BigDecimal repasseMedicoAposMateriais) {}
    public record Totais(long consultasFaturadas,long examesFaturados,BigDecimal faturamentoBruto,
        BigDecimal recebidoNoPeriodo,BigDecimal saldoAReceberDaProducao,BigDecimal custoMateriais,BigDecimal resultadoAposMateriais) {}
    public record Clinica(long examesFaturados,BigDecimal faturamentoExames,BigDecimal recebidoExamesNoPeriodo,
        BigDecimal saldoExames,BigDecimal custoMateriaisExames,BigDecimal participacaoConsultas,
        BigDecimal resultadoClinicaAntesOutrasDespesas) {}
    public record Relatorio(LocalDate inicio,LocalDate fim,String fusoHorario,Totais totais,List<Medico> medicos,Clinica clinica) {}
    public static Totais totalizar(List<Medico> medicos) {
        return new Totais(medicos.stream().mapToLong(Medico::consultasFaturadas).sum(),
            medicos.stream().mapToLong(Medico::examesFaturados).sum(),
            medicos.stream().map(Medico::faturamentoBruto).reduce(BigDecimal.ZERO,BigDecimal::add),
            medicos.stream().map(Medico::recebidoNoPeriodo).reduce(BigDecimal.ZERO,BigDecimal::add),
            medicos.stream().map(Medico::saldoAReceberDaProducao).reduce(BigDecimal.ZERO,BigDecimal::add),
            medicos.stream().map(Medico::custoMateriais).reduce(BigDecimal.ZERO,BigDecimal::add),
            medicos.stream().map(Medico::resultadoAposMateriais).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    private final NamedParameterJdbcTemplate jdbc;
    public RelatorioFaturamentoService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public static LocalDate[] intervalo(Periodo periodo,LocalDate referencia,LocalDate hoje) {
        if (referencia.isAfter(hoje)) throw new IllegalArgumentException("Referência não pode estar no futuro");
        var inicio=switch(periodo) {
            case DIA -> referencia;
            case MES -> referencia.withDayOfMonth(1);
            case ANO -> referencia.withDayOfYear(1);
        };
        var fim=switch(periodo) {
            case DIA -> referencia;
            case MES -> referencia.with(TemporalAdjusters.lastDayOfMonth());
            case ANO -> referencia.with(TemporalAdjusters.lastDayOfYear());
        };
        return new LocalDate[]{inicio,fim.isAfter(hoje)?hoje:fim};
    }

    @Transactional(readOnly=true)
    public Relatorio consultar(Periodo periodo,LocalDate referencia,UUID medicoId) {
        var zona=ZoneId.of("America/Sao_Paulo");
        var datas=intervalo(periodo,referencia,LocalDate.now(zona));
        var params=new MapSqlParameterSource()
            .addValue("inicio",Date.valueOf(datas[0])).addValue("fim",Date.valueOf(datas[1]))
            .addValue("inicioPagamento",Timestamp.from(datas[0].atStartOfDay(zona).toInstant()))
            .addValue("fimPagamento",Timestamp.from(datas[1].plusDays(1).atStartOfDay(zona).toInstant()));
        String filtro="";
        if (medicoId!=null) { params.addValue("medicoId",medicoId); filtro=" AND t.medico_id = :medicoId "; }
        // Pré-agregações evitam multiplicar pagamentos pelo número de itens consumidos.
        var sql="""
            SELECT t.medico_id,
              COUNT(*) FILTER (WHERE t.data_competencia BETWEEN :inicio AND :fim AND t.categoria='CONSULTA') consultas,
              COUNT(*) FILTER (WHERE t.data_competencia BETWEEN :inicio AND :fim AND t.categoria='EXAME') exames,
              COALESCE(SUM(CASE WHEN t.data_competencia BETWEEN :inicio AND :fim THEN t.valor ELSE 0 END),0) bruto,
              COALESCE(SUM(p.recebido),0) recebido,
              COALESCE(SUM(CASE WHEN t.data_competencia BETWEEN :inicio AND :fim THEN t.valor-t.liquidado ELSE 0 END),0) saldo,
              COALESCE(SUM(CASE WHEN t.data_competencia BETWEEN :inicio AND :fim THEN COALESCE(c.total,0) ELSE 0 END),0) materiais,
              COALESCE(SUM(CASE WHEN t.data_competencia BETWEEN :inicio AND :fim THEN ROUND(t.valor*t.percentual_clinica,2) ELSE 0 END),0) taxa
            FROM titulo t
            LEFT JOIN (SELECT titulo_id,SUM(custo_total) total FROM custo_item GROUP BY titulo_id) c ON c.titulo_id=t.id
            LEFT JOIN (SELECT titulo_id,SUM(valor) recebido FROM pagamento
              WHERE criado_em >= :inicioPagamento AND criado_em < :fimPagamento GROUP BY titulo_id) p ON p.titulo_id=t.id
            WHERE t.tipo='RECEBER' AND t.status <> 'CANCELADO' AND t.categoria IS NOT NULL
              AND (t.data_competencia BETWEEN :inicio AND :fim OR p.recebido IS NOT NULL)
            """+filtro+" GROUP BY t.medico_id ORDER BY t.medico_id";
        var linhas=jdbc.query(sql,params,(rs,n) -> {
            var bruto=rs.getBigDecimal("bruto"); var materiais=rs.getBigDecimal("materiais");
            var taxa=rs.getBigDecimal("taxa"); var repasse=bruto.subtract(taxa);
            return new Medico(rs.getObject("medico_id",UUID.class),rs.getLong("consultas"),rs.getLong("exames"),
                bruto,rs.getBigDecimal("recebido"),rs.getBigDecimal("saldo"),materiais,bruto.subtract(materiais),
                taxa,repasse,repasse.subtract(materiais));
        });
        var medicos=linhas.stream().filter(m -> m.medicoId()!=null).toList();
        var exames=totalizar(linhas.stream().filter(m -> m.medicoId()==null).toList());
        var participacao=medicos.stream().map(Medico::taxaClinica).reduce(BigDecimal.ZERO,BigDecimal::add);
        var clinica=new Clinica(exames.examesFaturados(),exames.faturamentoBruto(),exames.recebidoNoPeriodo(),
            exames.saldoAReceberDaProducao(),exames.custoMateriais(),participacao,
            participacao.add(exames.faturamentoBruto()).subtract(exames.custoMateriais()));
        return new Relatorio(datas[0],datas[1],zona.getId(),totalizar(linhas),medicos,clinica);
    }
}
