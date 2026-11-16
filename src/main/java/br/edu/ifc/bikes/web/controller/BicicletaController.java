package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.service.BicicletaService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bicicletas", description = "Operações administrativas de gestão de bicicletas")
@RestController
@RequestMapping("/api/v1/bicicletas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class BicicletaController {

    private final BicicletaService bicicletaService;

    @Operation(summary = "Cadastrar bicicleta")
    @ApiResponse(responseCode = "201", description = "Bicicleta cadastrada",
            content = @Content(schema = @Schema(implementation = BicicletaResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Código já cadastrado",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @ApiResponse(responseCode = "422", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @PostMapping
    public ResponseEntity<BicicletaResponseDTO> create(@Valid @RequestBody BicicletaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bicicletaService.create(request));
    }

    @Operation(summary = "Listar bicicletas")
    @ApiResponse(responseCode = "200", description = "Lista de bicicletas",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = BicicletaResponseDTO.class))))
    @GetMapping
    public ResponseEntity<List<BicicletaResponseDTO>> getAll() {
        return ResponseEntity.ok(bicicletaService.getAll());
    }

    @Operation(summary = "Consultar bicicleta pelo código")
    @ApiResponse(responseCode = "200", description = "Bicicleta encontrada",
            content = @Content(schema = @Schema(implementation = BicicletaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @GetMapping("/{codigo}")
    public ResponseEntity<BicicletaResponseDTO> getByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(bicicletaService.getByCodigo(codigo));
    }

    @Operation(summary = "Atualizar bicicleta pelo código")
    @ApiResponse(responseCode = "200", description = "Bicicleta atualizada",
            content = @Content(schema = @Schema(implementation = BicicletaResponseDTO.class)))
    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @ApiResponse(responseCode = "409", description = "Código já cadastrado",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @ApiResponse(responseCode = "422", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @PutMapping("/{codigo}")
    public ResponseEntity<BicicletaResponseDTO> update(
            @PathVariable String codigo,
            @Valid @RequestBody BicicletaRequestDTO request) {
        return ResponseEntity.ok(bicicletaService.update(codigo, request));
    }

    @Operation(summary = "Remover bicicleta pelo código")
    @ApiResponse(responseCode = "204", description = "Bicicleta removida")
    @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
            content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> delete(@PathVariable String codigo) {
        bicicletaService.delete(codigo);
        return ResponseEntity.noContent().build();
    }
}
