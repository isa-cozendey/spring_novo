package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Devolucao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {
    Optional<Devolucao> findByLocacaoId(Long locacaoId);
    List<Devolucao> findByLocacaoClienteId(Long clienteId);
}
