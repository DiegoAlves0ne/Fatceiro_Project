package br.com.clinica.financeiro.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class Titulo {
    public enum Tipo { RECEBER, PAGAR }
    public enum Status { ABERTO, PARCIAL, QUITADO, CANCELADO }
    public enum Categoria { CONSULTA, EXAME }
    @Id public UUID id;
    @Version public long version;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Tipo tipo;
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status;
    @Column(nullable=false,length=200) public String descricao;
    public UUID pacienteId;
    public UUID atendimentoId;
    @Column(unique=true) public UUID compraEstoqueId;
    public UUID fornecedorId;
    public UUID medicoId;
    public LocalDate dataCompetencia;
    @Enumerated(EnumType.STRING) @Column(length=20) public Categoria categoria;
    @Column(nullable=false) public boolean custosInformados;
    @Column(precision=5,scale=4) public BigDecimal percentualClinica;
    @Column(nullable=false,precision=19,scale=2) public BigDecimal valor;
    @Column(nullable=false,precision=19,scale=2) public BigDecimal liquidado;
    @Column(nullable=false) public LocalDate vencimento;
    protected Titulo() {}
    public Titulo(Tipo tipo,String descricao,UUID pacienteId,UUID atendimentoId,BigDecimal valor,LocalDate vencimento) {
        validarValor(valor);
        if (tipo == null || descricao == null || descricao.isBlank() || vencimento == null)
            throw new IllegalArgumentException("Tipo, descrição e vencimento são obrigatórios");
        this.id=UUID.randomUUID(); this.tipo=tipo; this.descricao=descricao; this.pacienteId=pacienteId;
        this.atendimentoId=atendimentoId; this.valor=valor; this.vencimento=vencimento;
        this.liquidado=new BigDecimal("0.00"); this.status=Status.ABERTO;
    }
    public BigDecimal saldo() { return valor.subtract(liquidado); }
    public void liquidar(BigDecimal pagamento) {
        validarValor(pagamento);
        if (status == Status.CANCELADO || status == Status.QUITADO) throw new IllegalStateException("Título não aceita pagamentos");
        if (pagamento.compareTo(saldo()) > 0) throw new IllegalStateException("Pagamento excede o saldo");
        liquidado=liquidado.add(pagamento); status=saldo().signum()==0 ? Status.QUITADO : Status.PARCIAL;
    }
    public void cancelar() {
        if (liquidado.signum()>0) throw new IllegalStateException("Título com pagamentos exige estorno antes do cancelamento");
        status=Status.CANCELADO;
    }
    private static void validarValor(BigDecimal valor) {
        if (valor==null || valor.signum()<=0 || valor.scale()>2 || valor.precision()-valor.scale()>17)
            throw new IllegalArgumentException("Valor deve ser positivo, com até 17 dígitos inteiros e duas casas decimais");
    }
}
