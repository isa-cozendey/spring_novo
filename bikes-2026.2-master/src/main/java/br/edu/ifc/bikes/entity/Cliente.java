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
@ToString
@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clientes_cpf", columnNames = "cpf"),
        @UniqueConstraint(name = "uk_clientes_usuario_id", columnNames = "usuario_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Cliente implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

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
        if (!(obj instanceof Cliente cliente)) return false;
        return id != null && id.equals(cliente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
