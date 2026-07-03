CREATE TABLE assuntos (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    time VARCHAR(20) NOT NULL,
    CONSTRAINT chk_assuntos_time CHECK (time IN ('CARTOES', 'EMPRESTIMOS', 'OUTROS'))
);

INSERT INTO assuntos (id, nome, time) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Problemas com cartão', 'CARTOES'),
    ('a0000000-0000-0000-0000-000000000002', 'Contratação de empréstimo', 'EMPRESTIMOS'),
    ('a0000000-0000-0000-0000-000000000003', 'Outros', 'OUTROS');

ALTER TABLE atendimentos ADD COLUMN assunto_id UUID;
ALTER TABLE atendimentos ADD COLUMN observacao VARCHAR(500);

UPDATE atendimentos SET assunto_id = 'a0000000-0000-0000-0000-000000000003' WHERE assunto_id IS NULL;

ALTER TABLE atendimentos ALTER COLUMN assunto_id SET NOT NULL;
ALTER TABLE atendimentos ADD CONSTRAINT fk_atendimentos_assunto FOREIGN KEY (assunto_id) REFERENCES assuntos(id);

ALTER TABLE atendimentos DROP COLUMN assunto;
