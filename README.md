# Gerenciador de Leads Imobiliários

- Link do Repositório Backend: <https://github.com/RudeBoyOne/backend-leads>
- Link do Repositório Frontend: <https://github.com/RudeBoyOne/frontend-leads>

Nossa aplicação tem como objetivo servir para empresas do ramo imobiliário, uma aplicação que forneça a possibilidade de uma centralização de serviços internos, como: receber leads dos portais de imóveis, distribui-los para os corretores responsáveis, fazer o atendimento e acompanhamento do processo de vendas, gerar insights e relatórios e gerenciamento de equipes.

## Tecnologias utilizadas

- Kotlin 2.3.21
- JVM 25
- Spring Boot 4.1.1
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Validation
- PostgreSQL 16
- Flyway
- Gradle Wrapper
- Docker e Docker Compose

---

### Integrantes e entidades

| Integrante | Entidade 1 | Entidade 2 |
| --- | --- | --- |
| Allan Chagas | PropertySensor | SensorMetric |
| Gabriel Alves | Property | Lead |
| Lucas Ferreira | RealStateAgency | BranchOffice |
| Rafael Mateus | Team | Activity |
| Ygor Lopes | Role | User |

---

## Rodando o projeto

São necessários o **JDK 25** e o **Docker** com Docker Compose. Gradle não precisa ser instalado: o
wrapper (`./gradlew`) resolve a versão sozinho.

O compose depende de uma rede externa chamada `lead-network`, criada uma única vez:

```bash
docker network create lead-network
docker compose up -d
```

O PostgreSQL e a aplicação sobem juntos, e a aplicação responde em `:8080`.

Quem já tiver um PostgreSQL acessível em `localhost:5432` pode rodar sem o Docker:

```bash
./gradlew bootRun
```

---

## Organização do código

O código vive em `src/main/kotlin`, sob o pacote `com.br.semesperanca.backendleads`, em quatro
camadas:
```
application/   controllers REST e DTOs (data classes)
domain/        entidades JPA, repositories e services
configuration/ segurança, tratamento de exceções, e-mail, Swagger, async
```
