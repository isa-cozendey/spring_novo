package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.DevolucaoResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.DevolucaoService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Devoluções", description = "Registro, cobrança e estado de devoluções")
@RestController
@RequestMapping("/api/v1/devolucoes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DevolucaoController {

    private final DevolucaoService devolucaoService;

    @Operation(summary = "Registrar devolução de uma locação")
    @PostMapping("/locacao/{locacaoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<DevolucaoResponseDTO> registrar(
            @AuthenticationPrincipal JwtUserDetails usuario,
            @PathVariable Long locacaoId,
            @RequestBody(required = false) @Valid DevolucaoRequestDTO request) {
        boolean admin = usuario.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        DevolucaoRequestDTO effectiveRequest = request == null
                ? new DevolucaoRequestDTO(false, null)
                : request;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(devolucaoService.registrar(
                        usuario.getId(), admin, locacaoId, effectiveRequest));
    }

    @Operation(summary = "Alterar o estado da bicicleta após devolução")
    @PatchMapping("/{devolucaoId}/bicicleta-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucaoResponseDTO> alterarStatusBicicleta(
            @PathVariable Long devolucaoId,
            @RequestParam Bicicleta.Status status) {
        return ResponseEntity.ok(
                devolucaoService.alterarStatusBicicleta(devolucaoId, status));
    }

    @Operation(summary = "Consultar devolução por id")
    @GetMapping("/{devolucaoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<DevolucaoResponseDTO> getById(
            @AuthenticationPrincipal JwtUserDetails usuario,
            @PathVariable Long devolucaoId) {
        boolean admin = usuario.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(
                devolucaoService.getById(usuario.getId(), admin, devolucaoId));
    }

    @Operation(summary = "Consultar minhas devoluções")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<List<DevolucaoResponseDTO>> getMine(
            @AuthenticationPrincipal JwtUserDetails usuario) {
        return ResponseEntity.ok(devolucaoService.getMine(usuario.getId()));
    }

    @Operation(summary = "Listar todas as devoluções")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DevolucaoResponseDTO>> getAll() {
        return ResponseEntity.ok(devolucaoService.getAll());
    }
}
