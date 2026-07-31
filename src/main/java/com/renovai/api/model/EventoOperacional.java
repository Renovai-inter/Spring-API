package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_operacionais")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoOperacional {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventoId;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "status_id", foreignKey = @ForeignKey(name = "fk_eventos_status"))
    private Status status;
}