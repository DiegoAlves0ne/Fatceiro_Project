package br.com.clinica.financeiro.infrastructure;
import jakarta.persistence.*;
import br.com.clinica.financeiro.domain.FormaPagamento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity
public class Pagamento {
    @Id public UUID id;
    @Column(nullable=false) public UUID tituloId;
    @Column(nullable=false,unique=true,length=100) public String chaveIdempotencia;
    @Column(nullable=false,precision=19,scale=2) public BigDecimal valor;
    @Column(nullable=false) public Instant criadoEm;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) public FormaPagamento forma;
    protected Pagamento() {}
    public Pagamento(UUID tituloId,String chave,BigDecimal valor,FormaPagamento forma) {
        if (forma == null || forma == FormaPagamento.NAO_INFORMADA)
            throw new IllegalArgumentException("Informe a forma de pagamento");
        this.id=UUID.randomUUID(); this.tituloId=tituloId; this.chaveIdempotencia=chave;
        this.valor=valor; this.forma=forma; this.criadoEm=Instant.now();
    }
}
