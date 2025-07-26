# Backend-leads

## Pré-requisitos

Antes de iniciar a aplicação, certifique-se de ter instalado:

- **Java 21 (JDK 21+)**
- **Maven 3.9.11 ou superior**
- **Docker** e **Docker Compose**

---

## Passo a Passo para iniciar a aplicação

### 1. Configuração do Banco de Dados

A aplicação utiliza o banco de dados **PostgreSQL** configurado através do arquivo `docker-compose.yml`. Para iniciar o
banco:

1. Certifique-se de que o **Docker** está instalado e em execução.
2. No diretório raiz do projeto, execute o seguinte comando para inicializar o banco de dados:

   ```bash
   docker-compose up -d
   ```

   Isso criará e inicializará um container do PostgreSQL acessível pela porta **5432**

> **Nota:** O volume `pgdata` é utilizado para persistir os dados do banco mesmo que o container seja parado.

---

### 2. Compilar e executar a aplicação

1. No diretório raiz do projeto, compile o código utilizando o Maven Wrapper:

   ```bash
   ./mvnw clean install
   ```

   No Windows, utilize:

   ```bash
   mvnw.cmd clean install
   ```

2. Após a compilação, execute a aplicação com o comando:

   ```bash
   ./mvnw spring-boot:run
   ```

   No Windows, utilize:

   ```bash
   mvnw.cmd spring-boot:run
   ```

---

### 3. Verificar o funcionamento

Se tudo estiver corretamente configurado, a aplicação será iniciada no endereço padrão:

- URL: [http://localhost:8080](http://localhost:8080)

---

## Outros Comandos Úteis

### Parar o Banco de Dados

Para parar o container do PostgreSQL, execute:

   ```bash
     docker-compose down
   ```

### Limpar Banco de Dados Persistente

Se precisar remover os dados persistidos (volume), utilize:

   ```bash
     docker-compose down -v
   ```

---

## Formatação de Código com Spotless

Este projeto utiliza o plugin **Spotless** para garantir a formatação consistente do código seguindo o padrão **Google
Java Format**.

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

### **_SEMPRE FORMATAR O CÓDIGO ANTES DE COMMITAR!_**

### Como usar o Spotless

1. **Corrigir formatação**:
   Para corrigir automaticamente a formatação do código, execute:

   ```bash
    ./mvnw spotless:apply
   ```
   ou no Windows:

    ```bash
     mvnw.cmd spotless:apply
    ```
   ou

   ```bash
    ./mvnw compile
   ```
   ou no Windows:

    ```bash
     mvnw.cmd compile
    ```
