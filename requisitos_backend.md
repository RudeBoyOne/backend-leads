# Parte 2 - Backend com Kotlin + Spring Boot + Spring Security

## Crie o projeto base Spring Boot com Kotlin, contendo as dependências para Spring Web, Spring Data JPA, Validation, Spring
Security e banco de dados relacional a sua escolha e faça:

 - a) Implemente entidades a sua escolha, sendo que a quantidade de entidades deverá ser duas entidades para cada
integrante, desde que cada uma delas possuam ao menos 5 campos já incluindo o id. Dentre estes campos deve haver
ao menos 3 tipos de dados entre estes a seguir (String, Boolean, LocalDate, LocalTime, LocalDateTime, Int, Double ou
Float);

 - b) Crie os repositórios JPA para as entidades criadas, e coloque as annotations nestas entidades para determinar que são
entidades e definir os ids;
 - c) Implemente as classes de serviços para estas entidades, com lógica básica de CRUD, ou seja, contendo funções para
criar, listar, buscar por id, atualizar e remover registros;
 - d) Crie os controladores REST para estas entidades, com as rotas típicas (GET, POST, PUT, DELETE), faça uma rota GET para
pegar um elemento pelo id e outra rota GET para listar todos os elementos da entidade;
 - e) Crie um controlador REST de autenticação que deve receber rotas do tipo /login e /register, este controlador deve
validar o usuário e senha recebidos por meio do Spring Security e deve acionar a biblioteca JWT para gerar um token
caso os dados sejam válidos, retornando este token no Response desta requisição HTTP;
 - f) Faça uma biblioteca de utilitários para JWT de forma a gerar e validar tokens, podendo ler o usuário do token validado;
 - g) Crie uma classe de configuração do Spring Security, criando Beans para passwordEncoder, AuthenticationManager,
configuração de autorização das rotas no HttpSecurity, este último deve ter regras para permitir o acesso sem
autenticação as rotas do tipo /login e /register e com autenticação as demais rotas
 - h) Implemente validações de entrada com Bean Validation, como por exemplo nome obrigatório e preço maior que zero,
sendo ao menos 2 (duas) validações para cada entidade;
 - i) Documente a API com Swagger/OpenAPI e envie uma captura de tela mostrando os endpoints funcionando, mostre as
chamadas sendo feitas para os controllers e suas respectivas respostas
