package com.renovai.api.repository;

import com.renovai.api.model.Coleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ColetaRepository extends JpaRepository<Coleta, Integer> {
    List<Coleta> findByCooperado_FuncionarioId(Integer cooperadoId);
}
