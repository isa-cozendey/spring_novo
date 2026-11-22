package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.LocacaoEfetivacaoDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.dto.LocacaoSolicitacaoDTO;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.LocacaoService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Locações", description = "Solicitação, efetivação e devolução de bicicletas")
@RestController
@RequestMapping("/api/v1/locacoes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LocacaoController {

    private final LocacaoService locacaoService;

    @Operation(summary = "Solicitar aluguel")
    @ApiResponse(responseCode = "201", description = "Solicitação criada",
            content = @Content(schema = @Schema(implementation = LocacaoResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Nenhuma bicicleta disponível",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @PostMapping("/solicitacoes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> solicitar(
            @AuthenticationPrincipal JwtUserDetails usuario,
            @Valid @RequestBody LocacaoSolicitacaoDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locacaoService.solicitar(usuario.getId(), request));
    }

    @Operation(summary = "Efetivar locação com cartão de crédito")
    @PostMapping("/{id}/efetivar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> efetivar(
            @AuthenticationPrincipal JwtUserDetails usuario,
            @PathVariable Long id,
            @Valid @RequestBody LocacaoEfetivacaoDTO request) {
        return ResponseEntity.ok(locacaoService.efetivar(usuario.getId(), id, request));
    }

    @Operation(summary = "Registrar devolução")
    @PostMapping("/{id}/devolver")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> devolver(
            @AuthenticationPrincipal JwtUserDetails usuario,
            @PathVariable Long id) {
        boolean admin = usuario.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(locacaoService.devolver(usuario.getId(), admin, id));
    }

    @Operation(summary = "Consultar minhas locações")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<List<LocacaoResponseDTO>> getMine(
            @AuthenticationPrincipal JwtUserDetails usuario) {
        return ResponseEntity.ok(locacaoService.getMyAll(usuario.getId()));
    }

    @Operation(summary = "Consultar locação por id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> getById(
            @AuthenticationPrincipal JwtUserDetails usuario,
            @PathVariable Long id) {
        boolean admin = usuario.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(locacaoService.getById(usuario.getId(), admin, id));
    }

    @Operation(summary = "Listar todas as locações")
    @ApiResponse(responseCode = "200", description = "Lista de locações",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = LocacaoResponseDTO.class))))
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LocacaoResponseDTO>> getAll() {
        return ResponseEntity.ok(locacaoService.getAll());
    }
}
