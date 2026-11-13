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

/**
 * Primitives cryptographiques du projet CryptoLedger.
 * Regroupe le hachage SHA-256, l'engagement cryptographique, les signatures
 * ECDSA
 * sur la courbe P-256 et le calcul des adresses Base58.
 */
public final class CryptoUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

    private CryptoUtils() {
    }

    /**
     * Calcule l'empreinte SHA-256 d'un tableau d'octets.
     *
     * @param data les données à hacher (taille quelconque)
     * @return l'empreinte de 32 octets (256 bits)
     */
    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 pas disponible", e);
        }
    }

    /**
     * Calcule l'empreinte SHA-256 d'une chaîne encodée en UTF-8.
     *
     * @param input la chaîne à hacher
     * @return l'empreinte en hexadécimal (64 caractères)
     */
    public static String sha256Hex(String input) {
        return HexFormat.of().formatHex(sha256(input.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Génère un nonce aléatoire de 256 bits avec {@link SecureRandom}.
     * Le nonce dissimule l'engagement, c'est-à-dire qu'on ne peut pas deviner le
     * message.
     *
     * @return le nonce en hexadécimal (64 caractères)
     */
    public static String randomNonce() {
        byte[] nonce = new byte[32]; // 256 bits
        RANDOM.nextBytes(nonce);
        return HexFormat.of().formatHex(nonce);
    }

    /**
     * Crée un engagement cryptographique : {@code SHA-256(nonce || msg)}
     *
     * @param msg   le message auquel on s'engage
     * @param nonce le nonce secret, révélé plus tard avec le message
     * @return l'engagement en hexadécimal à publier
     */
    public static String commit(String msg, String nonce) {
        return sha256Hex(nonce + msg);
    }

    /**
     * Vérifie qu'un message et un nonce révélés correspondent à un engagement
     * publié.
     *
     * @param commit l'engagement publié auparavant
     * @param msg    le message révélé
     * @param nonce  le nonce révélé
     * @return {@code true} si {@code SHA-256(nonce || msg)} est égal à l'engagement
     */
    public static boolean verify(String commit, String msg, String nonce) {
        return commit(msg, nonce).equals(commit);
    }

    /**
     * Génère une paire de clés ECDSA (sk, pk) sur la courbe P-256 (secp256r1).
     *
     * @return la paire de clés privée et publique
     * @throws IllegalStateException si la courbe n'est pas disponible dans le JDK
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1")); // = P-256
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("EC P-256 pas disponible", e);
        }
    }

    /**
     * Signe des données avec ECDSA (SHA256withECDSA).
     *
     * @param data les données à signer
     * @param sk   la clé privée du signataire
     * @return la signature
     * @throws IllegalStateException si la signature échoue
     */
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

    /**
     * Vérifie une signature ECDSA.
     *
     * @param data les données supposément signées
     * @param sig  la signature à vérifier
     * @param pk   la clé publique du signataire
     * @return {@code true} si la signature est valide ; {@code false} si elle est
     *         invalide ou mal formée
     */
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

    /**
     * Calcule l'adresse associée à une clé publique : {@code Base58(SHA-256(pk))}.
     *
     * @param pk la clé publique
     * @return l'adresse en Base58
     */
    public static String publicKeyToAddress(PublicKey pk) {
        return addressFromBytes(pk.getEncoded());
    }

    /**
     * Calcule l'adresse à partir des octets encodés (X.509) d'une clé publique.
     * Même calcul que {@link #publicKeyToAddress(PublicKey)} ; utile quand la clé
     * arrive sous forme d'octets.
     *
     * @param encodedPublicKey la clé publique encodée
     * @return l'adresse en Base58
     */
    public static String addressFromBytes(byte[] encodedPublicKey) {
        return base58(sha256(encodedPublicKey));
    }

    /**
     * Reconstruit une clé publique EC à partir de ses octets encodés (X.509).
     *
     * @param encodedPublicKey la clé publique encodée
     * @return la clé publique
     * @throws IllegalArgumentException si les octets ne forment pas une clé EC
     *                                  valide
     */
    public static PublicKey decodePublicKey(byte[] encodedPublicKey) {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encodedPublicKey));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Clé publique invalide", e);
        }
    }

    // Encode des octets en Base58
    private static String base58(byte[] input) {
        // Les octets sont lus comme un grand nombre positif, converti en base 58
        BigInteger n = new BigInteger(1, input);
        BigInteger base = BigInteger.valueOf(58);
        StringBuilder sb = new StringBuilder();
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(base); // qr[0] = quotient, qr[1] = reste (le chiffre)
            sb.append(BASE58_ALPHABET.charAt(qr[1].intValue()));
            n = qr[0];
        }
        // Les zéros de tête disparaissent dans la conversion en nombre :
        // chaque octet 0 en tête est donc encodé par un '1' pour ne pas perdre
        // d'information
        for (byte b : input) {
            if (b != 0)
                break;
            sb.append('1');
        }
        return sb.reverse().toString();
    }
}