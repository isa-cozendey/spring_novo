package br.edu.ifc.bikes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString(exclude = {"cliente", "bicicleta"})
@Entity
@Table(name = "locacoes")
@EntityListeners(AuditingEntityListener.class)
public class Locacao implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bicicleta_id", nullable = false)
    private Bicicleta bicicleta;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.SOLICITADA;

    @Column(name = "data_hora_retirada")
    private LocalDateTime dataHoraRetirada;

    @Column(name = "data_hora_prevista_devolucao", nullable = false)
    private LocalDateTime dataHoraPrevistaDevolucao;

    @Column(name = "data_hora_efetiva_devolucao")
    private LocalDateTime dataHoraEfetivaDevolucao;

    @Column(name = "cartao_titular", length = 150)
    private String cartaoTitular;

    @Column(name = "cartao_final", length = 4)
    private String cartaoFinal;

    @Column(name = "cartao_validade", length = 7)
    private String cartaoValidade;

    @CreatedDate
    private LocalDateTime dataCriacao;
    @LastModifiedDate
    private LocalDateTime dataModificacao;
    @CreatedBy
    private String criadoPor;
    @LastModifiedBy
    private String modificadoPor;

    public enum Status {
        SOLICITADA, EM_ANDAMENTO, FINALIZADA, CANCELADA
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Locacao locacao)) return false;
        return id != null && id.equals(locacao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
