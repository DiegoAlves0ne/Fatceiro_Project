package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity(name = "AplicacaoPagamento")
@Table(name = "aplicacao_pagamento")
public class AplicacaoPagamento {
    @Id public UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_pagar_id", nullable = false) public ContaPagar contaPagar;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_id", nullable = false) public Pagamento pagamento;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valorAplicado;
}
