# Financeiro e faturamento da clínica

[Diagrama de classes atual](docs/diagrama-classes.md) — entidades, serviços, controladores, repositórios e DTOs.

Microsserviço Java 21 / Spring Boot, com API REST versionada e banco PostgreSQL exclusivo. Faturamento inicial é representado por títulos a receber vinculáveis a pacientes e atendimentos. Contas a pagar são geradas por compras informadas pelo microsserviço de estoque. Moeda assumida: BRL.

## Executar

Com Docker e Compose instalados:

```sh
docker compose up --build
```

API: `http://localhost:8080`. Saúde: `GET /actuator/health`.
As credenciais do Compose são exclusivas para desenvolvimento local.

Alternativamente, com JDK 21 e Maven 3.6.3+:

```sh
docker compose up -d postgres
mvn verify
mvn spring-boot:run
```

Configuração por ambiente: `DB_URL`, `DB_USER`, `DB_PASSWORD` e `PORT`. Flyway cria o esquema; Hibernate apenas valida.

## Organização

```text
br.com.clinica.financeiro
├── api             # HTTP, DTOs, validação e erros Problem Details
├── application     # Casos de uso e transações
├── domain          # Títulos e regras monetárias
└── infrastructure  # Persistência JPA e pagamentos
```

Financeiro e faturamento formam um serviço inicialmente, permitindo liquidar títulos e registrar pagamentos na mesma transação. Não há necessidade de dividir cada entidade em outro serviço.

## Contrato HTTP

| Método | Caminho | Comportamento |
|---|---|---|
| POST | `/api/v1/titulos` | Cria título; retorna 201 e Location |
| GET | `/api/v1/titulos/{id}` | Consulta saldo e situação |
| GET | `/api/v1/titulos?page=0&size=20` | Lista paginada; máximo 100 |
| POST | `/api/v1/titulos/{id}/pagamentos` | Registra recebimento ou pagamento |
| GET | `/api/v1/titulos/{id}/pagamentos?page=0&size=20` | Histórico com valor e forma de cada pagamento |
| POST | `/api/v1/integracoes/estoque/compras` | Gera conta a pagar por compra; reenvio idêntico retorna o título original |
| POST | `/api/v1/titulos/{id}/cancelamento` | Cancela título sem pagamentos; retorna 204 |

Criar faturamento de consulta:

```json
{
  "tipo": "RECEBER",
  "descricao": "Consulta particular",
  "pacienteId": "f623d985-81af-4a45-8e65-65e456c98a91",
  "atendimentoId": "8bd31b1e-c211-4c6c-8480-762c67d33cd4",
  "valor": 150.00,
  "vencimento": "2026-12-10"
}
```

O cadastro manual de títulos aceita somente `RECEBER`. Para contas a pagar, use a integração de estoque descrita abaixo. Os IDs de paciente e atendimento são opcionais e não são validados por chamadas a outros serviços neste estágio.

Para registrar pagamento, envie `{"valor": 50.00, "forma": "PIX"}` e o cabeçalho `Idempotency-Key: pagamento-externo-123`. A forma é obrigatória também para contas a pagar. Repetir a mesma chave, título, valor e forma retorna o pagamento original. Reutilizá-la com outros dados retorna 409. A chave é global no serviço. Em uma corrida concorrente, uma tentativa pode receber 409; repita com a mesma chave. A criação manual de títulos a receber ainda não é idempotente.

Formas aceitas: `PIX`, `DINHEIRO`, `CARTAO_CREDITO`, `CARTAO_DEBITO`, `BOLETO`, `TRANSFERENCIA`. `NAO_INFORMADA` é reservado aos registros antigos migrados e não pode ser enviado em novos pagamentos.

