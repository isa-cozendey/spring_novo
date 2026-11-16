package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
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
@Sql(scripts = {
        "/sql/bicicletas/bicicletas-users-insert.sql",
        "/sql/bicicletas/bicicletas-test-delete.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = {
        "/sql/bicicletas/bicicletas-test-delete.sql",
        "/sql/bicicletas/bicicletas-users-delete.sql"
}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class BicicletaIT {

    private static final String BASE_URL = "/api/v1/bicicletas";
    private static final String CODIGO = "BIKE-001";

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
    void todasAsOperacoesExigemPapelAdmin() {
        criarBicicleta(CODIGO);

        testClient.get().uri(BASE_URL)
                .header("Authorization", bearerToken("bici.cliente@test", "ROLE_CLIENTE"))
                .exchange().expectStatus().isForbidden();
        testClient.get().uri(BASE_URL + "/" + CODIGO)
                .header("Authorization", bearerToken("bici.cliente@test", "ROLE_CLIENTE"))
                .exchange().expectStatus().isForbidden();
        testClient.post().uri(BASE_URL)
                .header("Authorization", bearerToken("bici.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("BIKE-002", Bicicleta.Status.LIVRE))
                .exchange().expectStatus().isForbidden();
        testClient.put().uri(BASE_URL + "/" + CODIGO)
                .header("Authorization", bearerToken("bici.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(CODIGO, Bicicleta.Status.OCUPADA))
                .exchange().expectStatus().isForbidden();
        testClient.delete().uri(BASE_URL + "/" + CODIGO)
                .header("Authorization", bearerToken("bici.cliente@test", "ROLE_CLIENTE"))
                .exchange().expectStatus().isForbidden();

        testClient.get().uri(BASE_URL)
                .exchange().expectStatus().is4xxClientError();
        testClient.get().uri(BASE_URL + "/" + CODIGO)
                .exchange().expectStatus().is4xxClientError();
        testClient.post().uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request("BIKE-003", Bicicleta.Status.LIVRE))
                .exchange().expectStatus().is4xxClientError();
        testClient.put().uri(BASE_URL + "/" + CODIGO)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(CODIGO, Bicicleta.Status.OCUPADA))
                .exchange().expectStatus().is4xxClientError();
        testClient.delete().uri(BASE_URL + "/" + CODIGO)
                .exchange().expectStatus().is4xxClientError();
    }

    @Test
    void administradorCadastraLocalizaAtualizaListaERemovePeloCodigo() {
        BicicletaResponseDTO criada = criarBicicleta(CODIGO);

        assertThat(criada.id()).isNotNull();
        assertThat(criada.codigo()).isEqualTo(CODIGO);
        assertThat(criada.codigo()).isNotEqualTo(criada.id().toString());
        assertThat(criada.status()).isEqualTo(Bicicleta.Status.LIVRE);

        BicicletaResponseDTO localizada = testClient.get()
                .uri(BASE_URL + "/" + CODIGO)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(BicicletaResponseDTO.class)
                .returnResult().getResponseBody();
        assertThat(localizada.codigo()).isEqualTo(CODIGO);

        testClient.get()
                .uri(BASE_URL)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BicicletaResponseDTO.class)
                .hasSize(1);

        BicicletaResponseDTO atualizada = testClient.put()
                .uri(BASE_URL + "/" + CODIGO)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(CODIGO, Bicicleta.Status.EM_MANUTENCAO))
                .exchange()
                .expectStatus().isOk()
                .expectBody(BicicletaResponseDTO.class)
                .returnResult().getResponseBody();
        assertThat(atualizada.status()).isEqualTo(Bicicleta.Status.EM_MANUTENCAO);

        testClient.delete()
                .uri(BASE_URL + "/" + CODIGO)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isNoContent();

        testClient.get()
                .uri(BASE_URL + "/" + CODIGO)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void codigoDuplicadoRetornaConflitoEStatusInvalidoRetorna422() {
        criarBicicleta(CODIGO);

        testClient.post()
                .uri(BASE_URL)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(CODIGO, Bicicleta.Status.LIVRE))
                .exchange()
                .expectStatus().isEqualTo(409);

        testClient.post()
                .uri(BASE_URL)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"codigo\":\" \",\"status\":null}")
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    private BicicletaResponseDTO criarBicicleta(String codigo) {
        return testClient.post()
                .uri(BASE_URL)
                .header("Authorization", bearerToken("jose@gmail.com", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request(codigo, Bicicleta.Status.LIVRE))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BicicletaResponseDTO.class)
                .returnResult().getResponseBody();
    }

    private BicicletaRequestDTO request(String codigo, Bicicleta.Status status) {
        return new BicicletaRequestDTO(codigo, status);
    }

    private String bearerToken(String username, String role) {
        return JwtUtils.JWT_BEARER + JwtUtils.createToken(username, role).getToken();
    }
}
