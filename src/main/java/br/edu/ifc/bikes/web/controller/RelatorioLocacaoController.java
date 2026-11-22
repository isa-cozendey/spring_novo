package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.RelatorioLocacaoDTO;
import br.edu.ifc.bikes.service.RelatorioLocacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Relatórios", description = "Relatórios administrativos de locações e devoluções")
@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class RelatorioLocacaoController {

    private final RelatorioLocacaoService relatorioLocacaoService;

    @Operation(summary = "Relatório de locações em PDF")
    @GetMapping(value = "/locacoes/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> gerarPdf() {
        byte[] pdf = relatorioLocacaoService.gerarPdfRelatorioLocacoes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"relatorio-locacoes.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Dados do relatório de locações")
    @GetMapping("/locacoes")
    public ResponseEntity<List<RelatorioLocacaoDTO>> listarDados() {
        return ResponseEntity.ok(relatorioLocacaoService.listarDadosRelatorio());
    }
}
