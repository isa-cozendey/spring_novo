package br.edu.ifc.bikes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RelatorioLocacaoDTO {
    private final String nomeCliente;
    private final String codigoBicicleta;
    private final String dataRetirada;
    private final String dataDevolucao;
    private final String duracao;
    private final String status;
}
