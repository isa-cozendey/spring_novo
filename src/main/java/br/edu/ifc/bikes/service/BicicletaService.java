package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.mapper.BicicletaMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.exception.BicicletaUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BicicletaService {

    private final BicicletaRepository bicicletaRepository;
    private final BicicletaMapper bicicletaMapper;

    @Transactional
    public BicicletaResponseDTO create(BicicletaRequestDTO request) {
        String codigo = request.codigo().trim();
        if (bicicletaRepository.existsByCodigo(codigo)) {
            throw new BicicletaUniqueViolationException(
                    String.format("Já existe uma bicicleta cadastrada com o código '%s'", codigo));
        }

        Bicicleta bicicleta = new Bicicleta();
        bicicleta.setCodigo(codigo);
        bicicleta.setStatus(request.status());
        return bicicletaMapper.toResponse(bicicletaRepository.save(bicicleta));
    }

    @Transactional(readOnly = true)
    public List<BicicletaResponseDTO> getAll() {
        return bicicletaMapper.toResponse(bicicletaRepository.findAll());
    }

    @Transactional(readOnly = true)
    public BicicletaResponseDTO getByCodigo(String codigo) {
        return bicicletaMapper.toResponse(buscarPorCodigo(codigo));
    }

    @Transactional
    public BicicletaResponseDTO update(String codigoAtual, BicicletaRequestDTO request) {
        Bicicleta bicicleta = buscarPorCodigo(codigoAtual);
        String novoCodigo = request.codigo().trim();
        if (!bicicleta.getCodigo().equals(novoCodigo)
                && bicicletaRepository.existsByCodigo(novoCodigo)) {
            throw new BicicletaUniqueViolationException(
                    String.format("Já existe uma bicicleta cadastrada com o código '%s'", novoCodigo));
        }

        bicicleta.setCodigo(novoCodigo);
        bicicleta.setStatus(request.status());
        return bicicletaMapper.toResponse(bicicletaRepository.save(bicicleta));
    }

    @Transactional
    public void delete(String codigo) {
        bicicletaRepository.delete(buscarPorCodigo(codigo));
    }

    private Bicicleta buscarPorCodigo(String codigo) {
        return bicicletaRepository.findByCodigo(codigo).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Bicicleta com código '%s' não encontrada", codigo))
        );
    }
}
