# Integraçao Leads grupo OLX
// header é definido o user-agent e o content-type qualquer requisiçao tem 30s de timeout

## endpoints validos:  
 - https://{odomino}/grupozap/lead/{ID-DO-ANUNCIANTE}
 - https://{ID-DO-ANUNCIANTE}.{yourdomain.com.br}/grupozap/lead

ID-DO-ANUNCIANTE é parte da URL que representa o identificador do anunciante no sistema que irá receber a requisição. É uma forma de identificação do cliente no Software/CRM.
pode ser implementado sem o id do identificador


## Segurança
 basic auth exemplo:
 Authorization: Basic dml2YXJlYWw6NTk0RjgwM0IzODBBNDEzOTZFRDYzRENBMzk1MDM1NDI=
o header deve conter o Authorization com o uma SECRET_KEY, que é o emissor da requisiçao + numeros da secret key, tanto o emisor quanto os numeros devem estar cidificados em base64

## Payload de envio
![img.png](img.png)!


# como o payload é enviado?? pra onde ele é enviado? onde é validado o Authorization?
# qual a endpoint faz isso?

TODO: perguntar ao gabriel sobre o canal pro 
'