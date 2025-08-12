# Integração com a API do Grupo ZAP (Canal Pro)

## habilitaçao da integraçao 
Para integrar com a plataforma do Grupo ZAP (OLX Imóveis) via Canal Pro, seu software/CRM deve ser homologado através do:  
[Formulário de Homologação](https://docs.google.com/forms/d/e/1FAIpQLSd6WJ3xw-qoFzW2-6OvrEihTjurUwVsJYei-P4alae2S1yedQ/viewform)

## Credenciais Importantes
Após aprovação, serão fornecidas:
- `X-API-KEY`: Chave de acesso à API  
- `X-Agent-Name`: Identificação do seu sistema/CRM  
- `Secret`: Usado para autenticação em webhooks  

---

## Envio de Leads via API
**Endpoint**:  
`POST https://crm-leadmanager-leadreceiver-api.gestao.prod.olxbr.io/v1/addLeads`

### Headers obrigatórios:

    X-API-KEY: [chave-api]
    X-Agent-Name: [agent-name]
    accept: application/json
    Content-Type: application/json

## Corpo da requisição (JSON)
``````JSON
{
  "LeadName": "string",
  "LeadEmail": "string",
  "LeadTelephone": "string",
  "Message": "string",
  "ExternalId": "string",
  "BusinessType": ["SALE", "RENTAL"],
  "BrokerEmail": "string",
  "LeadOrigin": "string"
}
``````

## Resposta
as respostas sao controladas pelos protocolos HTTP padrao 

- [Respostas Informativas](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_informativas) (100 – 199)

- [Respostas bem-sucedidas](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_bem-sucedidas) (200 – 299)

- [Mensagens de redirecionamento](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#mensagens_de_redirecionamento) (300 – 399)

- [Respostas de erro do cliente](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_de_erro_do_cliente) (400 – 499)

- [Respostas de erro do servidor](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_de_erro_do_servidor) (500 – 599)


## Documentação adicional

[Swagger para testes](https://crm-leadmanager-leadreceiver-api.gestao.prod.olxbr.io/swagger-ui/index.html#/add-lead-controller/postLead)

