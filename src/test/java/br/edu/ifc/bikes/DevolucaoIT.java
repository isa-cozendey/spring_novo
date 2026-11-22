package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.CartaoCreditoRequestDTO;
import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.DevolucaoResponseDTO;
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
class DevolucaoIT {

    private static final String LOCACOES_URL = "/api/v1/locacoes";
    private static final String DEVOLUCOES_URL = "/api/v1/devolucoes";

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
    void devolucaoRegistraTempoValorDataEStatusLivre() {
        LocacaoResponseDTO locacao = efetivarLocacao();

        DevolucaoResponseDTO devolucao = testClient.post()
                .uri(DEVOLUCOES_URL + "/locacao/" + locacao.id())
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DevolucaoRequestDTO(false, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DevolucaoResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(devolucao).isNotNull();
        assertThat(devolucao.locacaoId()).isEqualTo(locacao.id());
        assertThat(devolucao.dataHoraEfetiva()).isNotNull();
        assertThat(devolucao.tempoUtilizacaoMinutos()).isGreaterThanOrEqualTo(1);
        assertThat(devolucao.valorDebitado()).isNotNull();
        assertThat(devolucao.statusBicicleta()).isEqualTo("LIVRE");
        assertThat(devolucao.problemaIdentificado()).isFalse();

        testClient.post()
                .uri(LOCACOES_URL + "/solicitacoes")
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoSolicitacaoDTO(LocalDateTime.now().plusHours(2)))
                .exchange()
                .expectStatus().isCreated();

        testClient.post()
                .uri(DEVOLUCOES_URL + "/locacao/" + locacao.id())
                .header("Authorization", bearerToken("locacao.cliente@test", "ROLE_CLIENTE"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DevolucaoRequestDTO(false, null))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void problemaIdentificadoAlteraBicicletaParaManutencao() {
        LocacaoResponseDTO locacao = efetivarLocacao();

        DevolucaoResponseDTO devolucao = testClient.post()
                .uri(DEVOLUCOES_URL + "/locacao/" + locacao.id())
                .header("Authorization", bearerToken("locacao.admin@test", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new DevolucaoRequestDTO(true, "Freio com defeito"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DevolucaoResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(devolucao.problemaIdentificado()).isTrue();
        assertThat(devolucao.statusBicicleta()).isEqualTo("EM_MANUTENCAO");
        assertThat(devolucao.observacao()).isEqualTo("Freio com defeito");

        DevolucaoResponseDTO atualizada = testClient.patch()
                .uri(DEVOLUCOES_URL + "/" + devolucao.id()
                        + "/bicicleta-status?status=LIVRE")
                .header("Authorization", bearerToken("locacao.admin@test", "ROLE_ADMIN"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(DevolucaoResponseDTO.class)
                .returnResult().getResponseBody();
        assertThat(atualizada.statusBicicleta()).isEqualTo("LIVRE");
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
