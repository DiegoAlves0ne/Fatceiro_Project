package br.com.clinica.financeiro.infrastructure;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name="custo_item")
public class CustoItem {
    @Id public UUID id;
    @Column(nullable=false) public UUID tituloId;
    @Column(nullable=false) public UUID itemEstoqueId;
    @Column(nullable=false,length=200) public String descricao;
    @Column(nullable=false,precision=12,scale=3) public BigDecimal quantidade;
    @Column(nullable=false,precision=19,scale=2) public BigDecimal custoUnitario;
    @Column(nullable=false,precision=19,scale=2) public BigDecimal custoTotal;
    protected CustoItem() {}
    public CustoItem(UUID tituloId,UUID itemId,String descricao,BigDecimal quantidade,BigDecimal custoUnitario) {
        this.id=UUID.randomUUID(); this.tituloId=tituloId; this.itemEstoqueId=itemId;
        this.descricao=descricao; this.quantidade=quantidade; this.custoUnitario=custoUnitario;
        this.custoTotal=quantidade.multiply(custoUnitario).setScale(2,java.math.RoundingMode.HALF_UP);
        if (this.custoTotal.precision()>19) throw new IllegalArgumentException("Custo total excede o limite monetário");
    }
}
