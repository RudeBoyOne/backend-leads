# Integração de Leads via WebHook

## Configuração do Webhook
Para receber leads , o sistema deve estar

### Headers obrigatórios
```
User-Agent: grupozap-notification-api
Authorization: Basic [credenciais-em-base64]
````

> Timeout As requisições têm timeout de 30 segundos . Requisições que excederem este tempo serão consideradas falhas e sujeitas a retentativas

## Padrao de Endpoints.

Padrões de URL validos
 - Com ID do anunciante na path:
    - https://seudominio.com.br/grupozap/lead/ID-DO-ANUNCIANTE
    - https://crm.imobiliaria.com.br/grupozap/lead/123456

 - Com ID do anunciante no subdomínio:
    - https://ID-DO-ANUNCIANTE.seudominio.com.br/grupozap/lead
    - https://imobiliariaxpto.crm.com.br/grupozap/lead

- Sem ID do anunciante (opcional):
  > ID-DO-ANUNCIANTE, é a representaçao de quem ira receber a requisição, é um forma de identificaçao do clinte no Software/Crm, caso nao tenha o ID-DO-ANUNCIANTE o endpoint podera ser implementado sem ele,Exemplo:
    - https://seudominio.com.br/grupozap/lead

    

## Segurança 

O tipo de Autenticação é a [Basic Auth](https://en.wikipedia.org/wiki/Basic_access_authentication), Todas as requisições devem incluir um header com o campo Authorization, e a `secret key` como valor, exemplo:

    Authorization: Basic dml2YXJlYWw6NTk0RjgwM0IzODBBNDEzOTZFRDYzRENBMzk1MDM1NDI=

>  Caso a chave não for a chave enviada pelo grupo zap, deverá retornar response statusCode 401, provavelmente a requisição não foi realizada pelo GrupoZap


## Validaçao Da Secret_KEY

> Lembrando que a SECRET_KEY é por CRM não é por cliente, ou seja, não deve ser utilizada para identificação do cliente (anunciante) e sim para validar se a requisição está sendo realizado pelo GrupoZap.
a documentaçao disponibiliza um exemplo em NodeJs:

- Obter o header Authorization;
````javascript

    const authorization = req.headers.Authorization;
    // pegando o Authorization do header da requiçao 
    // a const authorization recebe o valor bruto da secretkey:
    // valor da secretkey : `Basic dml2YXJlYWw6NTk0RjgwM0IzODBBNDEzOTZFRDYzRENBMzk1MDM1NDI=`
````

- Ignore Basic e decodifique somente o valor;
````javascript
    const base64 = authorization.split(' ')[1];
    // tirando o inicio da Secretkey onte ta escrito "Basic "
    // valor da secretkey : `dml2YXJlYWw6NTk0RjgwM0IzODBBNDEzOTZFRDYzRENBMzk1MDM1NDI=`

    const value = new Buffer(base64, 'base64').toString('utf-8');
    // Essa linha de código está decodificando uma string que está em base64 para texto em UTF-8
    // valor da secretkey : vivareal:594F803B380A41396ED63DCA39503542
````

- Valide a SECRET_KEY;
    ````javascript
    // 'vivareal:594F803B380A41396ED63DCA39503542'
    const secretKey = value.split(':')[1];
    //  tirando o identificador do CRM da secret key
    // valor da secretkey : 594F803B380A41396ED63DCA39503542

    if (secretKey !== SECRET_KEY) {
        return res.sendStatus(401);
    }
    ````

# Como funciona o envio de leads
Envio : Os leads são enviados pela grupo OLX via HTTP POST, em formato JSON, para nosso endpoint homologado.

.Após processar as lead conforme os nossos requisitos, retornamos um status code à OLX, que interpreta essa resposta para fornecer o feedback adequado ao cliente.


  **Processo: Cada lead é enviado individualmente, sempre que houver um novo contato**.
- Confirmação de recebimento:
    - Status 2xx : ( OLX entende que)  A Lead foi recebido com sucesso, nenhuma ação extra é necessária.
    - Status 3xx, 4xx ou 5xx: Falha no processamento do lead.
  


 - Retentativas:
    - Se não receber status 2xx, o sistema tenta reenviar o lead até 3 vezes.
    - Se ainda assim não for entregue, o lead fica armazenado por até 14 dias e pode ser reenviado a pedido.


- Importante:
    - O controle de recebimento é feito apenas pelo código de status HTTP.
    - Qualquer informação no corpo da resposta será ignorada.


- [Respostas Informativas](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_informativas) (100 – 199)

- [Respostas bem-sucedidas](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_bem-sucedidas) (200 – 299)

- [Mensagens de redirecionamento](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#mensagens_de_redirecionamento) (300 – 399)

- [Respostas de erro do cliente](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_de_erro_do_cliente) (400 – 499)

- [Respostas de erro do servidor](https://developer.mozilla.org/pt-BR/docs/Web/HTTP/Reference/Status#respostas_de_erro_do_servidor) (500 – 599)


### Contrato Json
````JSON
    {
    "leadOrigin": "Grupo OLX",
    "timestamp": "2017-10-23T15:50:30.619Z",
    "originLeadId": "59ee0fc6e4b043e1b2a6d863",
    "originListingId": "87027856",
    "clientListingId": "a40171",
    "name": "Givaldo sem teto da silva",
    "email": "queroumacasapeloamorddeus@email.com",
    "ddd": "11",
    "phone": "999999999",
    "phoneNumber": "11999999999",
    "message": "oi quer casa",
    "temperature": "Alta",
    "transactionType": "SELL"
    }
````
### Dicioniario do contrato

- leadOrigin1: Identificador da origem do lead (Grupo OLX);
- timestamp: Data e horário da criação do lead no formato ISO_LOCAL_DATE_TIME;
- originLeadId: Identificador do lead do GrupoZap;
- originListingId: Identificador do anúncio do GrupoZap;
- clientListingId: Identificador do anúncio para o anunciante (ListingId);
- name: Nome do consumidor que gerou o lead;
- email: E-Mail do consumidor que gerou o lead;
- ddd: DDD do telefone do consumidor que gerou o lead Ex:11;
- phone: Telefone do consumidor que gerou o lead Ex:999999999;
- phoneNumber: [deprecado] Telefone do consumidor que gerou o lead (DDD + Phone) Ex:11999999999;
- message2: Mensagem do consumidor que gerou o lead;
- temperature: Temperatura do lead (Baixa, Média, Alta);
- transactionType: Tipo de transação (RENT, SELL);
  
# Integração por E-mail

---

## Visão Geral

A integraçao por email permite a extraçao automatica de dados das leads novas, Permite coletar dados Estruturados diretamente do email.

---

## Como Funciona

A automação é baseada no uso de **atributos HTML `title`** em campos específicos do e-mail. Cada informação relevante (como nome, e-mail, etc.) é mapeada com um `title` único.

### Padrão de Nomenclatura dos Campos

`[entidade]-[campo]`

* **`[entidade]`**: Categoria da informação (ex: `lead` ou `listing`).
* **`[campo]`**: Informação específica (ex: `name`, `email`, `address`).

---

## Campos Disponíveis para Mapeamento

| Descrição do Campo        | Atributo (`title`)     |
| ------------------------- | ---------------------- |
| **Informações do lead**   |                        |
| Nome do Lead              | `lead-name`            |
| E-mail do Lead            | `lead-email`           |
| Telefone do Lead          | `lead-phoneNumber`     |
| Qualificação do Lead      | `lead-score`           |
| **Informações do imóvel** |
| Imagem do imóvel          | `listing-image`        |
| Código do imóvel          | `listing-externalCode` |
| Bairro do imóvel          | `listing-neighborhood` |
| Endereço do imóvel        | `listing-address`      |
| Preço do imóvel           | `listing-price`        |

**Atenção:** As informações de histórico do lead, como tempo de busca (`history-timeLine`) e bairros pesquisados (`history-neighborhoods`),estão indisponíveis.

---

##  Exemplo de Extração de Dados

Sua ferramenta de integração pode usar seletores para localizar os atributos `title` e extrair o conteúdo correspondente.

**Exemplo em JavaScript:**

```javascript
// Seleciona o elemento com o atributo title="lead-email"
const emailElement = document.querySelector('[title="lead-email"]');

// Extrai o texto do elemento, removendo espaços em branco
const leadEmail = emailElement ? emailElement.textContent.trim() : null;

console.log("E-mail do Lead:", leadEmail);
```

## Ajuda adicional

[repo do github](
    https://github.com/olxbr/crm-lead-integration/blob/master/samples/java/README.md
    )

[Validador de endpoint](
    https://developers.grupozap.com/webhooks/endpoint_validator
)
