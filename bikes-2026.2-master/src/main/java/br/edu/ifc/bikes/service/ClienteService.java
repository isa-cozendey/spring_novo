package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.dto.mapper.ClienteMapper;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.exception.ClienteUniqueViolationException;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;

    @Transactional
    public ClienteResponseDTO create(ClienteRequestDTO request, Long usuarioId) {
        String cpf = request.cpf().replaceAll("\\D", "");
        if (clienteRepository.existsByCpf(cpf)) {
            throw new ClienteUniqueViolationException("Já existe um cliente cadastrado com este CPF");
        }
        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            throw new ClienteUniqueViolationException("O usuário autenticado já possui um cadastro de cliente");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(
                () -> new EntityNotFoundException(String.format("Usuário com id=%d não encontrado", usuarioId))
        );
        Cliente cliente = new Cliente();
        cliente.setNome(request.nome().trim());
        cliente.setCpf(cpf);
        cliente.setUsuario(usuario);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO getMine(Long usuarioId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("Cliente do usuário autenticado não encontrado")
        );
        return clienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> getAll() {
        return clienteMapper.toResponse(clienteRepository.findAll());
    }
}
