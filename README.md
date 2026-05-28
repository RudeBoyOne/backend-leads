# Backend-leads

## Pré-requisitos

Antes de iniciar a aplicação, certifique-se de ter instalado:

- **Java 21 (JDK 21+)**
- **Maven 3.9.11 ou superior**
- **Docker** e **Docker Compose**

---

## Passo a Passo para iniciar a aplicação

### 1. Iniciar a aplicação

```bash
# Criar a rede Docker (necessário apenas na primeira vez)
docker network create lead-network

# Iniciar PostgreSQL + aplicação
docker compose up -d
```

A aplicação estará disponível em [http://localhost:8080](http://localhost:8080).

### 2. Desenvolvimento local (sem Docker)

```bash
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

---

## Outros Comandos Úteis

```bash
docker compose down          # parar containers
docker compose down -v       # parar e remover volume do banco
./mvnw compile               # compilar + formatar código
./mvnw spotless:apply        # formatar código manualmente
./mvnw spotless:check        # verificar formatação
```

---

## Formatação de Código com Spotless

Este projeto utiliza o plugin **Spotless** para garantir a formatação consistente do código seguindo o padrão **Google Java Format**.

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

### **SEMPRE FORMATAR O CÓDIGO ANTES DE COMMITAR!**

A formatação é aplicada automaticamente durante o `compile`. Você também pode executar manualmente:

```bash
./mvnw spotless:apply
```
