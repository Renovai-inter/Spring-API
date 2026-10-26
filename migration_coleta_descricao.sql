-- Descrição e data prevista da triagem no cadastro da coleta (app: tela Registrar Coleta).
-- Rodar no banco ANTES de publicar a versão da API que traz estes campos.
ALTER TABLE coletas
    ADD COLUMN IF NOT EXISTS descricao TEXT,
    ADD COLUMN IF NOT EXISTS data_prevista_triagem DATE;