Cada chamada registra uma parcela com uma forma. Para uma consulta de R$ 150 paga com R$ 50 em PIX e R$ 100 em dinheiro, envie dois pagamentos com chaves distintas. O banco mantém os dois valores, formas e datas individualmente, e o título fica quitado. O GET de pagamentos permite recuperar esse detalhamento, inclusive depois da quitação. As chamadas são independentes: se a segunda falhar, a primeira continua registrada.

## Compras do estoque

O outro microsserviço deve chamar `POST /api/v1/integracoes/estoque/compras` após confirmar uma compra:

```json
{
  "compraId": "ccddca44-9aac-44eb-b4ce-d72fedf506f0",
  "fornecedorId": "ceafafbc-24a4-4508-a3ab-a506bb325105",
  "descricao": "Compra de equipamento de ultrassom",
  "valorTotal": 15000.00,
  "vencimento": "2026-12-10"
}
```

O endpoint retorna 200 com o título `PAGAR`, `compraEstoqueId` e `fornecedorId`. A referência da compra é única no banco. Reenviar o mesmo ID e dados retorna o título original; alterar fornecedor, descrição, valor ou vencimento para uma compra já recebida retorna 409. Em envios simultâneos, pode ocorrer 409; repita com os mesmos dados. Reenviar uma compra cancelada retorna o título cancelado, sem reabri-lo.

Assumimos uma conta a pagar por compra. A composição dos itens, quantidades, equipamentos e movimentações continua no estoque e pode ser recuperada lá pelo ID da compra. Não há consulta automática ao estoque, broker ou atualização/cancelamento automático por evento nesta etapa. O contrato usa UUIDs; a outra equipe deverá adotar esses identificadores ou acordar um adaptador. Contas a pagar antigas são preservadas pela migração, mesmo sem referência de compra.

Valores usam BigDecimal com até duas casas decimais. Pagamentos não podem exceder o saldo. Situações: ABERTO, PARCIAL, QUITADO e CANCELADO. Cancelamento com pagamento é bloqueado; o fluxo de estorno ainda não está implementado. Concorrência usa versão otimista, e pagamentos e saldo são persistidos na mesma transação. Recursos ausentes retornam 404; validação retorna 400; conflitos retornam 409.

## Faturamento por médico e materiais consumidos

O microsserviço de atendimento deve enviar o atendimento concluído e todos os materiais consumidos em `POST /api/v1/integracoes/atendimentos/faturamentos`:

```json
{
  "atendimentoId": "20cf9c90-395e-4f43-b625-ac347071fcfa",
  "medicoId": "41a61be7-5f0d-495e-b49c-34a8890f63b7",
  "pacienteId": "f623d985-81af-4a45-8e65-65e456c98a91",
  "categoria": "CONSULTA",
  "descricao": "Consulta clínica",
  "valor": 150.00,
  "dataCompetencia": "2026-11-11",
  "vencimento": "2026-11-20",
  "itens": [
    {
      "itemEstoqueId": "90a56e2a-f62d-4adb-83fb-88c4fe37599c",
      "descricao": "Material descartável",
      "quantidade": 2.000,
      "custoUnitario": 10.00
    }
  ]
}
```

Categorias: `CONSULTA` e `EXAME`. CONSULTA exige `medicoId`. EXAME representa exame próprio da clínica e exige `medicoId` ausente ou null; informar médico em exame retorna 400. A data de competência é a data do serviço concluído, não o vencimento; não aceita futuro. O atendente informa os itens no sistema de atendimento, que envia essa requisição ao financeiro. O custo unitário deve ser o custo histórico de aquisição/consumo informado por esse sistema em acordo com o estoque, e não o preço de venda. O financeiro não consulta preços automaticamente.

A lista `itens` é obrigatória: `[]` confirma que não houve materiais. Quantidades têm até três casas decimais e custo unitário duas. Cada linha é arredondada para centavos com HALF_UP; itens repetidos devem ser agrupados pelo remetente. Os custos são armazenados individualmente e podem ser consultados em `GET /api/v1/titulos/{id}/custos`. Mudanças futuras de preço no estoque não alteram esses registros.

