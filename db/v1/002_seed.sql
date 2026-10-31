-- CC_Bank: dados iniciais (spec, seção 5.2).
-- Operadores: senha123. Contas: 123456.

INSERT INTO cooperativa (codigo, nome) VALUES
    ('0001', 'Cooperativa Alfa'),
    ('0002', 'Cooperativa Beta'),
    ('0003', 'Cooperativa Gama');

INSERT INTO pac (cooperativa_id, codigo, nome)
SELECT co.id, v.codigo, v.nome
FROM cooperativa co
CROSS JOIN (VALUES ('001', 'PAC Centro'), ('002', 'PAC Norte')) AS v(codigo, nome);

INSERT INTO terminal (pac_id, codigo)
SELECT p.id, v.codigo
FROM pac p
CROSS JOIN (VALUES ('001'), ('002')) AS v(codigo);

INSERT INTO operador (login, senha, nome, terminal_id)
SELECT v.login, 'senha123', v.nome, t.id
FROM (VALUES
    ('rodrigop',    'Rodrigo P.',    '0001', '001', '001'),
    ('carla.dias',  'Carla Dias',    '0001', '001', '002'),
    ('bruno.souza', 'Bruno Souza',   '0001', '002', '001'),
    ('diego.alves', 'Diego Alves',   '0001', '002', '002'),
    ('ana.lima',    'Ana Lima',      '0002', '001', '001'),
    ('elisa.rocha', 'Elisa Rocha',   '0002', '001', '002'),
    ('fabio.nunes', 'Fábio Nunes',   '0002', '002', '001'),
    ('gabi.melo',   'Gabriela Melo', '0002', '002', '002'),
    ('hugo.pires',  'Hugo Pires',    '0003', '001', '001'),
    ('iris.costa',  'Íris Costa',    '0003', '001', '002'),
    ('joao.reis',   'João Reis',     '0003', '002', '001'),
    ('karen.vaz',   'Karen Vaz',     '0003', '002', '002')
) AS v(login, nome, coop, pac, term)
JOIN cooperativa co ON co.codigo = v.coop
JOIN pac p ON p.cooperativa_id = co.id AND p.codigo = v.pac
JOIN terminal t ON t.pac_id = p.id AND t.codigo = v.term;

INSERT INTO cliente (nome, cpf) VALUES
    ('Maria Souza',      '00000000001'),
    ('João Pereira',     '00000000002'),
    ('Lucas Martins',    '00000000003'),
    ('Juliana Alves',    '00000000004'),
    ('Rafael Gomes',     '00000000005'),
    ('Fernanda Ribeiro', '00000000006'),
    ('Gustavo Carvalho', '00000000007'),
    ('Patrícia Rocha',   '00000000008'),
    ('Thiago Barbosa',   '00000000009'),
    ('Pedro Lima',       '00000000010'),
    ('Ana Costa',        '00000000011'),
    ('Bruno Teixeira',   '00000000012'),
    ('Camila Fernandes', '00000000013'),
    ('Daniel Moreira',   '00000000014'),
    ('Eduarda Nunes',    '00000000015'),
    ('Felipe Cardoso',   '00000000016'),
    ('Gabriela Dias',    '00000000017'),
    ('Henrique Castro',  '00000000018'),
    ('Isabela Freitas',  '00000000019'),
    ('Leonardo Pinto',   '00000000020'),
    ('Mariana Lopes',    '00000000021'),
    ('Nicolas Ramos',    '00000000022'),
    ('Olívia Mendes',    '00000000023'),
    ('Paulo Azevedo',    '00000000024'),
    ('Renata Correia',   '00000000025'),
    ('Sérgio Monteiro',  '00000000026'),
    ('Tatiana Vieira',   '00000000027'),
    ('Vinícius Araújo',  '00000000028'),
    ('Larissa Duarte',   '00000000029'),
    ('Marcos Batista',   '00000000030');

INSERT INTO conta (cooperativa_id, numero, cliente_id, senha, saldo)
SELECT co.id, v.numero, cl.id, '123456', v.saldo
FROM (VALUES
    ('0001', '100001', '00000000001', 1000.00),
    ('0001', '100002', '00000000002', 500.00),
    ('0001', '100003', '00000000003', 3200.00),
    ('0001', '100004', '00000000004', 150.75),
    ('0001', '100005', '00000000005', 7800.00),
    ('0001', '100006', '00000000006', 45.30),
    ('0001', '100007', '00000000007', 9999.99),
    ('0001', '100008', '00000000008', 2500.00),
    ('0001', '100009', '00000000009', 620.10),
    ('0001', '100010', '00000000010', 0.00),
    ('0002', '200001', '00000000011', 2000.00),
    ('0002', '200002', '00000000012', 1200.00),
    ('0002', '200003', '00000000013', 0.00),
    ('0002', '200004', '00000000014', 5400.00),
    ('0002', '200005', '00000000015', 310.00),
    ('0002', '200006', '00000000016', 8750.50),
    ('0002', '200007', '00000000017', 95.00),
    ('0002', '200008', '00000000018', 4100.00),
    ('0002', '200009', '00000000019', 0.50),
    ('0002', '200010', '00000000020', 6600.00),
    ('0003', '300001', '00000000021', 3000.00),
    ('0003', '300002', '00000000022', 10000.00),
    ('0003', '300003', '00000000023', 720.00),
    ('0003', '300004', '00000000024', 0.00),
    ('0003', '300005', '00000000025', 1890.00),
    ('0003', '300006', '00000000026', 450.00),
    ('0003', '300007', '00000000027', 2750.25),
    ('0003', '300008', '00000000028', 130.00),
    ('0003', '300009', '00000000029', 5000.00),
    ('0003', '300010', '00000000030', 880.00)
) AS v(coop, numero, cpf, saldo)
JOIN cooperativa co ON co.codigo = v.coop
JOIN cliente cl ON cl.cpf = v.cpf;
