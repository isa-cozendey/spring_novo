package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.Locacao;

import java.time.LocalDateTime;

public record LocacaoResponseDTO(
        Long id,
        Long clienteId,
        String bicicletaCodigo,
        Locacao.Status status,
        LocalDateTime dataHoraRetirada,
        LocalDateTime dataHoraPrevistaDevolucao,
        LocalDateTime dataHoraEfetivaDevolucao,
        String cartaoTitular,
        String cartaoFinal,
        String cartaoValidade,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao,
        String criadoPor,
        String modificadoPor
) {}
