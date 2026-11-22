delete from devolucoes;
delete from locacoes;
delete from bicicletas;
delete from clientes;
delete from usuarios where username in ('locacao.admin@test', 'locacao.cliente@test');
