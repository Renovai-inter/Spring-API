ALTER TABLE coletas
    ADD COLUMN imagem_url TEXT,
    ADD COLUMN rota_id UUID;

ALTER TABLE coletas
    ADD CONSTRAINT fk_coletas_rotas FOREIGN KEY (rota_id) REFERENCES rotas(rota_id);
