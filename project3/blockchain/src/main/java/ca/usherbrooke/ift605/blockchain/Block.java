package ca.usherbrooke.ift605.blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

/**
 * Bloc de la chaîne : un en-tête et une liste de transactions.
 * Un bloc est immuable, c'est-à-dire que le minage crée une copie avec un autre
 * nonce.
 *
 * @param <T> le type des transactions
 */
public class Block<T> implements Serializable {

    private final BlockHeader header;
    private final List<T> transactions;

    /**
     * Crée un bloc non miné (nonce = 0) ; la racine de Merkle est calculée
     * automatiquement.
     *
     * @param previousHash le hash du bloc précédent
     * @param transactions les transactions du bloc (au moins une)
     */
    public Block(String previousHash, List<T> transactions) {
        this.transactions = new ArrayList<>(transactions);
        String merkleRoot = HexFormat.of().formatHex(new MerkleTree<>(this.transactions).getMerkleRoot());
        this.header = new BlockHeader(1, previousHash, merkleRoot, System.currentTimeMillis(), 0);
    }

    // Utilisé par withNonce : réutilise l'en-tête et les transactions sans
    // recalculer la racine de Merkle
    private Block(BlockHeader header, List<T> transactions) {
        this.header = header;
        this.transactions = transactions;
    }

    /**
     * Retourne une copie du bloc avec un autre nonce, utilisé par le mineur.
     *
     * @param nonce le nouveau nonce
     * @return le nouveau bloc, mêmes transactions et même en-tête sauf le nonce
     */
    public Block<T> withNonce(long nonce) {
        BlockHeader h = new BlockHeader(header.version(), header.previousHash(), header.merkleRoot(),
                header.timestamp(), nonce);
        return new Block<>(h, transactions);
    }

    /**
     * @return l'en-tête du bloc
     */
    public BlockHeader getHeader() {
        return header;
    }

    /**
     * @return les transactions du bloc
     */
    public List<T> getTransactions() {
        return transactions;
    }

    /**
     * Calcule le hash du bloc : double SHA-256 de l'en-tête, comme dans Bitcoin.
     *
     * @return le hash en hexadécimal (64 caractères)
     */
    public String computeHash() {
        return HexFormat.of().formatHex(CryptoUtils.sha256(CryptoUtils.sha256(header.toBytes())));
    }
}