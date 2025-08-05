# Integração de Leads via WebHook

## Configuração do Webhook
Para receber leads do GrupoZAP, seu sistema deve estar preparado para receber requisições HTTP POST com os seguintes parâmetros:

### Headers obrigatórios
```
User-Agent: grupozap-notification-api
Authorization: Basic [credenciais-em-base64]
````

> Timeout As requisições têm timeout de 30 segundosRequisições que excederem este tempo serão consideradas falhas e sujeitas a retentativas

## Configuração de Endpoints.

Padrões de URL validos
 - Com ID do anunciante na path:
    - https://seudominio.com.br/grupozap/lead/ID-DO-ANUNCIANTE
    - https://crm.imobiliaria.com.br/grupozap/lead/123456

 - Com ID do anunciante no subdomínio:
    - https://ID-DO-ANUNCIANTE.seudominio.com.br/grupozap/lead
    - https://imobiliariaxpto.crm.com.br/grupozap/lead

- Sem ID do anunciante (opcional):
    - https://seudominio.com.br/grupozap/lead

## Segurança 
Autenticação Basic Auth, Todas as requisições incluem um header de autenticação no formato:
    Authorization: Basic dml2YXJlYWw6NTk0RjgwM0IzODBBNDEzOTZFRDYzRENBMzk1MDM1NDI=

**EU SO PEGUEI O CODIGO NODEJS DE EXEMPLO DA DOC E PEDI PRO GEPETO FAZER UM EXEMPLO EM JAVA , !NAO USE SEM VERIFICARA ANTES!**
```
    @RestController
@RequestMapping("/grupozap/lead")
public class LeadController {

    private static final String SECRET_KEY = "594F803B380A41396ED63DCA39503542";

    @PostMapping
    public ResponseEntity<?> receiveLead(@RequestHeader("Authorization") String authorizationHeader) {
        // 1. Verify Authorization header exists
        if (authorizationHeader == null || !authorizationHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 2. Extract and decode Base64 value
            String base64Credentials = authorizationHeader.substring("Basic ".length());
            byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes);
            
            // 3. Validate SECRET_KEY
            String[] values = credentials.split(":", 2);
            if (values.length != 2 || !SECRET_KEY.equals(values[1])) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // If validation passes, continue processing
            return ResponseEntity.ok().build();
            
        } catch (IllegalArgumentException e) {
            // Handle Base64 decoding errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid authorization format");
        }
    }
}
```
> ### Caso a chave não for a que enviamos, deverá retornar response statusCode 401, provavelmente a requisição não foi realizada pelo GrupoZap

# Envio de Leads

o controle de sucesso de envio das leads é feito pelos protocolos http:
2xx: sucesso e açoes extras nao sao necessarias 
3xx, 4xx e 5xx indicam que houve falha no processamento da lead por parte do software/crm

### Contrato Json
```
{
  "leadOrigin": "Grupo OLX",
  "timestamp": "2017-10-23T15:50:30.619Z",
  "originLeadId": "59ee0fc6e4b043e1b2a6d863",
  "originListingId": "87027856",
  "clientListingId": "a40171",
  "name": "Nome Consumidor",
  "email": "nome.consumidor@email.com",
  "ddd": "11",
  "phone": "999999999",
  "phoneNumber": "11999999999",
  "message": "Olá, tenho interesse neste imóvel. Aguardo o contato. Obrigado.",
  "temperature": "Alta",
  "transactionType": "SELL"
}

```