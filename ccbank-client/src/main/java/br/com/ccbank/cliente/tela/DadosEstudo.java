package br.com.ccbank.cliente.tela;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contas e operadores do seed (db/&lt;lado&gt;/002_seed.sql), para os seletores de conveniência da tela.
 * As senhas são as de estudo; só facilitam o preenchimento, não mudam o protocolo.
 */
final class DadosEstudo {

    static final String SENHA_CONTA = "123456";
    static final String SENHA_OPERADOR = "senha123";

    static final List<OperadorEstudo> OPERADORES = Collections.unmodifiableList(Arrays.asList(
            new OperadorEstudo("rodrigop", "Rodrigo P."),
            new OperadorEstudo("carla.dias", "Carla Dias"),
            new OperadorEstudo("bruno.souza", "Bruno Souza"),
            new OperadorEstudo("diego.alves", "Diego Alves"),
            new OperadorEstudo("ana.lima", "Ana Lima"),
            new OperadorEstudo("elisa.rocha", "Elisa Rocha"),
            new OperadorEstudo("fabio.nunes", "Fábio Nunes"),
            new OperadorEstudo("gabi.melo", "Gabriela Melo"),
            new OperadorEstudo("hugo.pires", "Hugo Pires"),
            new OperadorEstudo("iris.costa", "Íris Costa"),
            new OperadorEstudo("joao.reis", "João Reis"),
            new OperadorEstudo("karen.vaz", "Karen Vaz")));

    static final List<ContaEstudo> CONTAS = Collections.unmodifiableList(construirContas());

    private DadosEstudo() {
    }

    private static List<ContaEstudo> construirContas() {
        List<ContaEstudo> contas = new ArrayList<>();
        contas.add(new ContaEstudo("0001", "100001", "Maria Souza"));
        contas.add(new ContaEstudo("0001", "100002", "João Pereira"));
        contas.add(new ContaEstudo("0001", "100003", "Lucas Martins"));
        contas.add(new ContaEstudo("0001", "100004", "Juliana Alves"));
        contas.add(new ContaEstudo("0001", "100005", "Rafael Gomes"));
        contas.add(new ContaEstudo("0001", "100006", "Fernanda Ribeiro"));
        contas.add(new ContaEstudo("0001", "100007", "Gustavo Carvalho"));
        contas.add(new ContaEstudo("0001", "100008", "Patrícia Rocha"));
        contas.add(new ContaEstudo("0001", "100009", "Thiago Barbosa"));
        contas.add(new ContaEstudo("0001", "100010", "Pedro Lima"));
        contas.add(new ContaEstudo("0002", "200001", "Ana Costa"));
        contas.add(new ContaEstudo("0002", "200002", "Bruno Teixeira"));
        contas.add(new ContaEstudo("0002", "200003", "Camila Fernandes"));
        contas.add(new ContaEstudo("0002", "200004", "Daniel Moreira"));
        contas.add(new ContaEstudo("0002", "200005", "Eduarda Nunes"));
        contas.add(new ContaEstudo("0002", "200006", "Felipe Cardoso"));
        contas.add(new ContaEstudo("0002", "200007", "Gabriela Dias"));
        contas.add(new ContaEstudo("0002", "200008", "Henrique Castro"));
        contas.add(new ContaEstudo("0002", "200009", "Isabela Freitas"));
        contas.add(new ContaEstudo("0002", "200010", "Leonardo Pinto"));
        contas.add(new ContaEstudo("0003", "300001", "Mariana Lopes"));
        contas.add(new ContaEstudo("0003", "300002", "Nicolas Ramos"));
        contas.add(new ContaEstudo("0003", "300003", "Olívia Mendes"));
        contas.add(new ContaEstudo("0003", "300004", "Paulo Azevedo"));
        contas.add(new ContaEstudo("0003", "300005", "Renata Correia"));
        contas.add(new ContaEstudo("0003", "300006", "Sérgio Monteiro"));
        contas.add(new ContaEstudo("0003", "300007", "Tatiana Vieira"));
        contas.add(new ContaEstudo("0003", "300008", "Vinícius Araújo"));
        contas.add(new ContaEstudo("0003", "300009", "Larissa Duarte"));
        contas.add(new ContaEstudo("0003", "300010", "Marcos Batista"));
        return contas;
    }
}
