package ca.usherbrooke.ift605.consensus;

import ca.usherbrooke.ift605.blockchain.Block;

public final class Miner {

    private static final long MAX_NONCE = 1L << 32; // 2^32

    private Miner() {}

    public static <T> Block<T> mine(Block<T> b, int difficulty) {
        long start = System.nanoTime();

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

    public static boolean meetsDifficulty(String hash, int difficulty) {
        return hash.startsWith("0".repeat(difficulty));
    }
}