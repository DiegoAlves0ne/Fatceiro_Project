package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity(name = "ItemContaPagar")
@Table(name = "item_conta_pagar")
public class ItemContaPagar {
    @Id public UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_pagar_id", nullable = false) public ContaPagar contaPagar;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public TipoOrigem origem;
    @Column(nullable = false) public UUID origemId;
    @Column(nullable = false, length = 200) public String descricao;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valor;
}