Um atendimento gera um único título, associado ao médico nas consultas e à clínica nos exames próprios. Faturamento e custos são gravados na mesma transação. Reenvio idêntico retorna 200 com o título original; mudança de dados retorna 409. A ordem dos itens e zeros decimais adicionais não mudam a comparação. Em uma corrida, repita com os mesmos dados após um eventual 409. A integração pressupõe que todos os custos estejam confirmados antes do envio: não há edição posterior, rateio entre médicos ou múltiplas cobranças por atendimento. Correções/estornos precisam de um fluxo próprio futuro. Não cadastre o mesmo atendimento também pelo endpoint manual.

### Relatório

```http
GET /api/v1/relatorios/faturamento?periodo=DIA&referencia=2026-11-11
GET /api/v1/relatorios/faturamento?periodo=MES&referencia=2026-11-11&medicoId=41a61be7-5f0d-495e-b49c-34a8890f63b7
GET /api/v1/relatorios/faturamento?periodo=ANO&referencia=2026-11-11
```

Sem referência, usa hoje; sem período, DIA; sem médico, retorna todos os médicos com movimento e o bloco dos exames próprios da clínica. A resposta contém totais e linhas por médico. Se filtrar médico, os totais representam somente esse médico. Períodos históricos completos seguem o calendário; o período atual termina hoje, no fuso `America/Sao_Paulo`. Não há limite inicial de hora para um atendimento: a competência é uma data. Pagamentos usam intervalos de instantes no mesmo fuso, incluindo o início e excluindo o início do dia seguinte ao fim.

| Campo | Significado |
|---|---|
| consultasFaturadas | Quantidade de atendimentos faturados como CONSULTA no período |
| examesFaturados | Quantidade de atendimentos faturados como EXAME no período |
| faturamentoBruto | Valor cobrado pela produção com competência no período |
| recebidoNoPeriodo | Pagamentos efetivamente registrados no período, mesmo de atendimentos antigos |
| saldoAReceberDaProducao | Saldo atual dos atendimentos produzidos no período |
| custoMateriais | Soma dos custos históricos dos itens desses atendimentos |
| resultadoAposMateriais | faturamentoBruto menos custoMateriais; pode ser negativo |

Exemplo: consulta de R$ 150 com dois materiais de R$ 10 gera faturamento de R$ 150, custo de R$ 20 e resultado após materiais de R$ 130. Se recebeu somente R$ 50, o recebido é R$ 50 e o saldo atual R$ 100; isso não reduz o faturamento por competência.

O relatório conta serviços faturados, não consultas apenas agendadas. Não calcula lucro líquido: faltam repasses médicos, taxas de cartão, impostos e despesas operacionais. A compra de estoque gera contas a pagar, mas não é novamente descontada no resultado por atendimento: o custo entra pelo consumo. Equipamentos duráveis exigirão regras de depreciação/rateio, não a baixa integral do preço em cada consulta.

Limites: títulos cancelados são excluídos; perdas de materiais em atendimentos cancelados ainda não têm relatório próprio. O saldo é o saldo de agora, não uma reconstrução histórica do saldo no fechamento. Recebimentos e competência usam bases diferentes e não devem ser subtraídos entre si como lucro. Registros antigos e títulos manuais sem médico/competência ficam fora deste relatório e precisam de conciliação/migração de dados; não há atribuição automática. A API permite integração, mas depende do envio pelo serviço da outra equipe.

## Evolução da integração

### Participação da clínica e demonstrativo médico

Para cada CONSULTA, a clínica retém 30% do valor bruto cobrado. O médico tem direito aos 70% restantes, descontando os materiais da própria consulta:

```text
taxaClinica = arredondar(valorConsulta × 0,30, 2 casas)
repasseMedicoAntesMateriais = valorConsulta − taxaClinica
repasseMedicoAposMateriais = repasseMedicoAntesMateriais − custoMateriaisConsulta
```

