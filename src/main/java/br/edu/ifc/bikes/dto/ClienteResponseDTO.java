package br.edu.ifc.bikes.dto;

import java.time.LocalDateTime;

public record ClienteResponseDTO(
        Long id,
        String nome,
        String cpf,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao,
        String criadoPor,
        String modificadoPor
) {}
