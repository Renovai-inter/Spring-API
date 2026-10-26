package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.renovai.api.dto.request.Requests.MaterialRequest;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.CategoriaMaterial;
import com.renovai.api.model.Material;
import com.renovai.api.repository.CategoriaMaterialRepository;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.MaterialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {
    private static final String FOTO = "https://res.cloudinary.com/ki7bmzxt/image/upload/v1/Renovai/materiais/pet.jpg";

    @Mock private MaterialRepository repository;
    @Mock private CategoriaMaterialRepository categoriaRepository;
    @Mock private CooperativaRepository cooperativaRepository;

    private MaterialService service;
    private final UUID categoriaId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new MaterialService(repository, categoriaRepository, cooperativaRepository);
        CategoriaMaterial categoria = new CategoriaMaterial();
        categoria.setCategoriaId(categoriaId);
        categoria.setNomeCategoria("PET");
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
    }

    @Test
    void cadastroGuardaAFotoDoMaterial() {
        when(repository.save(any(Material.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.criar(new MaterialRequest(categoriaId, null, new BigDecimal("2.50"), true, FOTO));

        assertThat(resposta.imagemUrl()).isEqualTo(FOTO);
    }

    @Test
    void atualizarSemFotoMantemAFotoAtual() {
        UUID id = UUID.randomUUID();
        Material material = Material.builder().materialId(id).imagemUrl(FOTO).estaDisponivel(true).build();
        when(repository.findById(id)).thenReturn(Optional.of(material));
        when(repository.save(any(Material.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = service.atualizar(id, new MaterialRequest(categoriaId, null, BigDecimal.ONE, true, null));

        assertThat(resposta.imagemUrl()).isEqualTo(FOTO);
    }

    @Test
    void recusaFotoQueNaoEhHttps() {
        assertThatThrownBy(() -> service.criar(
                        new MaterialRequest(categoriaId, null, BigDecimal.ONE, true, "http://exemplo.com/a.jpg")))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
