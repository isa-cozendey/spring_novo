# Bikes — API de aluguel de bicicletas

Projeto acadêmico de uma API REST para administrar usuários, clientes, bicicletas, locações, devoluções e relatórios. O backend é feito com Java 21 e Spring Boot. Há também um frontend simples, separado do backend, para testar as operações pela interface.

Este README foi escrito como guia de estudo e apresentação: explica a organização do código e acompanha o caminho de uma requisição desde a URL até o banco de dados.

## 1. Visão geral para apresentar

> “O projeto é uma API REST de aluguel de bicicletas. O Spring MVC recebe as requisições nos controllers; os services aplicam as regras do negócio; os repositories acessam o banco usando Spring Data JPA; e as entities representam as tabelas. A autenticação usa JWT e as permissões são verificadas por papel. Os dados trafegam em JSON, exceto o relatório, que também pode ser gerado em PDF com JasperReports.”

### Tecnologias principais

- **Java 21**: linguagem e versão configurada para compilação.
- **Spring Boot**: inicializa a aplicação e integra os componentes.
- **Spring Web MVC**: cria os endpoints REST.
- **Spring Data JPA / Hibernate**: mapeia objetos Java para tabelas e executa consultas.
- **MySQL**: banco configurado para a execução normal.
- **H2**: banco em memória usado pelos testes de integração.
- **Spring Security + JWT**: autenticação e autorização.
- **Bean Validation**: valida dados recebidos nos DTOs.
- **MapStruct**: gera implementações para converter entities em DTOs.
- **SpringDoc / Swagger UI**: documentação navegável da API.
- **JasperReports**: geração do relatório PDF.
- **Vite + JavaScript**: frontend separado para testes manuais.

## 2. Como a estrutura está organizada

Código principal: `src/main/java/br/edu/ifc/bikes/`.

```text
br.edu.ifc.bikes/
├── BikesApplication.java
├── config/
├── dto/
│   └── mapper/
├── entity/
├── exception/
├── jwt/
├── repository/
├── service/
└── web/
    ├── controller/
    └── exception/
```

### `BikesApplication.java`

É o ponto de entrada do Spring Boot. O método `main` chama `SpringApplication.run(...)`, que inicializa o servidor web, encontra os componentes e prepara a aplicação.

### `config/` — configuração

Classes que configuram o comportamento geral:

- `SpringSecurityConfig`: define a cadeia de segurança, desativa login por formulário e HTTP Basic, ativa sessão sem estado e libera os endpoints de cadastro de usuário e login.
- `SpringJpaAuditingConfig`: ativa auditoria automática e informa quem é o usuário autenticado que está fazendo uma alteração.
- `SpringTimezoneConfig`: define o fuso horário usado pela aplicação.
- `SpringDocOpenApiConfig`: configura Swagger/OpenAPI e o esquema Bearer JWT.

### `web/controller/` — entrada HTTP

Controllers recebem HTTP, interpretam caminho, método, parâmetros e corpo JSON, chamam um service e definem a resposta/status HTTP. Eles não devem concentrar as regras de negócio.

Exemplos: `UsuarioController`, `BicicletaController`, `LocacaoController`.

### `service/` — regras de negócio

Services implementam as ações do sistema. Por exemplo, `LocacaoService` verifica se há bicicleta livre, associa a locação ao cliente e controla a transição de status. `@Transactional` mantém operações relacionadas na mesma transação do banco.

### `repository/` — acesso a dados

Interfaces baseadas em `JpaRepository`. O Spring gera a implementação em tempo de execução. Métodos como `findByCodigo` e `findByClienteId` são interpretados pelo Spring Data JPA e viram consultas ao banco.

### `entity/` — modelo persistido

Classes anotadas com `@Entity` representam tabelas e relacionamentos:

- `Usuario`: credenciais e papel (`ROLE_ADMIN` ou `ROLE_CLIENTE`).
- `Cliente`: cadastro de cliente associado a um usuário.
- `Bicicleta`: código único e status (`LIVRE`, `OCUPADA`, `EM_MANUTENCAO`).
- `Locacao`: cliente, bicicleta, datas, status e dados não sensíveis do cartão.
- `Devolucao`: devolução efetiva, duração, valor e eventual problema.

Campos como `dataCriacao`, `dataModificacao`, `criadoPor` e `modificadoPor` são preenchidos pela auditoria JPA nas entities que a utilizam.

### `dto/` e `dto/mapper/` — dados de entrada e saída

DTO significa *Data Transfer Object*. São objetos usados na fronteira da API para receber ou devolver dados sem expor diretamente a entity do banco.

