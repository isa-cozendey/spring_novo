# Bikes

Projeto acadêmico de backend desenvolvido em Java 21 e Spring Boot para gerenciamento de uma locadora de bicicletas.

A aplicação disponibiliza uma API REST para administrar usuários, clientes, bicicletas, locações, devoluções e relatórios. O frontend incluído no repositório é simples e existe apenas para testes manuais e demonstração das principais operações. O foco principal do projeto é o backend.

## Funcionalidades

- Cadastro e autenticação de usuários com Spring Security e JWT.
- Controle de acesso por perfis `ROLE_ADMIN` e `ROLE_CLIENTE`.
- Cadastro e consulta de clientes.
- Cadastro, alteração e consulta de bicicletas.
- Solicitação e efetivação de locações.
- Registro de devoluções e cálculo de valores.
- Geração de relatório de locações em JSON e PDF.
- Documentação interativa dos endpoints com Swagger/OpenAPI.
- Testes de integração com banco H2.

## Tecnologias

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Security
- JWT
- Spring Data JPA e Hibernate
- MySQL
- H2
- Bean Validation
- MapStruct
- SpringDoc OpenAPI
- JasperReports
- Maven Wrapper
- Vite e JavaScript no frontend de demonstração

## Estrutura do projeto

```text
src/main/java/br/edu/ifc/bikes/
├── config/             Configurações de segurança, auditoria e documentação
├── dto/                Objetos de entrada e saída da API
├── dto/mapper/         Conversão entre entidades e DTOs
├── entity/             Entidades persistidas pelo JPA
├── exception/          Exceções de domínio
├── jwt/                Autenticação e geração de tokens JWT
├── repository/         Acesso ao banco com Spring Data JPA
├── service/            Regras de negócio
└── web/
    ├── controller/     Endpoints REST
    └── exception/      Tratamento global de erros HTTP

src/main/resources/
├── application.properties
└── reports/            Template JRXML do relatório

src/test/               Testes de integração e dados SQL de teste
frontend/               Interface simples para demonstração e testes
```

## Arquitetura

A aplicação segue uma organização em camadas:

```text
Cliente HTTP
  -> Controller
  -> Service
  -> Repository
  -> Banco de dados
```

- **Controllers** recebem as requisições HTTP, validam os dados dos DTOs e chamam os serviços.
- **Services** concentram as regras de negócio e o controle das transações.
- **Repositories** realizam a persistência usando Spring Data JPA.
- **Entities** representam as tabelas e seus relacionamentos.
- **DTOs e mappers** definem os dados expostos pela API e evitam o uso direto das entidades nas respostas.
- **Exception handler** converte exceções conhecidas em respostas HTTP padronizadas.

## Modelo funcional

### Usuários

Os usuários possuem autenticação por nome de usuário e senha. Os perfis disponíveis são:

- `ROLE_CLIENTE`: pode operar sobre seu próprio cadastro e suas locações.
- `ROLE_ADMIN`: pode administrar clientes, bicicletas, locações e relatórios.

O cadastro público cria uma conta de cliente. Operações administrativas devem ser realizadas por uma conta já autorizada como administradora.

### Clientes

Cada cliente representa os dados pessoais associados a um usuário da aplicação.

### Bicicletas

Uma bicicleta possui código único e um status:

- `LIVRE`
- `OCUPADA`
- `EM_MANUTENCAO`

Administradores podem cadastrar, consultar, alterar e remover bicicletas. O status determina se ela pode ser usada em uma nova locação.

### Locações

Uma locação registra o cliente, a bicicleta, a data prevista de devolução e o estado da operação. O processo principal é:

1. Solicitação da locação por um cliente.
2. Seleção de uma bicicleta disponível.
3. Efetivação da locação.
4. Registro da devolução.
5. Liberação da bicicleta ou alteração para manutenção.

Dados completos de cartão não são persistidos. São armazenados somente o titular, a validade e os quatro últimos dígitos.

### Devoluções

Na devolução, o sistema registra data e hora, duração e valor correspondente. A tarifa padrão é R$ 5,00 por hora e pode ser configurada pela propriedade `bikes.locacao.tarifa-hora`.

