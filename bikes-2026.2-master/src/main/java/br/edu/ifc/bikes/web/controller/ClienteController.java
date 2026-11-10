package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.ClienteService;
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

@Tag(name = "Clientes", description = "Operações de cadastro e consulta de clientes")
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(summary = "Cadastrar cliente", description = "Cadastra um cliente vinculado ao usuário autenticado",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Cliente cadastrado",
                            content = @Content(schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "409", description = "CPF ou usuário já possui cadastro",
                            content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
                    @ApiResponse(responseCode = "422", description = "Dados de entrada inválidos",
                            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
            })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ClienteResponseDTO> create(
            @Valid @RequestBody ClienteRequestDTO cliente,
            @AuthenticationPrincipal JwtUserDetails usuarioAutenticado) {
        ClienteResponseDTO clienteCriado = clienteService.create(cliente, usuarioAutenticado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteCriado);
    }

    @Operation(summary = "Consultar meus dados", description = "Retorna somente o cadastro do usuário autenticado",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cliente recuperado",
                            content = @Content(schema = @Schema(implementation = ClienteResponseDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
            })
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<ClienteResponseDTO> getMine(
            @AuthenticationPrincipal JwtUserDetails usuarioAutenticado) {
        return ResponseEntity.ok(clienteService.getMine(usuarioAutenticado.getId()));
    }

    @Operation(summary = "Listar clientes", description = "Lista todos os clientes; disponível somente para Admin",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de clientes",
                            content = @Content(array = @ArraySchema(
                                    schema = @Schema(implementation = ClienteResponseDTO.class))))
            })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClienteResponseDTO>> getAll() {
        return ResponseEntity.ok(clienteService.getAll());
    }
}