- DTOs `Request` representam dados enviados pelo cliente.
- DTOs `Response` representam dados retornados pela API.
- Anotações como `@NotBlank`, `@NotNull`, `@Size`, `@Pattern` e `@Future` validam os dados recebidos.
- Mappers MapStruct convertem entities em DTOs de resposta.

### `jwt/` — autenticação com token

- `AutenticacaoController` recebe usuário e senha e pede ao Spring Security para autenticar.
- `JwtUtils` cria e valida o token.
- `JwtAuthorizationFilter` lê o cabeçalho `Authorization` em cada requisição protegida.
- `JwtUserDetailsService` carrega o usuário e os papéis no banco.
- `JwtUserDetails` adapta o usuário ao modelo esperado pelo Spring Security.

O token é enviado nas requisições protegidas no formato:

```http
Authorization: Bearer <token>
```

### `exception/` e `web/exception/` — erros da API

As exceções de domínio ficam em `exception/`, por exemplo, bicicleta indisponível ou locação inválida. `ApiExceptionHandler`, em `web/exception/`, converte exceções conhecidas em respostas HTTP com um formato de erro (`ErrorMessage`), como `404`, `409` ou `422`.

### `src/main/resources/`

Contém configuração e recursos carregados pela aplicação:

- `application.properties`: nome, locale, datasource, JPA e Swagger.
- `reports/relatorio-locacoes.jrxml`: template JasperReports que define o layout do PDF.

### `src/test/`

Contém testes de integração. As classes `*IT` iniciam a aplicação em uma porta aleatória, fazem requisições HTTP de teste e verificam status e resposta. Os arquivos `src/test/resources/sql/` preparam e limpam dados de teste. A configuração de testes usa H2.

### `frontend/`

Aplicação de teste independente do backend, feita com JavaScript e Vite. Ela chama a API por `/api`; o proxy de desenvolvimento configurado em `vite.config.js` encaminha essas chamadas para `http://localhost:8080`.

### `pom.xml` e Maven Wrapper

`pom.xml` declara Java 21, dependências e plugins do Maven. `mvnw` e `mvnw.cmd` permitem executar o Maven sem precisar instalá-lo globalmente.

## 3. Caminho de uma requisição

Exemplo: cadastrar uma bicicleta.

1. O cliente envia `POST /api/v1/bicicletas` com JSON e token JWT.
2. O filtro JWT valida o token e identifica usuário e papel.
3. Spring Security verifica se o papel tem autorização; bicicletas são restritas a `ADMIN`.
4. `BicicletaController` recebe o JSON como `BicicletaRequestDTO`.
5. `@Valid` verifica as regras de validação do DTO.
6. O controller chama `BicicletaService`.
7. O service aplica regras, por exemplo, impedir código duplicado.
8. `BicicletaRepository` salva a entity via JPA/Hibernate.
9. O mapper converte a entity salva em `BicicletaResponseDTO`.
10. O controller retorna JSON e o status HTTP apropriado, como `201 Created`.

Em resumo:

```text
Requisição HTTP
  -> filtro de segurança
  -> Controller
  -> validação do DTO
  -> Service (regra de negócio)
  -> Repository / JPA
  -> banco de dados
  -> Mapper / DTO
  -> resposta HTTP
```

## 4. Fluxo funcional dos recursos

### Usuários e autenticação

`POST /api/v1/usuarios` cria uma conta com senha de 6 caracteres. O cadastro público cria usuários com papel `ROLE_CLIENTE`; o papel não é aceito no corpo da requisição para evitar que qualquer pessoa se conceda privilégios administrativos.

`POST /api/v1/auth` valida credenciais e devolve um JWT. Esse token é usado para acessar os demais recursos. Para criar um administrador em ambiente de desenvolvimento, uma conta precisa ser promovida por um procedimento administrativo confiável no banco; não se deve expor essa operação no cadastro público.

### Clientes

Cliente é criado por usuário autenticado e fica associado a esse usuário. O cliente consulta seu próprio cadastro; administrador pode listar todos.

### Bicicletas

Administrador cadastra, lista, localiza pelo código, atualiza e remove bicicletas. O código é único e diferente do ID interno. Status possíveis: `LIVRE`, `OCUPADA` e `EM_MANUTENCAO`.

### Locações

O cliente autenticado solicita uma locação e informa a data prevista de devolução. O sistema escolhe uma bicicleta livre e a reserva. Para iniciar a locação, o cliente efetiva a solicitação com dados do cartão, e a retirada é registrada.

