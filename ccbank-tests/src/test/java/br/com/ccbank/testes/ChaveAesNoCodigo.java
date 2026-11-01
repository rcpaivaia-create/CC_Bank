package br.com.ccbank.testes;

import br.com.ccbank.cliente.CifraArquivo;

/** #14: a chave AES fixa no código decifra o arquivo de último login. */
final class ChaveAesNoCodigo implements Ataque {

    @Override
    public String id() {
        return "chave-aes-no-codigo";
    }

    @Override
    public String descricao() {
        return "Chave AES no código decifra dados/ultimo-login-*.dat";
    }

    @Override
    public Veredito avaliar(ContextoAtaque contexto) throws Exception {
        String login = "rodrigop";
        String cifrado = CifraArquivo.cifrar(login);
        String recuperado = CifraArquivo.decifrar(cifrado);
        return login.equals(recuperado) ? Veredito.VULNERAVEL : Veredito.PROTEGIDO;
    }
}
