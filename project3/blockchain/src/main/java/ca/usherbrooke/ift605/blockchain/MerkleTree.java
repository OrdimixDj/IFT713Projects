package ca.usherbrooke.ift605.blockchain;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Arbre de Merkle construit à partir d'une liste de transactions.
 * Les feuilles sont {@code SHA-256(tx)} ; chaque nœud interne est le SHA-256 de
 * ses deux enfants.
 * Si un niveau a un nombre impair de nœuds, le dernier est dupliqué.
 * Les deux enfants sont concaténés dans l'ordre croissant, car
 * {@link #verifyProof} ne reçoit
 * pas la position (gauche/droite) de la transaction.
 *
 * @param <T> le type des transactions (feuilles)
 */
public class MerkleTree<T> {

    // Tous les niveaux de l'arbre, gardés en mémoire pour construire les preuves :
    // levels.get(0) = feuilles, levels.get(last) = [racine]
    private final List<List<byte[]>> levels = new ArrayList<>();

    /**
     * Construit l'arbre complet, des feuilles jusqu'à la racine.
     *
     * @param transactions les transactions du bloc, dans l'ordre
     * @throws IllegalArgumentException si la liste est vide
     */
    public MerkleTree(List<T> transactions) {
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Un arbre de Merkle a besoin d'au moins une transaction");
        }
        List<byte[]> current = new ArrayList<>();
        for (T tx : transactions) {
            current.add(leafHash(tx));
        }
        levels.add(current);
        // On remonte niveau par niveau : chaque paire de nœuds donne un parent, jusqu'à
        // n'avoir plus qu'un nœud (la racine)
        while (current.size() > 1) {
            List<byte[]> next = new ArrayList<>();
            for (int i = 0; i < current.size(); i += 2) {
                byte[] left = current.get(i);
                // Si le niveau a un nombre impair de nœuds, le dernier est apparié avec
                // lui-même
                byte[] right = (i + 1 < current.size()) ? current.get(i + 1) : left;
                next.add(hashPair(left, right));
            }
            levels.add(next);
            current = next;
        }
    }

    /**
     * Calcule le hachage d'une feuille : {@code SHA-256(tx.toString())} en UTF-8.
     *
     * @param tx  la transaction
     * @param <T> le type de la transaction
     * @return le hachage de la feuille (32 octets)
     */
    public static <T> byte[] leafHash(T tx) {
        return CryptoUtils.sha256(tx.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Retourne la racine de Merkle, qui résume toutes les transactions.
     *
     * @return la racine (32 octets)
     */
    public byte[] getMerkleRoot() {
        return levels.get(levels.size() - 1).get(0);
    }

    /**
     * Génère la preuve d'appartenance d'une transaction : la liste des hachages
     * voisins, de la feuille jusqu'à la racine.
     *
     * @param txIndex la position de la transaction dans le bloc
     * @return les hachages voisins, du bas vers le haut
     */
    public List<byte[]> getMerkleProof(int txIndex) {
        List<byte[]> proof = new ArrayList<>();
        int index = txIndex;
        // À chaque niveau (sauf la racine), on ajoute le voisin du nœud courant, puis
        // on monte vers son parent
        for (int level = 0; level < levels.size() - 1; level++) {
            List<byte[]> nodes = levels.get(level);
            // index ^ 1 inverse le dernier bit
            int sibling = index ^ 1;
            // Pas de voisin : il a été apparié avec lui-même
            proof.add(sibling < nodes.size() ? nodes.get(sibling) : nodes.get(index));
            index /= 2; // position du parent au niveau supérieur
        }
        return proof;
    }

    /**
     * Vérifie une preuve d'appartenance en remontant de la feuille jusqu'à la
     * racine.
     *
     * @param txHash le hachage de la transaction ({@link #leafHash})
     * @param proof  la preuve obtenue avec {@link #getMerkleProof}
     * @param root   la racine de Merkle attendue (celle de l'en-tête du bloc)
     * @return {@code true} si la transaction appartient bien à l'arbre de cette
     *         racine
     */
    public static boolean verifyProof(byte[] txHash, List<byte[]> proof, byte[] root) {
        // On recalcule le chemin de la feuille jusqu'à la racine avec les voisins
        // fournis
        byte[] current = txHash;
        for (byte[] sibling : proof) {
            current = hashPair(current, sibling);
        }
        // La transaction appartient à l'arbre si on retombe exactement sur la racine
        // publiée
        return Arrays.equals(current, root);
    }

    // Nœud interne : SHA-256 des deux enfants concaténés, le plus petit en premier.
    // Ce tri rend le résultat indépendant de l'ordre gauche/droite : verifyProof
    // n'a donc pas besoin de connaître la position de la transaction, ce qui est
    // une différence avec le vrai Bitcoin
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