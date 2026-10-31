package br.com.ccbank.cliente;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra arquivos locais do cliente com AES/CBC/PKCS5Padding. Formato: Base64(IV de 16 bytes ‖ cifrado).
 *
 * <p>v1: a chave fica fixa no código, de propósito — quem tem o .jar decifra os arquivos (ataque #14).
 */
public final class CifraArquivo {

    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";
    private static final int TAMANHO_IV = 16;
    /** Chave AES de 128 bits fixa no código. */
    private static final byte[] CHAVE = "CCBank-Lab-AES16".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CifraArquivo() {
    }

    public static String cifrar(String texto) {
        try {
            byte[] iv = new byte[TAMANHO_IV];
            ALEATORIO.nextBytes(iv);
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(CHAVE, "AES"), new IvParameterSpec(iv));
            byte[] cifrado = cifra.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] tudo = Arrays.copyOf(iv, TAMANHO_IV + cifrado.length);
            System.arraycopy(cifrado, 0, tudo, TAMANHO_IV, cifrado.length);
            return Base64.getEncoder().encodeToString(tudo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES indisponível na JVM", e);
        }
    }

    /** @throws GeneralSecurityException se o conteúdo não for um valor gerado por {@link #cifrar} */
    public static String decifrar(String base64) throws GeneralSecurityException {
        byte[] tudo;
        try {
            tudo = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("conteúdo não é Base64", e);
        }
        if (tudo.length <= TAMANHO_IV) {
            throw new GeneralSecurityException("conteúdo curto demais");
        }
        Cipher cifra = Cipher.getInstance(ALGORITMO);
        cifra.init(Cipher.DECRYPT_MODE, new SecretKeySpec(CHAVE, "AES"),
                new IvParameterSpec(Arrays.copyOf(tudo, TAMANHO_IV)));
        byte[] texto = cifra.doFinal(tudo, TAMANHO_IV, tudo.length - TAMANHO_IV);
        return new String(texto, StandardCharsets.UTF_8);
    }
}
