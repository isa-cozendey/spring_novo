package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.Size;

public record DevolucaoRequestDTO(
        boolean problemaIdentificado,
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {}
