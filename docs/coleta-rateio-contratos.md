# Coleta, triagem e rateio individual

## Materiais da coleta

`POST /coletas` mantém os campos anteriores e recebe `equipeId`, `precisaTriagem` e `materiais`:

```json
{
  "cooperadoId": "UUID",
  "quantidadeKg": 100,
  "tipoColeta": "INTERNA",
  "equipeId": "UUID",
  "precisaTriagem": false,
  "materiais": [
    {"materialId": "UUID", "quantidadeKg": 60},
    {"materialId": "UUID", "quantidadeKg": 40}
  ]
}
```

Os materiais devem ser distintos, e a soma dos pesos deve corresponder ao peso da coleta. A equipe deve pertencer à cooperativa do cooperado. Materiais globais ou da mesma cooperativa são aceitos.

Quando `precisaTriagem=false`, os materiais já estão separados: a API registra os vínculos em `triagens` com o status existente `Concluído` e as entradas de estoque na mesma transação da coleta. Isso permite rastrear a origem da entrada usando `movimentacoes_estoques.triagem_id`, sem modificar o schema.

Quando `precisaTriagem=true`, os vínculos usam `Em triagem` e não entram no estoque até a conclusão. Se os materiais ainda não forem conhecidos, a lista pode ser omitida; eles serão registrados pelos endpoints de triagem. Omitir o indicador mantém a necessidade de triagem para novas coletas.

As consultas da coleta devolvem `materiais`, com os registros de triagem, e `precisaTriagem`, derivado desses registros. Não existe nova coluna para esse indicador.

`PUT /coletas/{id}` permite corrigir os pesos dos mesmos materiais informando a lista completa. Alterações da separação ou de materiais com rejeito devem ser feitas pelos endpoints de triagem.

## Estoque da triagem

`triagens.quantidade_kg` representa o peso aproveitável, conforme as views do banco. `quantidade_rejeito_kg` é separado e não é descontado novamente desse peso.

Criar uma triagem pendente não registra entrada. Concluir por `PATCH /triagens/{id}/concluir`, atualizar o status para `Concluído` ou criar uma triagem já concluída registra o peso aproveitável. Correções em triagens concluídas registram somente a diferença. Repetir a mesma conclusão não duplica entrada.

Reabrir uma triagem estorna a entrada; corrigir ou estornar além do saldo disponível é rejeitado. Triagens com histórico de movimentação não podem ser excluídas. Equipe, coleta e material de uma triagem existente não podem ser trocados.

As operações usam bloqueios e transações. O ajuste de saldo considera o efeito de um trigger de movimentação para evitar aplicar o mesmo valor duas vezes.

## Rateio individual

`POST /rateios/executar-individual` exige gestor/administrador da cooperativa autenticado e recebe:

```json
{
  "gestorId": "UUID",
  "cooperativaId": "UUID",
  "dataInicio": "2026-09-01T00:00:00",
  "dataFim": "2026-09-30T23:59:59",
  "participantes": [
    {"cooperadoId": "UUID", "percentual": 70},
    {"cooperadoId": "UUID", "percentual": 30}
  ]
}
```

O app define os participantes e percentuais. Todos devem estar ativos e pertencer à cooperativa. Os percentuais devem ser positivos, ter até quatro casas decimais e somar exatamente 100%. Participantes repetidos são rejeitados.

O total é calculado pelas vendas do período, usando a mesma fonte dos rateios existentes. O período deve estar dentro de um único mês. Um rateio já existente para a cooperativa nesse mês impede uma nova execução, conforme a constraint `uq_rateio_cooperativa_mes`.

`tipoRateioId` pode ser informado para usar um tipo existente; quando omitido, usa o tipo já cadastrado `PROPORCIONAL`. Não cria novos tipos, tabelas ou colunas. Os valores são gravados em `rateios_funcionarios.valor_rateio`; os percentuais podem ser reconstruídos a partir dos valores persistidos.

Centavos de arredondamento são distribuídos pelas maiores frações restantes, preservando o total. Uma falha reverte o rateio e todas as distribuições.

## Validação

Os testes usam H2 para verificar transações, rollback, concorrência, idempotência e simulação do trigger de saldo. Há também testes com repositórios JPA reais. Essa validação local não substitui a execução no PostgreSQL implantado com os triggers do servidor.
