insert into usuarios (username, password, role)
    values ('locacao.admin@test', 'test', 'ROLE_ADMIN');
insert into usuarios (username, password, role)
    values ('locacao.cliente@test', 'test', 'ROLE_CLIENTE');
insert into clientes (nome, cpf, usuario_id)
    select 'Cliente Locação', '12345678901', id
    from usuarios
    where username = 'locacao.cliente@test';
insert into bicicletas (codigo, status)
    values ('LOC-BIKE-001', 'LIVRE');
