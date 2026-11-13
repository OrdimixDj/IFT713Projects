package ca.usherbrooke.ift605.blockchain;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Chaîne de blocs reliés par des pointeurs de hachage. Propriété tamper-evident
 * : modifier une transaction d'un bloc passé change sa racine de Merkle (donc
 * son hash) et casse le lien avec le bloc suivant. {@link #isValid()} le
 * détecte.
 *
 * @param <T> le type des transactions
 */
public class Blockchain<T> {

    /**
     * Valeur de {@code previousHash} du bloc genesis, qui n'a pas de prédécesseur
     * (64 zéros).
     */
    public static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);

    private final List<Block<T>> blocks = new ArrayList<>();

    /**
     * Ajoute un bloc en tête de chaîne, sans validation (la validation est faite
     * par {@link #isValid()}).
     *
     * @param b le bloc à ajouter
     */
    public void addBlock(Block<T> b) {
        blocks.add(b);
    }

    /**
     * @param height la hauteur du bloc (0 = genesis)
     * @return le bloc à cette hauteur
     * @throws IndexOutOfBoundsException si la hauteur n'existe pas
     */
    public Block<T> getBlock(int height) {
        return blocks.get(height);
    }

    /**
     * @return le dernier bloc de la chaîne
     * @throws IndexOutOfBoundsException si la chaîne est vide
     */
    public Block<T> getLatestBlock() {
        return blocks.get(blocks.size() - 1);
    }

    /**
     * @return le nombre de blocs dans la chaîne
     */
    public int size() {
        return blocks.size();
    }

    /**
     * Vérifie l'intégrité de toute la chaîne : pour chaque bloc, la racine de
     * Merkle
     * correspond aux transactions et {@code previousHash} correspond au hash du
     * bloc précédent.
     *
     * @return {@code true} si aucune modification n'est détectée
     */
    public boolean isValid() {
        for (int i = 0; i < blocks.size(); i++) {
            Block<T> block = blocks.get(i);

            // Les transactions n'ont pas été modifiées : la racine recalculée doit
            // correspondre à celle de l'en-tête
            String merkleRoot = HexFormat.of().formatHex(
                    new MerkleTree<>(block.getTransactions()).getMerkleRoot());
            if (!merkleRoot.equals(block.getHeader().merkleRoot())) {
                return false;
            }

            // Le pointeur de hachage est intact : previousHash doit être le hash actuel
            // du bloc précédent
            String expectedPrevious = (i == 0) ? GENESIS_PREVIOUS_HASH : blocks.get(i - 1).computeHash();
            if (!expectedPrevious.equals(block.getHeader().previousHash())) {
                return false;
            }
        }
        return true;
    }
}