O backend não persiste o número completo do cartão nem o código de segurança; guarda somente o titular, validade e os quatro últimos dígitos. Uma bicicleta não deve ser alugada simultaneamente em duas locações.

### Devoluções

Ao registrar a devolução, o sistema registra a data/hora efetiva, calcula a duração e o valor devido. A tarifa padrão configurada no service é R$ 5,00 por hora e pode ser alterada pela propriedade `bikes.locacao.tarifa-hora`. Sem problema, a bicicleta volta a `LIVRE`; se houver problema, pode ir para `EM_MANUTENCAO`.

### Relatórios

Os endpoints de relatório são administrativos. O JSON permite inspecionar os dados e o endpoint PDF usa JasperReports e o template `.jrxml` em `src/main/resources/reports/`.

## 5. Endpoints principais

Base da API: `http://localhost:8080/api/v1`.

| Recurso | Método e caminho | Acesso |
|---|---|---|
| Criar usuário | `POST /usuarios` | Público |
| Login | `POST /auth` | Público |
| Consultar usuário | `GET /usuarios/{id}` | Admin ou o próprio usuário |
| Listar usuários | `GET /usuarios` | Admin |
| Cadastrar cliente | `POST /clientes` | Autenticado |
| Consultar próprio cliente | `GET /clientes/me` | Cliente ou admin |
| Listar clientes | `GET /clientes` | Admin |
| CRUD bicicletas | `/bicicletas` e `/bicicletas/{codigo}` | Admin |
| Solicitar locação | `POST /locacoes/solicitacoes` | Cliente ou admin |
| Efetivar locação | `POST /locacoes/{id}/efetivar` | Cliente dono |
| Registrar devolução da locação | `POST /locacoes/{id}/devolver` | Cliente dono ou admin |
| Listar próprias locações | `GET /locacoes/me` | Usuário associado a um cliente |
| Registrar devolução detalhada | `POST /devolucoes/locacao/{locacaoId}` | Cliente dono ou admin |
| Listar devoluções | `GET /devolucoes` | Admin |
| Relatório JSON / PDF | `GET /relatorios/locacoes[/pdf]` | Admin |

Os endpoints de efetivação e devolução em `/locacoes` são diferentes do recurso detalhado `/devolucoes`: este último registra a cobrança e os dados da devolução. Apesar de algumas rotas permitirem o papel `ADMIN` na anotação do controller, `efetivar` ainda exige que a conta autenticada esteja associada ao cliente dono da locação. A rota `GET /locacoes/me` também procura um cadastro de cliente associado à conta autenticada.

## 6. Como executar

### Pré-requisitos

- JDK 21.
- MySQL em execução e banco `bikes` criado.
- Node.js e npm para rodar o frontend.

O datasource de desenvolvimento está em `src/main/resources/application.properties`. Ajuste usuário e senha do MySQL para seu ambiente e não publique credenciais reais. Hibernate está configurado com `ddl-auto=update`, que atualiza o esquema com base nas entities; isso é prático para o projeto acadêmico, mas em produção normalmente se usa uma ferramenta de migrations.

### Backend

Na raiz do repositório:

```bash
bash ./mvnw spring-boot:run
```

A API sobe por padrão em `http://localhost:8080`.

### Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. O backend precisa estar rodando para as ações da interface funcionarem.

### Swagger UI

Com o backend no ar, abra `http://localhost:8080/docs-bikes.html`. É possível consultar endpoints e enviar requisições; para operações protegidas, informe o token Bearer.

## 7. Testes

Para executar os testes:

```bash
bash ./mvnw test
```

Os testes de integração usam H2 e cobrem autenticação, autorização, cadastro, regras de locação/devolução e geração do relatório. O H2 evita depender do MySQL para executar os testes. No estado atual, alguns testes antigos de `UsuarioIT` podem falhar porque o script de dados de teste grava `ADMIN`, enquanto o enum de papel do usuário espera `ROLE_ADMIN`.

## 8. Exemplos de chamadas

Criar usuário:

```bash
curl -X POST http://localhost:8080/api/v1/usuarios \
  -H 'Content-Type: application/json' \
  -d '{"username":"cliente@exemplo.com","password":"123456"}'
```

Autenticar e obter o token:

```bash
curl -X POST http://localhost:8080/api/v1/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"cliente@exemplo.com","password":"123456"}'
```

Depois, enviar o token nas chamadas protegidas:

```bash
curl http://localhost:8080/api/v1/clientes/me \
  -H 'Authorization: Bearer <token>'
```


