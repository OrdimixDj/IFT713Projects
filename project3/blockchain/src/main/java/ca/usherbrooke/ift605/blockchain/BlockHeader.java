package ca.usherbrooke.ift605.blockchain;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * En-tête d'un bloc : c'est lui qui est haché pour obtenir le hash du bloc.
 *
 * @param version      la version du format de bloc
 * @param previousHash le hash du bloc précédent (pointeur de hachage)
 * @param merkleRoot   la racine de Merkle des transactions, en hexadécimal
 * @param timestamp    la date de création, en millisecondes
 * @param nonce        la valeur que le mineur fait varier pour la preuve de
 *                     travail
 */
public record BlockHeader(int version, String previousHash, String merkleRoot, long timestamp, long nonce)
        implements Serializable {
    /**
     * Sérialise l'en-tête en octets en concaténant les champs pour le hachage.
     *
     * @return les octets de l'en-tête, en UTF-8
     */
    public byte[] toBytes() {
        String s = version + previousHash + merkleRoot + timestamp + nonce;
        return s.getBytes(StandardCharsets.UTF_8);
    }
}