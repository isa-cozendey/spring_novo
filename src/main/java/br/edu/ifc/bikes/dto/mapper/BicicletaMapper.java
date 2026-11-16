package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BicicletaMapper {
    BicicletaResponseDTO toResponse(Bicicleta bicicleta);
    List<BicicletaResponseDTO> toResponse(List<Bicicleta> bicicletas);
}
