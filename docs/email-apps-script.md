# Envio de e-mail gratuito pelo Google Apps Script

O plano gratuito do Render bloqueia as portas de SMTP (25, 465 e 587), então a API não envia
e-mail direto pelo Gmail. Ela chama por HTTPS um Web App do Google Apps Script na conta
`renovai.suporte@gmail.com`, e o script envia pelo Gmail dessa conta.

- **Já vem ligado:** a URL do Web App está no código (`EmailService.SCRIPT_PADRAO`). Não precisa
  configurar nada no Render. Para usar outro script: `MAIL_SCRIPT_URL=<url>`; para desligar e usar
  SMTP: `MAIL_SCRIPT_URL=-` (com `MAIL_USERNAME`/`MAIL_PASSWORD`).
- **Sem segredo, mas sem abuso:** a API manda só `{ para, codigo, validade }`. O script valida o
  e-mail e o código (6 dígitos), monta o e-mail com o modelo do Renovaí e não aceita assunto nem
  HTML de fora. Também limita a 1 e-mail por endereço a cada 60 s e 90 por dia.
- **Custo:** grátis (limite do Gmail: cerca de 100 e-mails por dia).
- **Modelo:** o mesmo de `src/main/resources/templates/email/codigo-redefinicao.html` (usado no
  SMTP). Mudou um, mude o outro.

## Projeto atual

- Projeto: "Renovai - Envio de e-mail" em <https://script.google.com>, conta `renovai.suporte@gmail.com`.
- Implantação: App da Web, executar como **Eu**, acesso **Qualquer pessoa**.

## Atualizar o script

1. Cole o código abaixo no editor e salve.
2. **Implantar → Gerenciar implantações → lápis → Versão: Nova versão → Implantar** (mantém a URL).
3. Para ver o e-mail sem passar pela API, rode a função `testarEnvio` no editor.

