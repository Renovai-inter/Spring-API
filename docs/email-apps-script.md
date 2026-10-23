# Envio de e-mail gratuito pelo Google Apps Script

O plano gratuito do Render bloqueia as portas de SMTP (25, 465 e 587), então a API não consegue
enviar e-mail direto pelo Gmail. Em vez disso, ela chama por HTTPS um script do Google na conta que
envia os e-mails (ex.: `renovai.suporte@gmail.com`), e o script manda pelo Gmail dessa conta.

- Custo: grátis. Limite do Google para contas Gmail comuns: cerca de 100 destinatários por dia.
- O remetente aparece como a conta dona do script.

## 1. Criar o script (uma vez)

1. Entre em <https://script.google.com> **com a conta que vai enviar os e-mails**.
2. **Novo projeto**, apague o conteúdo e cole:

```javascript
// Troque pelo mesmo valor que vai na variável MAIL_SCRIPT_SEGREDO do Render.
const SEGREDO = 'TROQUE-POR-UM-TEXTO-LONGO-E-ALEATORIO';

function doPost(e) {
  try {
    const dados = JSON.parse(e.postData.contents);
    if (dados.segredo !== SEGREDO) {
      return resposta({ ok: false, erro: 'segredo inválido' });
    }
    GmailApp.sendEmail(dados.para, dados.assunto, dados.texto, {
      htmlBody: dados.html,
      name: dados.nome || 'Renovaí',
    });
    return resposta({ ok: true });
  } catch (erro) {
    return resposta({ ok: false, erro: String(erro) });
  }
}

function resposta(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

// Rode esta função uma vez pelo editor para autorizar o envio pelo Gmail.
function autorizar() {
  GmailApp.sendEmail(Session.getActiveUser().getEmail(), 'Teste Renovaí', 'Funcionou!');
}
```

3. Troque o valor de `SEGREDO` por um texto longo e aleatório e salve.
4. Selecione a função `autorizar` e clique em **Executar**. Aceite as permissões (em "O Google não
   verificou este app", clique em **Avançado → Acessar**). Chega um e-mail "Teste Renovaí".
5. **Implantar → Nova implantação → tipo "App da Web"**:
   - Executar como: **Eu**
   - Quem pode acessar: **Qualquer pessoa**
6. Copie a **URL do app da Web** (termina em `/exec`).

Se mudar o código do script depois, use **Implantar → Gerenciar implantações → editar → Nova
versão**, para manter a mesma URL.

## 2. Configurar no Render

Em **Environment** do serviço da API, adicione e salve (o Render reinicia sozinho):

| Variável | Valor |
|---|---|
| `MAIL_SCRIPT_URL` | a URL `/exec` do passo 6 |
| `MAIL_SCRIPT_SEGREDO` | o mesmo texto do `SEGREDO` do script |

Com essas duas variáveis, a API usa o script e ignora o SMTP (`MAIL_USERNAME`/`MAIL_PASSWORD`).
