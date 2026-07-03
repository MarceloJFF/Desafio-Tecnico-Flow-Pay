CREATE TABLE atendentes (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    time VARCHAR(20) NOT NULL,
    atendimentos_ativos INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_atendentes_time CHECK (time IN ('CARTOES', 'EMPRESTIMOS', 'OUTROS')),
    CONSTRAINT chk_limite_atendimentos CHECK (atendimentos_ativos BETWEEN 0 AND 3)
);

CREATE TABLE atendimentos (
    id UUID PRIMARY KEY,
    assunto VARCHAR(255) NOT NULL,
    time VARCHAR(20) NOT NULL,
    atendente_id UUID REFERENCES atendentes(id),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atribuido_em TIMESTAMP,
    finalizado_em TIMESTAMP,
    CONSTRAINT chk_atendimentos_time CHECK (time IN ('CARTOES', 'EMPRESTIMOS', 'OUTROS')),
    CONSTRAINT chk_atendimentos_status CHECK (status IN ('AGUARDANDO', 'EM_ATENDIMENTO', 'FINALIZADO')),
    CONSTRAINT chk_atendimento_aguardando_sem_atendente CHECK (status <> 'AGUARDANDO' OR atendente_id IS NULL)
);

CREATE INDEX idx_atendimentos_status_time ON atendimentos (status, time, criado_em);
CREATE INDEX idx_atendentes_time_carga ON atendentes (time, atendimentos_ativos);

INSERT INTO atendentes (id, nome, time, atendimentos_ativos, version) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Ana Cartoes', 'CARTOES', 0, 0),
    ('11111111-1111-1111-1111-111111111112', 'Bruno Cartoes', 'CARTOES', 0, 0),
    ('11111111-1111-1111-1111-111111111113', 'Carla Cartoes', 'CARTOES', 0, 0),
    ('22222222-2222-2222-2222-222222222221', 'Diego Emprestimos', 'EMPRESTIMOS', 0, 0),
    ('22222222-2222-2222-2222-222222222222', 'Elisa Emprestimos', 'EMPRESTIMOS', 0, 0),
    ('22222222-2222-2222-2222-222222222223', 'Felipe Emprestimos', 'EMPRESTIMOS', 0, 0),
    ('33333333-3333-3333-3333-333333333331', 'Gabriela Outros', 'OUTROS', 0, 0),
    ('33333333-3333-3333-3333-333333333332', 'Henrique Outros', 'OUTROS', 0, 0),
    ('33333333-3333-3333-3333-333333333333', 'Isabela Outros', 'OUTROS', 0, 0);
