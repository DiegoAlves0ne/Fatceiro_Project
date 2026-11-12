package br.com.clinica.financeiro.api;

import br.com.clinica.financeiro.application.RelatorioFaturamentoService;
import br.com.clinica.financeiro.application.RelatorioFaturamentoService.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/relatorios/faturamento")
public class RelatorioFaturamentoController {
    private final RelatorioFaturamentoService service;
    public RelatorioFaturamentoController(RelatorioFaturamentoService service) { this.service=service; }
    @GetMapping
    public Relatorio consultar(@RequestParam(defaultValue="DIA") Periodo periodo,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate referencia,
        @RequestParam(required=false) UUID medicoId) {
        return service.consultar(periodo,referencia==null?LocalDate.now(ZoneId.of("America/Sao_Paulo")):referencia,medicoId);
    }
}
