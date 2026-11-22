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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@ToString(exclude = "locacao")
@Entity
@Table(name = "devolucoes")
@EntityListeners(AuditingEntityListener.class)
public class Devolucao implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "locacao_id", nullable = false, unique = true)
    private Locacao locacao;

    @Column(name = "data_hora_efetiva", nullable = false)
    private LocalDateTime dataHoraEfetiva;

    @Column(name = "tempo_utilizacao_minutos", nullable = false)
    private Long tempoUtilizacaoMinutos;

    @Column(name = "valor_debitado", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorDebitado;

    @Column(name = "problema_identificado", nullable = false)
    private boolean problemaIdentificado;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @CreatedDate
    private LocalDateTime dataCriacao;
    @LastModifiedDate
    private LocalDateTime dataModificacao;
    @CreatedBy
    private String criadoPor;
    @LastModifiedBy
    private String modificadoPor;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Devolucao devolucao)) return false;
        return id != null && id.equals(devolucao.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
