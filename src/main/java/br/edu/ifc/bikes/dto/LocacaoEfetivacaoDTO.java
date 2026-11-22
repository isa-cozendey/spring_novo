package br.edu.ifc.bikes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record LocacaoEfetivacaoDTO(
        @NotNull(message = "Os dados do cartão são obrigatórios")
        @Valid
        CartaoCreditoRequestDTO cartao
) {}
