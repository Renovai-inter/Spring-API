# Cadastro público de cooperativa

`POST /api/auth/cadastro-cooperativa`, sem token, com `Content-Type: application/json`.
Cria endereço, cooperativa e administrador institucional (`Perfil`) em uma única
transação. Uma falha desfaz todos os registros. Não é necessário chamar os três
endpoints administrativos nem usar credenciais de administrador do site.

```json
{
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

Obrigatórios: `nome`, `contatoPreferencial`, `email`, `cnpj`, `senha` e `endereco`.
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
  "role": "ADMIN_COOPERATIVA"
}
```

## Fluxo do site

1. Enviar o formulário completo ao endpoint público e guardar os três IDs.
2. Fazer `POST /api/auth/login` com `email` retornado e `senha` informada.
3. Usar `Authorization: Bearer <token>` nas próximas chamadas.
4. Consultar `GET /api/cooperativas/{cooperativaId}`, `GET /api/perfis/{perfilId}`
   e `GET /api/enderecos/{enderecoId}`. Para editar, usar `PUT` nos mesmos caminhos,
   enviando os respectivos contratos completos existentes.

O login retorna `token`, `tipo: Bearer`, `email`, `role: ADMIN_COOPERATIVA` e
`usuarioId`, que neste fluxo é o **perfilId**, não um registro de `/usuarios`.
O administrador é uma conta institucional: não cria pessoa física, cargo ou
funcionário; não precisa dos endpoints de primeiro acesso ou completar cadastro.
O cliente não escolhe role, empresa ou vínculos com registros existentes.

Erros seguem o `GlobalExceptionHandler`: `400` para campos inválidos, `422` para
e-mail/CNPJ já cadastrado ou senha acima do limite em bytes e `409` para conflito
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
