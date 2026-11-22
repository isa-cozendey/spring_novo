package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.LocacaoEfetivacaoDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.dto.LocacaoSolicitacaoDTO;
import br.edu.ifc.bikes.dto.mapper.LocacaoMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.LocacaoInvalidException;
import br.edu.ifc.bikes.exception.LocacaoUnavailableException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class LocacaoService {

    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final BicicletaRepository bicicletaRepository;
    private final LocacaoMapper locacaoMapper;

    @Transactional
    public LocacaoResponseDTO solicitar(Long usuarioId, LocacaoSolicitacaoDTO request) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado"));

        Bicicleta bicicleta = bicicletaRepository
                .findFirstByStatus(Bicicleta.Status.LIVRE)
                .orElseThrow(() -> new LocacaoUnavailableException(
                        "Não existem bicicletas disponíveis no momento"));

        boolean bicicletaEmLocacao = locacaoRepository.existsByBicicletaIdAndStatusIn(
                bicicleta.getId(),
                List.of(Locacao.Status.SOLICITADA, Locacao.Status.EM_ANDAMENTO));
        if (bicicletaEmLocacao) {
            throw new LocacaoUnavailableException(
                    "A bicicleta selecionada já possui uma locação em andamento");
        }

        bicicleta.setStatus(Bicicleta.Status.OCUPADA);

        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setBicicleta(bicicleta);
        locacao.setStatus(Locacao.Status.SOLICITADA);
        locacao.setDataHoraPrevistaDevolucao(request.dataHoraPrevistaDevolucao());
        return locacaoMapper.toResponse(locacaoRepository.save(locacao));
    }

    @Transactional
    public LocacaoResponseDTO efetivar(Long usuarioId, Long locacaoId, LocacaoEfetivacaoDTO request) {
        Locacao locacao = buscarLocacaoDoUsuarioParaAtualizacao(usuarioId, locacaoId);
        if (locacao.getStatus() != Locacao.Status.SOLICITADA) {
            throw new LocacaoInvalidException("Somente solicitações pendentes podem ser efetivadas");
        }

        Bicicleta bicicleta = bicicletaRepository.findById(locacao.getBicicleta().getId()).orElseThrow(
                () -> new EntityNotFoundException("Bicicleta da locação não encontrada"));
        if (bicicleta.getStatus() != Bicicleta.Status.OCUPADA) {
            throw new LocacaoUnavailableException("A bicicleta não está reservada para esta locação");
        }

        locacao.setStatus(Locacao.Status.EM_ANDAMENTO);
        locacao.setDataHoraRetirada(LocalDateTime.now());
        locacao.setCartaoTitular(request.cartao().titular().trim());
        locacao.setCartaoFinal(request.cartao().numero()
                .substring(request.cartao().numero().length() - 4));
        locacao.setCartaoValidade(request.cartao().validade());
        return locacaoMapper.toResponse(locacaoRepository.save(locacao));
    }

    @Transactional
    public LocacaoResponseDTO devolver(Long usuarioId, boolean admin, Long locacaoId) {
        Locacao locacao = buscarLocacaoComPermissao(usuarioId, admin, locacaoId);
        if (locacao.getStatus() != Locacao.Status.EM_ANDAMENTO) {
            throw new LocacaoInvalidException("Somente locações em andamento podem ser devolvidas");
        }

        locacao.setDataHoraEfetivaDevolucao(LocalDateTime.now());
        locacao.setStatus(Locacao.Status.FINALIZADA);
        locacao.getBicicleta().setStatus(Bicicleta.Status.LIVRE);
        bicicletaRepository.save(locacao.getBicicleta());
        return locacaoMapper.toResponse(locacaoRepository.save(locacao));
    }

    @Transactional(readOnly = true)
    public LocacaoResponseDTO getById(Long usuarioId, boolean admin, Long locacaoId) {
        Locacao locacao;
        if (admin) {
            locacao = locacaoRepository.findById(locacaoId).orElseThrow(
                    () -> new EntityNotFoundException(
                            String.format("Locação com id=%d não encontrada", locacaoId)));
        } else {
            locacao = buscarLocacaoDoUsuario(usuarioId, locacaoId);
        }
        return locacaoMapper.toResponse(locacao);
    }

    @Transactional(readOnly = true)
    public LocacaoResponseDTO getMine(Long usuarioId, Long locacaoId) {
        return locacaoMapper.toResponse(buscarLocacaoDoUsuario(usuarioId, locacaoId));
    }

    @Transactional(readOnly = true)
    public List<LocacaoResponseDTO> getMyAll(Long usuarioId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado"));
        return locacaoMapper.toResponse(locacaoRepository.findByClienteId(cliente.getId()));
    }

    @Transactional(readOnly = true)
    public List<LocacaoResponseDTO> getAll() {
        return locacaoMapper.toResponse(locacaoRepository.findAll());
    }

    private Locacao buscarLocacaoDoUsuario(Long usuarioId, Long locacaoId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado"));
        Locacao locacao = locacaoRepository.findById(locacaoId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Locação com id=%d não encontrada", locacaoId)));
        if (!locacao.getCliente().getId().equals(cliente.getId())) {
            throw new EntityNotFoundException(
                    String.format("Locação com id=%d não encontrada", locacaoId));
        }
        return locacao;
    }

    private Locacao buscarLocacaoDoUsuarioParaAtualizacao(Long usuarioId, Long locacaoId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado"));
        Locacao locacao = locacaoRepository.findLockedById(locacaoId).orElseThrow(
                () -> new EntityNotFoundException(
                        String.format("Locação com id=%d não encontrada", locacaoId)));
        if (!locacao.getCliente().getId().equals(cliente.getId())) {
            throw new EntityNotFoundException(
                    String.format("Locação com id=%d não encontrada", locacaoId));
        }
        return locacao;
    }

    private Locacao buscarLocacaoComPermissao(Long usuarioId, boolean admin, Long locacaoId) {
        if (admin) {
            return locacaoRepository.findLockedById(locacaoId).orElseThrow(
                    () -> new EntityNotFoundException(
                            String.format("Locação com id=%d não encontrada", locacaoId)));
        }
        return buscarLocacaoDoUsuarioParaAtualizacao(usuarioId, locacaoId);
    }
}
