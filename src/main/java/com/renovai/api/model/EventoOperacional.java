package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoOperacional {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "evento_id")
    private UUID eventoId;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "status_id", foreignKey = @ForeignKey(name = "fk_eventos_status"))
    private Status status;
}
