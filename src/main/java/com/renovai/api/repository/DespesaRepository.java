package com.renovai.api.repository;
 
import com.renovai.api.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.UUID;
 
@Repository
public interface DespesaRepository extends JpaRepository<Despesa, UUID> {
    List<Despesa> findByCooperativa_CooperativaId(UUID cooperativaId);
    List<Despesa> findByCooperativa_CooperativaIdAndEstaAtivaTrue(UUID cooperativaId);
}