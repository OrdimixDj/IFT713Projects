package ca.usherbrooke.ift605.blockchain;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MerkleTree<T> {

    // levels.get(0) = feuilles, levels.get(last) = [racine]
    private final List<List<byte[]>> levels = new ArrayList<>();

    public MerkleTree(List<T> transactions) {
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Un arbre de Merkle a besoin d'au moins une transaction");
        }
        List<byte[]> current = new ArrayList<>();
        for (T tx : transactions) {
            current.add(leafHash(tx));
        }
        levels.add(current);
        while (current.size() > 1) {
            List<byte[]> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                byte[] left = current.get(i);
                // Si le nombre de feuilles est impair, on duplique la dernière
                byte[] right = (i + 1 < current.size()) ? current.get(i + 1) : left;
                next.add(hashPair(left, right));
            }
            levels.add(next);
            current = next;
        }
    }

    public static <T> byte[] leafHash(T tx) {
        return CryptoUtils.sha256(tx.toString().getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getMerkleRoot() {
        return levels.get(levels.size() - 1).get(0);
    }

    public List<byte[]> getMerkleProof(int txIndex) {
        List<byte[]> proof = new ArrayList<>();
        int index = txIndex;
        for (int level = 0; level < levels.size() - 1; level++) {
            List<byte[]> nodes = levels.get(level);
            int sibling = index ^ 1; // voisin de gauche ou de droite
            proof.add(sibling < nodes.size() ? nodes.get(sibling) : nodes.get(index));
            index /= 2;
        }
        return proof;
    }

    public static boolean verifyProof(byte[] txHash, List<byte[]> proof, byte[] root) {
        byte[] current = txHash;
        for (byte[] sibling : proof) {
            current = hashPair(current, sibling);
        }
        return Arrays.equals(current, root);
    }

    // Nœud interne : SHA-256 des deux enfants, concaténés dans l'ordre croissant
    private static byte[] hashPair(byte[] a, byte[] b) {
        boolean aFirst = Arrays.compareUnsigned(a, b) <= 0;
        byte[] first = aFirst ? a : b;
        byte[] second = aFirst ? b : a;
        byte[] concat = new byte[first.length + second.length];
        System.arraycopy(first, 0, concat, 0, first.length);
        System.arraycopy(second, 0, concat, first.length, second.length);
        return CryptoUtils.sha256(concat);
    }
}