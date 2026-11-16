package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity(name = "ItemFaturaConvenio")
@Table(name = "item_fatura_convenio")
public class ItemFaturaConvenio {
    @Id public UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fatura_convenio_id", nullable = false) public FaturaConvenio faturaConvenio;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_conta_receber_id", nullable = false, unique = true) public ItemContaReceber itemContaReceber;
    @Column(nullable = false) public UUID atendimentoId;
    public String numeroAutorizacao;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valorApresentado;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valorGlosado = new BigDecimal("0.00");
    public String motivoGlosa;
}
