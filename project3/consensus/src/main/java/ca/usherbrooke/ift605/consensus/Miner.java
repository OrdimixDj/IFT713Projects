package ca.usherbrooke.ift605.consensus;

import ca.usherbrooke.ift605.blockchain.Block;

/**
 * Mineur par preuve de travail.
 */
public final class Miner {

    private static final long MAX_NONCE = 1L << 32; // 2^32

    private Miner() {
    }

    /**
     * Mine un bloc : essaie les nonces de 0 à 2^32 jusqu'à ce que
     * {@code SHA-256(SHA-256(header))} commence par {@code difficulty} zéros
     * hexadécimaux. Affiche le nombre d'itérations et le temps de minage.
     *
     * @param b          le bloc à miner
     * @param difficulty le nombre de zéros hexadécimaux exigés
     * @param <T>        le type des transactions
     * @return le bloc miné, avec le nonce trouvé
     * @throws IllegalStateException si aucun nonce entre 0 et 2^32 ne convient
     */
    public static <T> Block<T> mine(Block<T> b, int difficulty) {
        long start = System.nanoTime();

        // Le hash est imprévisible : la seule méthode est d'essayer les nonces un par
        // un. Chaque zéro hexadécimal de plus multiplie le nombre moyen d'essais par 16
        for (long nonce = 0; nonce < MAX_NONCE; nonce++) {
            Block<T> candidate = b.withNonce(nonce);
            String hash = candidate.computeHash(); // SHA-256(SHA-256(header))
            if (meetsDifficulty(hash, difficulty)) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                System.out.printf("    [Miner] difficulté %d : %d itérations, %d ms, hash %s…%n",
                        difficulty, nonce + 1, ms, hash.substring(0, 16));
                return candidate;
            }
        }
        throw new IllegalStateException("Aucun nonce valide entre 0 et 2^32");
    }

    /**
     * Vérifie la condition de preuve de travail.
     * 
     * @param hash       le hash du bloc, en hexadécimal
     * @param difficulty le nombre de zéros hexadécimaux exigés
     * @return {@code true} si le hash commence par {@code difficulty} zéros
     */
    public static boolean meetsDifficulty(String hash, int difficulty) {
        return hash.startsWith("0".repeat(difficulty));
    }
}