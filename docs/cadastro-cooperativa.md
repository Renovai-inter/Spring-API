# Cadastro público de cooperativa

`POST /api/auth/cadastro-cooperativa`, sem token, com `Content-Type: application/json`.
Cria endereço, cooperativa, perfil institucional, usuário responsável e vínculo de funcionário em uma única
transação. Uma falha desfaz todos os registros. Não é necessário chamar os três
endpoints administrativos nem usar credenciais de administrador do site.

```json
{
  "nomeResponsavel": "Nome do responsável",
  "cpfResponsavel": "123.456.789-00",
  "nome": "Cooperativa Exemplo",
  "descricao": "Cooperativa de reciclagem",
  "numeroCooperados": 20,
  "horarioFuncionamento": "Segunda a sexta, das 8h às 17h",
  "imagemUrl": "https://exemplo.com/logo.png",
  "contatoPreferencial": "(11) 99999-9999",
  "email": "administracao@cooperativa.com.br",
  "cnpj": "12.345.678/0001-90",
  "senha": "senha123",
  "endereco": {
    "cep": "01001-000",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "complemento": "Galpão",
    "bairro": "Centro",
    "cidade": "São Paulo"
  }
}
```

Obrigatórios: `nomeResponsavel`, `cpfResponsavel`, `nome`, `contatoPreferencial`, `email`, `cnpj`, `senha` e `endereco`.
No endereço, todos os campos são obrigatórios exceto `complemento`.
`numeroCooperados` é opcional (padrão zero) e não pode ser negativo.
A senha tem de 6 a 72 caracteres e no máximo 72 bytes UTF-8, por limite do BCrypt.
CNPJ e CEP usam os formatos acima; a validação é de formato, sem consulta à Receita
ou verificação dos dígitos de CNPJ. E-mail é armazenado em minúsculas.

Resposta `201 Created`:

```json
{
  "cooperativaId": "<uuid>",
  "perfilId": "<uuid>",
  "enderecoId": "<uuid>",
  "email": "administracao@cooperativa.com.br",
  "role": "ADMIN_COOPERATIVA",
  "usuarioId": "<uuid>",
  "funcionarioId": "<uuid>"
}
```

## Fluxo do site

1. Enviar o formulário completo ao endpoint público e guardar os cinco IDs.
2. Fazer `POST /api/auth/login` com `email` retornado e `senha` informada.
3. Usar `Authorization: Bearer <token>` nas próximas chamadas.
4. Consultar `GET /api/cooperativas/{cooperativaId}`, `GET /api/perfis/{perfilId}`
   e `GET /api/enderecos/{enderecoId}`. Para editar, usar `PUT` nos mesmos caminhos,
   enviando os respectivos contratos completos existentes.

O login retorna `token`, `tipo: Bearer`, `email`, `role: ADMIN_COOPERATIVA` e
`usuarioId` real da tabela `usuarios`. Use `GET /api/funcionarios/por-usuario/{usuarioId}`
para recuperar o vínculo e a cooperativa. O `perfilId` continua identificando a instituição.
A senha dos novos cadastros fica somente em `usuarios`; `perfis` guarda e-mail,
CNPJ e endereço. O cargo `Admin` é reutilizado ou criado pelo backend e gera a
permissão `ADMIN_COOPERATIVA` no login. `ADMIN_SITE` continua sendo o cargo master.
O alias legado `Administrador` mantém seu mapeamento anterior para `ADMIN_SITE`.
Não é criado um cargo `ADMIN_COOPERATIVA`. O cliente não escolhe o cargo.
Nome do responsável e CPF com máscara `000.000.000-00` são obrigatórios; CPF deve ser único.

Contas antigas que autenticam por `Perfil` com senha continuam funcionando, inclusive
com o ID legado no login. Não há migração automática desses registros.
Cooperados, gestores, motoristas e empresas preservam seus fluxos de login.
Recuperação de senha dos novos administradores atualiza o usuário.

Erros seguem o `GlobalExceptionHandler`: `400` para campos inválidos, `422` para
e-mail/CNPJ/CPF já cadastrado ou senha acima do limite em bytes e `409` para conflito
de integridade detectado pelo banco, inclusive em cadastros concorrentes.

## Limites do fluxo atual

O perfil nasce ativo, com acesso imediato após login. Não há confirmação de e-mail,
aprovação institucional ou limitação de tentativas implementadas neste fluxo.
A imagem é uma URL; este endpoint não faz upload. O modelo atual não tem UF.
CORS continua aceitando `http://localhost:5173`; configure a origem real do site
quando houver um domínio de publicação definido.
Os endpoints administrativos continuam exigindo autenticação e suas permissões.
As regras atuais de autorização desses endpoints não foram redesenhadas para
isolar acesso por cooperativa neste cadastro.