## API

A base da API é:

```text
http://localhost:8080/api/v1
```

### Endpoints principais

| Recurso | Método | Caminho | Acesso |
|---|---|---|---|
| Usuário | `POST` | `/usuarios` | Público |
| Autenticação | `POST` | `/auth` | Público |
| Usuários | `GET` | `/usuarios` | Admin |
| Usuário | `GET` | `/usuarios/{id}` | Admin ou proprietário |
| Clientes | `POST`, `GET` | `/clientes` | Autenticado/admin |
| Cliente atual | `GET` | `/clientes/me` | Cliente autenticado |
| Bicicletas | `POST`, `GET`, `PUT`, `DELETE` | `/bicicletas` | Admin |
| Bicicleta por código | `GET` | `/bicicletas/{codigo}` | Admin |
| Solicitações de locação | `POST` | `/locacoes/solicitacoes` | Cliente/admin |
| Efetivação da locação | `POST` | `/locacoes/{id}/efetivar` | Cliente proprietário |
| Devolução | `POST` | `/locacoes/{id}/devolver` | Cliente/admin |
| Locações do usuário | `GET` | `/locacoes/me` | Cliente autenticado |
| Devoluções | `GET`, `POST` | `/devolucoes` | Admin/cliente relacionado |
| Relatório JSON | `GET` | `/relatorios/locacoes` | Admin |
| Relatório PDF | `GET` | `/relatorios/locacoes/pdf` | Admin |

Os caminhos completos e os modelos de entrada e saída estão disponíveis no Swagger após iniciar a aplicação.

## Autenticação

A autenticação devolve um token JWT. Esse token deve ser enviado no cabeçalho das requisições protegidas:

```http
Authorization: Bearer <token>
```

Exemplo de criação de usuário:

```bash
curl -X POST http://localhost:8080/api/v1/usuarios \
  -H 'Content-Type: application/json' \
  -d '{"username":"cliente@exemplo.com","password":"123456"}'
```

Exemplo de autenticação:

```bash
curl -X POST http://localhost:8080/api/v1/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"cliente@exemplo.com","password":"123456"}'
```

## Execução

### Pré-requisitos

- JDK 21
- MySQL
- Node.js e npm para executar o frontend demonstrativo

O datasource padrão está configurado em `src/main/resources/application.properties`. Antes da execução, ajuste a URL, o usuário e a senha do MySQL. O projeto cria ou atualiza o esquema com `spring.jpa.hibernate.ddl-auto=update`; para uma aplicação em produção, esse comportamento deveria ser substituído por migrations versionadas.

### Backend

Na raiz do repositório:

```bash
./mvnw spring-boot:run
```

No Linux, caso o arquivo não esteja executável:

```bash
bash ./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

### Documentação da API

Com o backend em execução:

```text
http://localhost:8080/docs-bikes.html
```

### Frontend de demonstração

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

A interface estará disponível em `http://localhost:5173`. O backend precisa estar em execução para que as chamadas sejam concluídas.

## Testes

Execute todos os testes com:

```bash
./mvnw test
```

Os testes de integração iniciam a aplicação e verificam endpoints relacionados a usuários, clientes, bicicletas, locações, devoluções e relatórios. O ambiente de testes utiliza H2 e scripts SQL localizados em `src/test/resources/sql`.

## Banco de dados

A aplicação usa MySQL no ambiente de execução padrão e H2 nos testes.

O Hibernate está configurado para atualizar o esquema a partir das entidades. As principais entidades são:

- `Usuario`
- `Cliente`
- `Bicicleta`
- `Locacao`
- `Devolucao`

O sistema também utiliza auditoria JPA para registrar dados de criação e alteração em entidades elegíveis.

## Contexto acadêmico

Este repositório foi desenvolvido como um projeto acadêmico para estudo e demonstração de conceitos de desenvolvimento backend com Spring. A implementação também inclui recursos de segurança, persistência, testes e geração de documentos.

O frontend não possui o nível de refinamento ou de complexidade esperado em uma aplicação comercial. Ele foi mantido como uma interface pequena para facilitar testes visuais e demonstrações das operações oferecidas pela API.
