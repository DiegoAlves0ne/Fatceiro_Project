package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "ContaPagar")
@Table(name = "conta_pagar")
public class ContaPagar {
    @Id public UUID id = UUID.randomUUID();
    @Version public long version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public TipoCredor tipoCredor;
    @Column(nullable = false) public UUID credorId;
    @Column(nullable = false) public LocalDate dataEmissao;
    @Column(nullable = false) public LocalDate dataVencimento;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valorOriginal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public StatusConta status = StatusConta.ABERTO;
    @OneToMany(mappedBy = "contaPagar", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemContaPagar> itens = new ArrayList<>();
    @OneToMany(mappedBy = "contaPagar")
    public List<AplicacaoPagamento> aplicacoes = new ArrayList<>();
}
