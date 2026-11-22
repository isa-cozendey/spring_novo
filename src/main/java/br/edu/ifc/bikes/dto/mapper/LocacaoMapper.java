package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Locacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LocacaoMapper {
    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "bicicleta.codigo", target = "bicicletaCodigo")
    LocacaoResponseDTO toResponse(Locacao locacao);

    List<LocacaoResponseDTO> toResponse(List<Locacao> locacoes);
}
