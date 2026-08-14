package com.renovai.api.controller;
 
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Cargo;
import com.renovai.api.repository.CargoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/cargos")
@Tag(name = "Cargos", description = "Tabela de domínio — cargos dos funcionários")
public class CargoController {
 
    private final CargoRepository repository;
 
    public CargoController(CargoRepository repository) {
        this.repository = repository;
    }
 
    public record CargoRequest(@NotBlank @Size(max = 10) String cargo) {}
    public record CargoResponse(UUID cargoId, String cargo) {}
 
    @GetMapping
    @Operation(summary = "Listar todos os cargos")
    public ResponseEntity<List<CargoResponse>> listar() {
        return ResponseEntity.ok(repository.findAll().stream()
                .map(c -> new CargoResponse(c.getCargoId(), c.getCargo())).toList());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar cargo por ID")
    public ResponseEntity<CargoResponse> buscarPorId(@PathVariable UUID id) {
        Cargo c = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", id));
        return ResponseEntity.ok(new CargoResponse(c.getCargoId(), c.getCargo()));
    }
 
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    @Operation(summary = "Criar cargo")
    public ResponseEntity<CargoResponse> criar(@RequestBody @Valid CargoRequest request) {
        Cargo cargo = new Cargo();
        cargo.setCargo(request.cargo());
        Cargo saved = repository.save(cargo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CargoResponse(saved.getCargoId(), saved.getCargo()));
    }
 
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    @Operation(summary = "Atualizar cargo")
    public ResponseEntity<CargoResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid CargoRequest request) {
        Cargo c = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", id));
        c.setCargo(request.cargo());
        return ResponseEntity.ok(new CargoResponse(repository.save(c).getCargoId(), c.getCargo()));
    }
 
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Deletar cargo")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}