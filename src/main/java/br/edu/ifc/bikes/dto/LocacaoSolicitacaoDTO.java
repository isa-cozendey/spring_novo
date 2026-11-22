package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record LocacaoSolicitacaoDTO(
        @NotNull(message = "A data/hora prevista para devolução é obrigatória")
        @Future(message = "A devolução prevista deve ser posterior à data/hora atual")
        LocalDateTime dataHoraPrevistaDevolucao
) {}
