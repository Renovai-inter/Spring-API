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


## Deploy AWS por release

O workflow `.github/workflows/deploy-aws-prod.yml` executa testes, publica a
imagem no ECR e atualiza o EKS somente ao publicar uma release estável
(**Releases > Draft a new release > Publish release**). O deploy usa o commit
da tag da release. Push, pull request, rascunho e pré-release não acionam o
deploy de produção. O workflow de CI continua independente.


## Entrada compartilhada: NLB e Nginx

O workflow `.github/workflows/gateway.yml` valida o proxy em push/PR e implanta
somente na publicação de release estável. É independente do deploy Java: ambos
rodam na release do Spring, enquanto Redis e IA mantêm suas releases próprias.
Não é preciso outro repositório nem construir uma imagem customizada do Nginx.

O único Service LoadBalancer é `renovai-gateway`, no namespace `renovai-api`.
As três APIs continuam ClusterIP. A configuração em `orchestration/gateway/nginx.conf`
preserva caminho, query string, corpo e Authorization:

| Caminho público | Destino interno |
|---|---|
| `/api` e `/api/...` | `springboot-service:80` |
| `/rotas` e `/rotas/...` | `redis-api-service:80` |
| Demais caminhos, incluindo `/chat`, `/docs`, `/monitor.html`, `/assets/...` | `ia-api-service:80` |
| `/gateway-health` | Health do Nginx, independente das APIs |

`/health` continua sendo o health da IA. O roteamento reserva todo `/rotas` para
Redis; o RPA chama o retreino da IA pelo Service interno, sem passar pelo gateway.
O proxy resolve Services dinamicamente para permitir deploys independentes,
não repete automaticamente requisições e não faz buffering das respostas do chat.
O timeout de leitura é 120s entre leituras e o limite do corpo é 10MiB.

### Preparação AWS

- EKS convencional com controlador legado de Services, VPC CNI e subnets
  públicas identificadas por `kubernetes.io/role/elb=1`. O Service usa a anotação
  `aws-load-balancer-type: "nlb"`, sem loadBalancerClass, com targets de instância
  e encaminhamento por NodePort. Não use este manifesto em EKS Auto Mode.
- Aplicar o Service solicita o NLB; não precisa instalar o AWS Load Balancer
  Controller ou criar uma role IRSA. Permissões ELB do cluster/laboratório
  continuam necessárias. Os pipelines não criam roles ou alteram IAM.
- O workflow usa os mesmos Secrets AWS do deploy Spring: `AWS_REGION`,
  `EKS_CLUSTER_NAME`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` e, para
  credenciais temporárias, `AWS_SESSION_TOKEN`. A identidade precisa consultar
  CoreDNS e gerenciar ConfigMap, Deployment e Service.
- Opcional: variável GitHub `NLB_CERTIFICATE_ARN`, ARN de um certificado ACM
  emitido na mesma região. Com ela, o Service publica apenas TLS 443 e o NLB
  termina TLS antes de encaminhar HTTP ao Nginx. Aponte seu domínio ao NLB e
  use o nome coberto pelo certificado. Sem ela, publica somente HTTP 80,
  adequado apenas a testes sem dados sensíveis. HTTP não serve para frontend HTTPS.

### Migração e verificação

1. Publique os deploys atualizados das três APIs e a release do Spring com gateway.
   Não remova a entrada antiga antes de testar a nova.
2. Aguarde o hostname do Service e targets saudáveis no console EC2:

```bash
kubectl get service renovai-gateway -n renovai-api -w
kubectl describe service renovai-gateway -n renovai-api
```

3. Teste `/gateway-health`, `/api/actuator/health`, `/rotas/ID_EXISTENTE`, `/health`
   e uma chamada real ao chat. O health do Nginx e o rollout não comprovam saúde
   dos bancos, APIs nem conclusão do provisionamento AWS.
4. Atualize a URL base do app/site e o DNS. Depois, remova os Ingresses antigos:

```bash
kubectl delete ingress springboot-ingress redis-api-ingress ia-api-ingress -n renovai-api --ignore-not-found
```

Confirme a remoção do ALB e de eventuais NLBs antigos no console antes de considerar
que existe apenas um balanceador. Services das APIs precisam estar ClusterIP.
Há cobrança sobreposta durante a migração. Preserve qualquer controlador antigo
até excluir os recursos que ele gerencia. Nginx inicia com uma réplica para economizar;
reinício ou falha do node pode interromper temporariamente a entrada. Requests
iniciais de 50m/32Mi e limites de 250m/128Mi precisam ser medidos sob carga.
Mudanças no ConfigMap alteram o checksum do pod e provocam rollout na release.

Validação local, sem AWS:

```bash
python -m pip install 'PyYAML>=6,<7'
python -m unittest discover -s orchestration/gateway/tests -v
# Com Docker ativo, inclua os testes reais do proxy:
DOCKER_GATEWAY_TEST=1 python -m unittest discover -s orchestration/gateway/tests -v
```
