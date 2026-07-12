# Renovaí API

API REST do sistema **Renovaí** — plataforma de gestão para cooperativas de reciclagem.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3 |
| Persistência | Spring Data JPA + PostgreSQL |
| Segurança | Spring Security + JWT (jjwt 0.12) |
| Documentação | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- PostgreSQL 14+ rodando localmente (ou via Docker)

---

## Configuração

### 1. Banco de dados

Execute o script SQL do projeto (schema Renovaí) em um banco chamado `renovai`:

```sql
CREATE DATABASE renovai;
```

### 2. Variáveis de ambiente

Crie um arquivo `.env` ou exporte as variáveis abaixo antes de rodar:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/renovai
export DB_USERNAME=postgres
export DB_PASSWORD=sua_senha
export JWT_SECRET=renovai-secret-key-2026-must-be-at-least-32-chars
export JWT_EXPIRATION=86400000   # 24h em milissegundos
```

### 3. Rodar a aplicação

```bash
mvn spring-boot:run
```

A API sobe em: `http://localhost:8080/api`

---

## Documentação (Swagger UI)

Acesse após subir a aplicação:

```
http://localhost:8080/api/swagger-ui.html
```

Clique em **Authorize** e insira `Bearer <token>` obtido via `/api/auth/login`.

---

## Autenticação

### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "gestor@cooperativa.com",
  "senha": "senha123"
}
```

Resposta:
```json
{
  "token": "eyJhbGci...",
  "tipo": "Bearer",
  "email": "gestor@cooperativa.com",
  "role": "GESTOR_COOPERATIVA"
}
```

Use o token nos demais endpoints:
```
Authorization: Bearer eyJhbGci...
```

---

## Perfis de Acesso (Roles)

| Role | Descrição |
|---|---|
| `ADMIN_SITE` | Acesso total ao sistema |
| `ADMIN_COOPERATIVA` | Gestão institucional da cooperativa |
| `GESTOR_COOPERATIVA` | Operações, estoque, pedidos e rateio |
| `FUNCIONARIO_COOPERATIVA` | Registro de coletas e triagens |
| `GESTOR_EMPRESA` | Busca de materiais e envio de pedidos |

---

## Endpoints

### Auth
| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/login` | Login e geração de JWT |

### Cooperativas
| Método | Rota | Acesso |
|---|---|---|
| GET | `/cooperativas` | Autenticado |
| GET | `/cooperativas/{id}` | Autenticado |
| POST | `/cooperativas` | ADMIN_SITE, ADMIN_COOPERATIVA |
| PUT | `/cooperativas/{id}` | ADMIN_SITE, ADMIN_COOPERATIVA |
| DELETE | `/cooperativas/{id}` | ADMIN_SITE |

### Materiais
| Método | Rota | Descrição |
|---|---|---|
| GET | `/materiais?categoria=&apenasDisponiveis=` | Listagem com filtros |
| POST | `/materiais` | GESTOR_COOPERATIVA |
| PUT | `/materiais/{id}` | GESTOR_COOPERATIVA |
| DELETE | `/materiais/{id}` | ADMIN_SITE |

### Coletas
| Método | Rota | Descrição |
|---|---|---|
| GET | `/coletas?cooperadoId=` | Lista coletas |
| POST | `/coletas` | FUNCIONARIO_COOPERATIVA |
| PUT | `/coletas/{id}` | FUNCIONARIO_COOPERATIVA |

### Triagens
| Método | Rota | Descrição |
|---|---|---|
| GET | `/triagens?coletaId=` | Lista triagens |
| POST | `/triagens` | Cria triagem **e atualiza estoque automaticamente** |

### Estoques
| Método | Rota | Descrição |
|---|---|---|
| GET | `/estoques?cooperativaId=&apenasDisponiveis=` | Listagem com filtros |
| PUT | `/estoques/{id}` | GESTOR_COOPERATIVA |

### Pedidos
| Método | Rota | Descrição |
|---|---|---|
| GET | `/pedidos?empresaId=` | Lista pedidos |
| POST | `/pedidos` | GESTOR_EMPRESA |
| POST | `/pedidos/itens` | Adiciona item |
| GET | `/pedidos/{pedidoId}/itens` | Lista itens |
| POST | `/pedidos/cooperativas` | Vincula pedido à cooperativa |
| PATCH | `/pedidos/cooperativas/{id}/status?statusId=` | Atualiza status (aceitar/recusar) |

### Rateios
| Método | Rota | Descrição |
|---|---|---|
| POST | `/rateios` | Cria rateio e **chama procedure `calcular_rateio`** |

### Avaliações
| Método | Rota | Descrição |
|---|---|---|
| GET | `/avaliacoes/media/{perfilId}` | Média de notas de um perfil |
| POST | `/avaliacoes` | Cria avaliação |

### Tabelas de domínio
- `GET/POST/PUT/DELETE /status`
- `GET/POST/PUT/DELETE /cargos`
- `GET/POST/PUT/DELETE /enderecos`
- `GET/POST/PUT/DELETE /perfis`
- `GET/POST/PATCH/DELETE /funcionarios`
- `GET/POST/PUT/DELETE /usuarios`
- `GET/POST/PUT/DELETE /empresas`

---

## Regras de negócio implementadas

- Triagem → estoque da cooperativa incrementado automaticamente
- Pedido → aceitar/recusar via `PATCH /pedidos/cooperativas/{id}/status`
- Avaliador ≠ Avaliado (validado na camada de serviço)
- Perfil tem **empresa OU cooperativa**, nunca os dois (validado no controller e no banco via CHECK)
- Email único por perfil
- CPF único por usuário
- Rateio aciona a procedure `calcular_rateio(p_rateio_id)` do PostgreSQL
- Senhas armazenadas com BCrypt

---

## Estrutura de pacotes

```
com.renovai.api
├── config/          # SecurityConfig, SwaggerConfig
├── controller/      # Um controller por recurso
├── dto/
│   ├── request/     # Requests (records com @Valid)
│   └── response/    # Responses (records imutáveis)
├── exception/       # GlobalExceptionHandler + exceções customizadas
├── model/           # Entidades JPA
├── repository/      # Interfaces Spring Data JPA
├── security/        # JwtTokenProvider, JwtAuthenticationFilter
└── service/         # Regras de negócio
```
