package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/clientes/clientes-test-insert.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clientes/clientes-test-delete.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ClienteIT {

    @LocalServerPort
    int port;

    WebTestClient testClient;

    @BeforeEach
    void setUp() {
        testClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void cadastroExigeAutenticacao() {
        testClient.post()
                .uri("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ClienteRequestDTO("Maria da Silva", "12345678901"))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void cadastroVinculaUsuarioAutenticadoERegistraAuditoria() {
        ClienteResponseDTO cliente = criarCliente("maria@gmail.com", "12345678901");

        assertThat(cliente.id()).isNotNull();
        assertThat(cliente.nome()).isEqualTo("Maria da Silva");
        assertThat(cliente.cpf()).isEqualTo("12345678901");
        assertThat(cliente.dataCriacao()).isNotNull();
        assertThat(cliente.criadoPor()).isEqualTo("maria@gmail.com");

        ClienteResponseDTO dadosDoCliente = testClient.get()
                .uri("/api/v1/clientes/me")
                .header("Authorization", bearerToken("maria@gmail.com", "ROLE_CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClienteResponseDTO.class)
                .returnResult().getResponseBody();
        assertThat(dadosDoCliente.id()).isEqualTo(cliente.id());
        assertThat(dadosDoCliente.nome()).isEqualTo(cliente.nome());
        assertThat(dadosDoCliente.cpf()).isEqualTo(cliente.cpf());
    }

    @Test
    void cpfDuplicadoRetornaConflitoMesmoComFormatacaoDiferente() {
        criarCliente("maria@gmail.com", "12345678901");

        testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", bearerToken("joao@gmail.com", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ClienteRequestDTO("João da Silva", "123.456.789-01"))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void clienteConsultaSomenteOsPropriosDadosEAdminListaTodos() {
        criarCliente("maria@gmail.com", "12345678901");

        testClient.get()
                .uri("/api/v1/clientes/me")
                .header("Authorization", bearerToken("joao@gmail.com", "ROLE_CLIENTE"))
                .exchange()
                .expectStatus().isNotFound();

        testClient.get()
                .uri("/api/v1/clientes")
                .header("Authorization", bearerToken("maria@gmail.com", "ROLE_CLIENTE"))
                .exchange()
                .expectStatus().isForbidden();

        testClient.get()
                .uri("/api/v1/clientes")
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ClienteResponseDTO.class)
                .hasSize(1);
    }

    @Test
    void dadosInvalidosRetornamUnprocessableEntity() {
        testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", bearerToken("maria@gmail.com", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ClienteRequestDTO(" ", "123"))
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    private ClienteResponseDTO criarCliente(String username, String cpf) {
        return testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", bearerToken(username, "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ClienteRequestDTO("Maria da Silva", cpf))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ClienteResponseDTO.class)
                .returnResult().getResponseBody();
    }

    private String bearerToken(String username, String role) {
        return JwtUtils.JWT_BEARER + JwtUtils.createToken(username, role).getToken();
    }
}
