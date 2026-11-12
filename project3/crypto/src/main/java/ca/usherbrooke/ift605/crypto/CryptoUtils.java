package ca.usherbrooke.ift605.crypto;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;

public final class CryptoUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE58_ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private CryptoUtils() {}

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 pas disponible", e);
        }
    }

    public static String sha256Hex(String input) {
        return HexFormat.of().formatHex(sha256(input.getBytes(StandardCharsets.UTF_8)));
    }

    public static String randomNonce() {
        byte[] nonce = new byte[32]; // 256 bits
        RANDOM.nextBytes(nonce);
        return HexFormat.of().formatHex(nonce);
    }

    public static String commit(String msg, String nonce) {
        return sha256Hex(nonce + msg);
    }

    public static boolean verify(String commit, String msg, String nonce) {
        return commit(msg, nonce).equals(commit);
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1")); // = P-256
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("EC P-256 pas disponible", e);
        }
    }

    public static byte[] sign(byte[] data, PrivateKey sk) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(sk);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signature ECDSA impossible", e);
        }
    }

    public static boolean verify(byte[] data, byte[] sig, PublicKey pk) {
        try {
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(pk);
            signature.update(data);
            return signature.verify(sig);
        } catch (SignatureException e) {
            return false;
            // signature mal formée donc invalide
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Vérification ECDSA impossible", e);
        }
    }

    public static String publicKeyToAddress(PublicKey pk) {
        return addressFromBytes(pk.getEncoded());
    }

    // Même calcul que publicKeyToAddress, mais à partir des octets de la clé
    public static String addressFromBytes(byte[] encodedPublicKey) {
        return base58(sha256(encodedPublicKey));
    }

    // Reconstruit une PublicKey à partir de ses octets
    public static PublicKey decodePublicKey(byte[] encodedPublicKey) {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encodedPublicKey));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Clé publique invalide", e);
        }
    }

    private static String base58(byte[] input) {
        BigInteger n = new BigInteger(1, input);
        BigInteger base = BigInteger.valueOf(58);
        StringBuilder sb = new StringBuilder();
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(base);
            sb.append(BASE58_ALPHABET.charAt(qr[1].intValue()));
            n = qr[0];
        }
        for (byte b : input) {
            // chaque octet 0 en tête devient '1'
            if (b != 0) break;
            sb.append('1');
        }
        return sb.reverse().toString();
    }
}