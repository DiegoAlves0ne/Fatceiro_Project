package br.com.clinica.financeiro.domain.contas;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "FaturaConvenio")
@Table(name = "fatura_convenio")
public class FaturaConvenio {
    @Id public UUID id = UUID.randomUUID();
    @Version public long version;
    @Column(nullable = false) public UUID convenioId;
    @Column(nullable = false) public LocalDate competencia;
    @Column(nullable = false) public LocalDate dataEmissao;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) public StatusFatura status = StatusFatura.ABERTA;
    @OneToMany(mappedBy = "faturaConvenio", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemFaturaConvenio> itens = new ArrayList<>();
}
