package br.edu.ifc.bikes.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DevolucaoResponseDTO(
        Long id,
        Long locacaoId,
        String bicicletaCodigo,
        LocalDateTime dataHoraEfetiva,
        Long tempoUtilizacaoMinutos,
        BigDecimal valorDebitado,
        boolean problemaIdentificado,
        String observacao,
        String statusBicicleta,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao,
        String criadoPor,
        String modificadoPor
) {}
