package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.CartaoCreditoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoEfetivacaoDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.dto.LocacaoSolicitacaoDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/locacoes/locacoes-test-delete.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/locacoes/locacoes-test-insert.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/locacoes/locacoes-test-delete.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class LocacaoIT {

    private static final String BASE_URL = "/api/v1/locacoes";

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
    void clienteSolicitaEfetivaEDevolveBicicleta() {
        LocacaoResponseDTO solicitada = testClient.post()
                .uri(BASE_URL + "/solicitacoes")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoSolicitacaoDTO(LocalDateTime.now().plusHours(3)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LocacaoResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(solicitada).isNotNull();
        assertThat(solicitada.status()).isEqualTo(Locacao.Status.SOLICITADA);
        assertThat(solicitada.bicicletaCodigo()).isEqualTo("LOC-BIKE-001");
        assertThat(solicitada.dataHoraRetirada()).isNull();

        LocacaoResponseDTO efetivada = testClient.post()
                .uri(BASE_URL + "/" + solicitada.id() + "/efetivar")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoEfetivacaoDTO(
                        new CartaoCreditoRequestDTO(
                                "Cliente Locação", "4111111111111111", "12/30", "123")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LocacaoResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(efetivada.status()).isEqualTo(Locacao.Status.EM_ANDAMENTO);
        assertThat(efetivada.dataHoraRetirada()).isNotNull();
        assertThat(efetivada.cartaoFinal()).isEqualTo("1111");

        LocacaoResponseDTO devolvida = testClient.post()
                .uri(BASE_URL + "/" + solicitada.id() + "/devolver")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LocacaoResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(devolvida.status()).isEqualTo(Locacao.Status.FINALIZADA);
        assertThat(devolvida.dataHoraEfetivaDevolucao()).isNotNull();

        LocacaoResponseDTO novaSolicitacao = testClient.post()
                .uri(BASE_URL + "/solicitacoes")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoSolicitacaoDTO(LocalDateTime.now().plusHours(2)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LocacaoResponseDTO.class)
                .returnResult().getResponseBody();
        assertThat(novaSolicitacao).isNotNull();
    }

    @Test
    void locacaoSemBicicletaLivreEReservaUnicaRetornamConflito() {
        LocacaoResponseDTO solicitada = solicitar();

        testClient.post()
                .uri(BASE_URL + "/solicitacoes")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoSolicitacaoDTO(LocalDateTime.now().plusHours(2)))
                .exchange()
                .expectStatus().isEqualTo(409);

        testClient.post()
                .uri(BASE_URL + "/" + solicitada.id() + "/efetivar")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoEfetivacaoDTO(
                        new CartaoCreditoRequestDTO(
                                "Cliente Locação", "4111111111111111", "12/30", "123")))
                .exchange()
                .expectStatus().isOk();

        testClient.post()
                .uri(BASE_URL + "/" + solicitada.id() + "/efetivar")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoEfetivacaoDTO(
                        new CartaoCreditoRequestDTO(
                                "Cliente Locação", "4111111111111111", "12/30", "123")))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void clienteNaoPodeConsultarLocacaoDeOutroCliente() {
        LocacaoResponseDTO solicitada = solicitar();

        testClient.get()
                .uri(BASE_URL + "/" + solicitada.id())
                .header("Authorization", bearerToken("outro.cliente@test", "ROLE_CLIENTE"))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    private LocacaoResponseDTO solicitar() {
        return testClient.post()
                .uri(BASE_URL + "/solicitacoes")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoSolicitacaoDTO(LocalDateTime.now().plusHours(2)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LocacaoResponseDTO.class)
                .returnResult().getResponseBody();
    }

    private String bearerToken(String username, String role) {
        return JwtUtils.JWT_BEARER + JwtUtils.createToken(username, role).getToken();
    }
}
