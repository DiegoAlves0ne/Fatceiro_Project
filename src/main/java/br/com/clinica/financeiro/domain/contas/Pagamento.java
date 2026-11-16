package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import br.com.clinica.financeiro.domain.FormaPagamento;

@Entity(name = "PagamentoConta")
@Table(name = "pagamento_conta")
public class Pagamento {
    @Id public UUID id = UUID.randomUUID();
    @Version public long version;
    @Column(nullable = false) public Instant dataHora;
    @Column(nullable = false, precision = 19, scale = 2) public BigDecimal valor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public FormaPagamento forma;
    @Column(length = 200) public String referenciaTransacao;
    @OneToMany(mappedBy = "pagamento", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<AplicacaoPagamento> aplicacoes = new ArrayList<>();
}