A taxa é arredondada por consulta, com HALF_UP. Os relatórios somam essas taxas individuais, evitando diferença de centavos por arredondamento global. O percentual é registrado no título (`percentualClinica: 0.3000`). Exemplo: consulta de R$ 200 e materiais de R$ 30 resulta em R$ 60 para a clínica e R$ 110 de repasse calculado para o médico. O custo dos materiais é de R$ 30. Resultados negativos são mantidos, sem zerar ou esconder o déficit.

O bloco `medicos` contém somente consultas e seus materiais; exames próprios não aparecem nele. Os campos `taxaClinica`, `repasseMedicoAntesMateriais` e `repasseMedicoAposMateriais` foram acrescentados. O campo anterior `resultadoAposMateriais` continua representando o bruto menos materiais, antes da taxa: use `repasseMedicoAposMateriais` para o valor final do médico. O detalhamento de itens de cada consulta continua em `GET /api/v1/titulos/{id}/custos`.

EXAME deve ser enviado com `categoria: EXAME` e `medicoId: null`. Seus materiais são gastos exclusivos da clínica, sua receita também pertence à clínica e não há repasse ao médico. O novo bloco `clinica` retorna:

- `examesFaturados`, `faturamentoExames`, `recebidoExamesNoPeriodo` e `saldoExames`;
- `custoMateriaisExames`, sem incluir materiais descontados do médico;
- `participacaoConsultas`, soma das taxas de 30%;
- `resultadoClinicaAntesOutrasDespesas`, participação das consultas mais receita dos exames menos materiais dos exames.

Sem filtro de médico, `totais` inclui consultas e exames, e `clinica` inclui todos os exames próprios. Com `medicoId`, apenas as consultas desse médico entram no relatório; exames da clínica não entram nem nos totais nem no bloco da clínica desse recorte. A participação exibida é somente a desse médico.

Esses valores são apurados por competência, mesmo com recebimento parcial. Não representam repasses já pagos, não movimentam dinheiro e não geram automaticamente contas a pagar ao médico. O recebimento do paciente continua sendo registrado separadamente. Ainda não há impostos, taxas de adquirentes, despesas fixas, liquidação dos repasses ou autenticação de um portal médico.

A migração V4 aplica a taxa de 30% também às consultas já registradas e reclassifica todos os EXAME anteriores como próprios da clínica, removendo o médico desses títulos. Itens e pagamentos são preservados. Um reenvio de exame anterior deve seguir o contrato novo, sem médico. Títulos sem categoria continuam fora dos relatórios. O índice de unicidade passa a abranger atendimentos de consultas e exames próprios.

Cada serviço deve possuir seu banco; pacienteId e atendimentoId são referências externas, sem chaves estrangeiras para bancos de outros serviços. Outros serviços podem consumir esta API HTTP. Autenticação e autorização devem ser definidas com a plataforma antes da exposição externa; esta base local ainda não as implementa.

Uma próxima etapa pode receber eventos de atendimento concluído para gerar cobranças. Antes de adicionar um broker, definir contratos versionados, deduplicação por evento e outbox transacional. Mensageria e publicação de eventos ainda não estão implementadas.

## Escopo e validação

Os testes unitários cobrem liquidação parcial, quitação, excesso de pagamento, cancelamento e valores inválidos. Execute `mvn verify`; o build Docker também executa essa etapa. Ainda são necessários testes de integração com PostgreSQL para validar migrações, persistência, HTTP e concorrência.

Esta é uma base inicial, não um produto pronto para produção. Não inclui emissão de NFS-e, TISS, convênios, glosas, parcelamento, estornos, conciliação bancária, relatórios contábeis, múltiplas clínicas ou integração com adquirentes. Não armazena prontuários. Os arquivos em `src/main/resources/archetype-resources` são do esqueleto anterior e não fazem parte das classes Java executáveis.

Referência técnica da versão: [requisitos oficiais do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).
