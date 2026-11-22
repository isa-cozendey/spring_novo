package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.DevolucaoResponseDTO;
import br.edu.ifc.bikes.dto.mapper.DevolucaoMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Devolucao;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.exception.DevolucaoInvalidException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.DevolucaoRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DevolucaoService {

    private final DevolucaoRepository devolucaoRepository;
    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final BicicletaRepository bicicletaRepository;
    private final DevolucaoMapper devolucaoMapper;

    @Value("${bikes.locacao.tarifa-hora:5.00}")
    private BigDecimal tarifaHora;

    @Transactional
    public DevolucaoResponseDTO registrar(Long usuarioId, boolean admin, Long locacaoId,
                                           DevolucaoRequestDTO request) {
        Locacao locacao = buscarLocacaoComPermissao(usuarioId, admin, locacaoId);
        if (locacao.getStatus() != Locacao.Status.EM_ANDAMENTO) {
            throw new DevolucaoInvalidException(
                    "Somente locações em andamento podem ser devolvidas");
        }
        if (devolucaoRepository.findByLocacaoId(locacaoId).isPresent()) {
            throw new DevolucaoInvalidException("Esta locação já possui devolução registrada");
        }
        if (locacao.getDataHoraRetirada() == null) {
            throw new DevolucaoInvalidException(
                    "A locação não possui data/hora de retirada registrada");
        }

        LocalDateTime efetiva = LocalDateTime.now();
        long minutos = Math.max(1, Duration.between(
                locacao.getDataHoraRetirada(), efetiva).toMinutes());

        BigDecimal valor = tarifaHora
                .multiply(BigDecimal.valueOf(minutos))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new DevolucaoInvalidException("Não foi possível calcular o período de utilização");
        }

        Devolucao devolucao = new Devolucao();
        devolucao.setLocacao(locacao);
        devolucao.setDataHoraEfetiva(efetiva);
        devolucao.setTempoUtilizacaoMinutos(minutos);
        devolucao.setValorDebitado(valor);
        devolucao.setProblemaIdentificado(request.problemaIdentificado());
        devolucao.setObservacao(trim(request.observacao()));

        locacao.setDataHoraEfetivaDevolucao(efetiva);
        locacao.setStatus(Locacao.Status.FINALIZADA);
        locacaoRepository.save(locacao);

        Bicicleta bicicleta = locacao.getBicicleta();
        bicicleta.setStatus(request.problemaIdentificado()
                ? Bicicleta.Status.EM_MANUTENCAO
                : Bicicleta.Status.LIVRE);
        bicicletaRepository.save(bicicleta);

        return devolucaoMapper.toResponse(devolucaoRepository.save(devolucao));
    }

    @Transactional
    public DevolucaoResponseDTO alterarStatusBicicleta(Long devolucaoId, Bicicleta.Status status) {
        if (status == null || status == Bicicleta.Status.OCUPADA) {
            throw new DevolucaoInvalidException(
                    "A bicicleta só pode ser alterada para LIVRE ou EM_MANUTENCAO");
        }
        Devolucao devolucao = devolucaoRepository.findById(devolucaoId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Devolução com id=%d não encontrada", devolucaoId)));
        devolucao.getLocacao().getBicicleta().setStatus(status);
        devolucao.setProblemaIdentificado(status == Bicicleta.Status.EM_MANUTENCAO);
        devolucaoRepository.save(devolucao);
        return devolucaoMapper.toResponse(devolucao);
    }

    @Transactional(readOnly = true)
    public DevolucaoResponseDTO getById(Long usuarioId, boolean admin, Long devolucaoId) {
        Devolucao devolucao = devolucaoRepository.findById(devolucaoId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Devolução com id=%d não encontrada", devolucaoId)));
        if (!admin && !pertenceAoCliente(usuarioId, devolucao)) {
            throw new EntityNotFoundException(
                    String.format("Devolução com id=%d não encontrada", devolucaoId));
        }
        return devolucaoMapper.toResponse(devolucao);
    }

    @Transactional(readOnly = true)
    public List<DevolucaoResponseDTO> getMine(Long usuarioId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado"));
        return devolucaoMapper.toResponse(
                devolucaoRepository.findByLocacaoClienteId(cliente.getId()));
    }

    @Transactional(readOnly = true)
    public List<DevolucaoResponseDTO> getAll() {
        return devolucaoMapper.toResponse(devolucaoRepository.findAll());
    }

    private Locacao buscarLocacaoComPermissao(Long usuarioId, boolean admin, Long locacaoId) {
        Locacao locacao = locacaoRepository.findById(locacaoId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Locação com id=%d não encontrada", locacaoId)));
        if (admin) {
            return locacao;
        }
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado"));
        if (!locacao.getCliente().getId().equals(cliente.getId())) {
            throw new EntityNotFoundException(
                    String.format("Locação com id=%d não encontrada", locacaoId));
        }
        return locacao;
    }

    private boolean pertenceAoCliente(Long usuarioId, Devolucao devolucao) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElse(null);
        return cliente != null
                && devolucao.getLocacao().getCliente().getId().equals(cliente.getId());
    }

    private String trim(String valor) {
        return valor == null ? null : valor.trim();
    }
}
