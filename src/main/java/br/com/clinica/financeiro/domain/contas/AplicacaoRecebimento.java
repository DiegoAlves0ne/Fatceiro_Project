package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity(name = "AplicacaoRecebimento")
@Table(name = "aplicacao_recebimento")
public class AplicacaoRecebimento {
    @Id public UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_receber_id", nullable = false) public ContaReceber contaReceber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recebimento_id", nullable = false) public Recebimento recebimento;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valorAplicado;
}
