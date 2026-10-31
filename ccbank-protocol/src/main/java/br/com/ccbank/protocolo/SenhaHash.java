package br.com.ccbank.protocolo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash das senhas no protocolo: MD5 em hexadecimal minúsculo, sobre os bytes UTF-8, sem salt.
 * v1: fraco de propósito (o hash vale como a senha e é quebrável por força bruta).
 */
public final class SenhaHash {

    private SenhaHash() {
    }

    public static String md5(String senha) {
        if (senha == null) {
            throw new IllegalArgumentException("senha nula");
        }
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(32);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 indisponível na JVM", e);
        }
    }
}
