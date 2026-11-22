package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.CartaoCreditoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoEfetivacaoDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.dto.LocacaoSolicitacaoDTO;
import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/locacoes/locacoes-test-delete.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/locacoes/locacoes-test-insert.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/locacoes/locacoes-test-delete.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RelatorioLocacaoIT {

    private static final String LOCACOES_URL = "/api/v1/locacoes";
    private static final String RELATORIO_URL = "/api/v1/relatorios/locacoes";

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
    void administradorGeraRelatorioComLocacaoFinalizada() {
        LocacaoResponseDTO locacao = efetivarLocacao();

        testClient.post()
                .uri("/api/v1/devolucoes/locacao/" + locacao.id())
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DevolucaoRequestDTO(false, null))
                .exchange()
                .expectStatus().isCreated();

        testClient.get()
                .uri(RELATORIO_URL)
                .header("Authorization", bearerToken("locacao.admin@test", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].nomeCliente").isEqualTo("Cliente Locação")
                .jsonPath("$[0].codigoBicicleta").isEqualTo("LOC-BIKE-001")
                .jsonPath("$[0].dataRetirada").isNotEmpty()
                .jsonPath("$[0].dataDevolucao").isNotEmpty()
                .jsonPath("$[0].duracao").isNotEmpty()
                .jsonPath("$[0].status").isEqualTo("FINALIZADA");

        testClient.get()
                .uri(RELATORIO_URL + "/pdf")
                .header("Authorization", bearerToken("locacao.admin@test", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectHeader().valueMatches("Content-Disposition",
                        ".*relatorio-locacoes\\.pdf.*")
                .expectBody()
                .consumeWith(response -> {
                    byte[] bytes = response.getResponseBody();
                    org.assertj.core.api.Assertions.assertThat(bytes).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(bytes)
                            .startsWith(new byte[]{0x25, 0x50, 0x44, 0x46});
                });
    }

    @Test
    void relatorioExigePapelAdministrador() {
        testClient.get()
                .uri(RELATORIO_URL)
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .exchange()
                .expectStatus().isForbidden();
    }

    private LocacaoResponseDTO efetivarLocacao() {
        LocacaoResponseDTO solicitada = testClient.post()
                .uri(LOCACOES_URL + "/solicitacoes")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoSolicitacaoDTO(LocalDateTime.now().plusHours(2)))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(LocacaoResponseDTO.class)
                .returnResult().getResponseBody();

        return testClient.post()
                .uri(LOCACOES_URL + "/" + solicitada.id() + "/efetivar")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoEfetivacaoDTO(
                        new CartaoCreditoRequestDTO(
                                "Cliente Locação", "4111111111111111", "12/30", "123")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LocacaoResponseDTO.class)
                .returnResult().getResponseBody();
    }

    private String bearerToken(String username, String role) {
        return JwtUtils.JWT_BEARER + JwtUtils.createToken(username, role).getToken();
    }
}