```javascript
// Renovaí - envio do código de redefinição de senha.
// Chamado pela API (EmailService) com { para, codigo, validade }. O e-mail é montado aqui,
// com o modelo do Renovaí: o script não aceita assunto nem HTML de fora, então a URL pública
// não serve para mandar outros e-mails. Mesmo modelo de templates/email/codigo-redefinicao.html.

const ESPERA_POR_EMAIL_SEG = 60;   // um código por e-mail a cada 60 s
const LIMITE_POR_DIA = 90;         // o Gmail gratuito permite cerca de 100 por dia

function doPost(e) {
  try {
    const dados = JSON.parse(e.postData.contents);
    const para = String(dados.para || '').trim().toLowerCase();
    const codigo = String(dados.codigo || '');
    const validade = parseInt(dados.validade, 10) || 15;

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(para)) return resposta({ ok: false, erro: 'e-mail inválido' });
    if (!/^\d{6}$/.test(codigo)) return resposta({ ok: false, erro: 'código inválido' });
    if (validade < 1 || validade > 60) return resposta({ ok: false, erro: 'validade inválida' });

    const cache = CacheService.getScriptCache();
    if (cache.get('espera:' + para)) return resposta({ ok: false, erro: 'aguarde para pedir outro código' });

    const props = PropertiesService.getScriptProperties();
    const hoje = 'enviados:' + Utilities.formatDate(new Date(), 'America/Sao_Paulo', 'yyyy-MM-dd');
    const enviados = parseInt(props.getProperty(hoje) || '0', 10);
    if (enviados >= LIMITE_POR_DIA) return resposta({ ok: false, erro: 'limite diário de e-mails atingido' });

    GmailApp.sendEmail(para, 'Seu código do Renovaí: ' + codigo,
      'Seu código para redefinir a senha do Renovaí é ' + codigo + '.\n' +
      'Ele vale por ' + validade + ' minutos. Se não foi você que pediu, ignore este e-mail.',
      { htmlBody: modelo(codigo, validade), name: 'Renovaí' });

    cache.put('espera:' + para, '1', ESPERA_POR_EMAIL_SEG);
    props.setProperty(hoje, String(enviados + 1));
    return resposta({ ok: true });
  } catch (erro) {
    return resposta({ ok: false, erro: String(erro) });
  }
}

function resposta(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function modelo(codigo, validade) {
  return MODELO.split('{{CODIGO}}').join(codigo).split('{{VALIDADE}}').join(String(validade));
}

// Rode pelo editor para ver o e-mail na própria caixa de entrada.
function testarEnvio() {
  GmailApp.sendEmail(Session.getActiveUser().getEmail(), 'Seu código do Renovaí: 123456', 'Teste',
    { htmlBody: modelo('123456', 15), name: 'Renovaí' });
}

const MODELO = `<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Seu código do Renovaí</title>
</head>
<body style="margin:0;padding:0;background-color:#EEF5EF;font-family:Arial,Helvetica,sans-serif;color:#1F2937;">
  <div style="display:none;max-height:0;overflow:hidden;">Seu código do Renovaí é {{CODIGO}}. Ele vale por {{VALIDADE}} minutos.</div>
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#EEF5EF;padding:28px 12px;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:520px;background-color:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
          <tr>
            <td style="background-color:#519059;padding:28px 24px 22px 24px;text-align:center;">
              <div style="font-size:34px;line-height:34px;color:#FFFFFF;">&#9851;</div>
              <div style="font-size:28px;font-weight:bold;color:#FFFFFF;letter-spacing:1px;margin-top:6px;">Renovaí</div>
              <div style="font-size:13px;color:#DCEFDF;margin-top:4px;">Reciclagem que conecta cooperativas e empresas</div>
            </td>
          </tr>
          <tr>
            <td style="height:6px;background-color:#882B4E;font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:30px 28px 6px 28px;">
              <h1 style="margin:0 0 12px 0;font-size:21px;color:#882B4E;">Vamos redefinir sua senha</h1>
              <p style="margin:0 0 22px 0;font-size:15px;line-height:23px;">
                Recebemos um pedido para trocar a senha da sua conta no Renovaí.
                Digite este código no aplicativo:
              </p>
            </td>
          </tr>
          <tr>
            <td align="center" style="padding:0 28px;">
              <div style="display:inline-block;background-color:#F0F7F1;border:2px dashed #519059;border-radius:12px;padding:18px 26px;font-size:36px;font-weight:bold;letter-spacing:12px;color:#2F6B38;font-family:'Courier New',Courier,monospace;">{{CODIGO}}</div>
              <p style="margin:10px 0 0 0;font-size:13px;color:#6B7280;">Vale por <strong style="color:#882B4E;">{{VALIDADE}} minutos</strong> e só pode ser usado uma vez.</p>
            </td>
          </tr>
          <tr>
            <td style="padding:26px 28px 6px 28px;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#FAF5F7;border-radius:10px;">
                <tr>
                  <td style="padding:16px 18px;font-size:14px;line-height:24px;color:#374151;">
                    <strong style="color:#882B4E;">Como usar</strong><br>
                    <span style="color:#519059;font-weight:bold;">1.</span> Volte para o app Renovaí<br>
                    <span style="color:#519059;font-weight:bold;">2.</span> Digite os 6 números acima<br>
                    <span style="color:#519059;font-weight:bold;">3.</span> Crie sua nova senha e entre de novo
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="padding:18px 28px 8px 28px;">
              <p style="margin:0 0 10px 0;font-size:13px;line-height:20px;color:#6B7280;">
                &#128274; Não compartilhe este código. A equipe do Renovaí nunca pede o seu código.
              </p>
              <p style="margin:0;font-size:13px;line-height:20px;color:#6B7280;">
                Não foi você? Pode ignorar este e-mail: sua senha continua a mesma.
              </p>
            </td>
          </tr>
          <tr>
            <td style="padding:22px 28px;border-top:1px solid #E5E7EB;text-align:center;font-size:12px;line-height:18px;color:#9CA3AF;">
              &#127807; Cada material reciclado volta para a economia e gera renda para quem coleta.<br>
              Dúvidas? Escreva para <a href="mailto:renovai.suporte@gmail.com" style="color:#882B4E;">renovai.suporte@gmail.com</a>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`;
```
