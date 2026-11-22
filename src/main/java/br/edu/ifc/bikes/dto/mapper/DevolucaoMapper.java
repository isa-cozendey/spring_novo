package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.DevolucaoResponseDTO;
import br.edu.ifc.bikes.entity.Devolucao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DevolucaoMapper {
    @Mapping(source = "locacao.id", target = "locacaoId")
    @Mapping(source = "locacao.bicicleta.codigo", target = "bicicletaCodigo")
    @Mapping(source = "locacao.bicicleta.status", target = "statusBicicleta")
    DevolucaoResponseDTO toResponse(Devolucao devolucao);

    List<DevolucaoResponseDTO> toResponse(List<Devolucao> devolucoes);
}
