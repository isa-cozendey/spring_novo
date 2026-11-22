package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Locacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocacaoRepository extends JpaRepository<Locacao, Long> {
    List<Locacao> findByClienteId(Long clienteId);

    boolean existsByBicicletaIdAndStatusIn(
            Long bicicletaId,
            Collection<Locacao.Status> status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Locacao l where l.id = :id")
    Optional<Locacao> findLockedById(@Param("id") Long id);
}
