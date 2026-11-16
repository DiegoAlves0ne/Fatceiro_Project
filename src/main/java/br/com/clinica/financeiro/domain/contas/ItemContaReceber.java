package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity(name = "ItemContaReceber")
@Table(name = "item_conta_receber")
public class ItemContaReceber {
    @Id public UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_receber_id", nullable = false) public ContaReceber contaReceber;
    @Column(nullable = false) public UUID itemAgendamentoId;
    @Column(nullable = false) public UUID atendimentoId;
    @Column(nullable = false, length = 200) public String descricao;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valor;
    @OneToOne(mappedBy = "itemContaReceber", fetch = FetchType.LAZY)
    public ItemFaturaConvenio itemFaturaConvenio;
}
