package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.Bicicleta;

import java.time.LocalDateTime;

public record BicicletaResponseDTO(
        Long id,
        String codigo,
        Bicicleta.Status status,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao,
        String criadoPor,
        String modificadoPor
) {}
