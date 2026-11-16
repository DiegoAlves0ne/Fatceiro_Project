# Modelo de contas do diagrama base

As dez entidades da imagem `Diagrama_De_Classe_Base.jpg` foram adicionadas ao pacote
`br.com.clinica.financeiro.domain.contas`, com persistência JPA e migração Flyway V5.
UUID representa identificadores, BigDecimal os valores monetários, LocalDate as datas
e Instant o instante de recebimento/pagamento.

## Relacionamentos

- ContaReceber possui ItemContaReceber; ContaPagar possui ItemContaPagar.
- Recebimento possui AplicacaoRecebimento, cada aplicação ligada a uma ContaReceber.
- Pagamento possui AplicacaoPagamento, cada aplicação ligada a uma ContaPagar.
- FaturaConvenio possui ItemFaturaConvenio, cada item ligado a um ItemContaReceber.
- Um ItemContaReceber pode participar de no máximo um ItemFaturaConvenio (FK única).

Os pais possuem coleções inversas; ao montar os objetos, preencha também a referência
ao pai no filho, que é o lado proprietário da FK. Cascata e remoção de órfãos se aplicam
somente às composições do diagrama. As referências a outros microsserviços são UUIDs,
sem chaves estrangeiras externas. As raízes têm versão para controle otimista.

## Decisões iniciais

A imagem não enumera os valores dos tipos. Foram adotados:

- TipoDevedor: PACIENTE, CONVENIO.
- TipoCredor: FORNECEDOR, MEDICO.
- TipoOrigem: COMPRA_ESTOQUE, REPASSE_MEDICO, DESPESA.
- StatusConta: ABERTO, PARCIAL, QUITADO, CANCELADO.
- StatusFatura: ABERTA, ENVIADA, PROCESSADA, CANCELADA.
- FormaPagamento reutiliza o enum existente.

O banco exige valores positivos, glosa não negativa e limitada ao valor apresentado.
As cardinalidades mínimas de coleções (`1..*`), soma dos itens, saldo, soma das aplicações,
transições de status e consistência de atendimento/convênio ainda precisam ser aplicadas
pelos casos de uso transacionais antes de expor o modelo pela API.

## Compatibilidade e próximo passo

Esta implementação adiciona o modelo persistente. Os endpoints, relatórios e integrações
atuais continuam utilizando Titulo e infrastructure.Pagamento. Não há migração automática
dos registros antigos, novos endpoints ou liquidação pelo modelo novo nesta etapa.
O novo Pagamento usa o nome JPA PagamentoConta e a tabela pagamento_conta para evitar
conflito com a entidade/tabela pagamento existente.

A adoção nos serviços requer definir o preenchimento dos IDs externos, o fluxo das faturas,
as regras de glosa e a conversão dos títulos/pagamentos antigos. A V5 é aditiva e preserva
as tabelas anteriores. A validação de migração e persistência em PostgreSQL deve ocorrer
antes dessa adoção.
