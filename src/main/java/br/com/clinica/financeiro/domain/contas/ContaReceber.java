package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "ContaReceber")
@Table(name = "conta_receber")
public class ContaReceber {
    @Id public UUID id = UUID.randomUUID();
    @Version public long version;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public TipoDevedor tipoDevedor;
    @Column(nullable = false) public UUID devedorId;
    @Column(nullable = false) public LocalDate dataEmissao;
    @Column(nullable = false) public LocalDate dataVencimento;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valorOriginal;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public StatusConta status = StatusConta.ABERTO;
    @OneToMany(mappedBy = "contaReceber", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemContaReceber> itens = new ArrayList<>();
    @OneToMany(mappedBy = "contaReceber")
    public List<AplicacaoRecebimento> aplicacoes = new ArrayList<>();
}
