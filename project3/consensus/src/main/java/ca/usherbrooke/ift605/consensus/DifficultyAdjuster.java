package ca.usherbrooke.ift605.consensus;

import ca.usherbrooke.ift605.blockchain.Blockchain;

public final class DifficultyAdjuster {

    public static final int ADJUSTMENT_INTERVAL = 10;          // tous les 10 blocs
    public static final long TARGET_BLOCK_TIME_MS = 10_000;    // 10 secondes par bloc

    private DifficultyAdjuster() {}

    public static int adjust(int currentDifficulty, long actualTime, long targetTime) {
        int newDifficulty = currentDifficulty;
        if (actualTime < targetTime / 2) newDifficulty++;
        if (actualTime > targetTime * 2) newDifficulty = Math.max(1, newDifficulty - 1);
        return newDifficulty;
    }

    // Appelée après chaque ajout de bloc : ne change la difficulté que tous les 10 blocs
    public static int nextDifficulty(Blockchain<?> chain, int currentDifficulty) {
        if (chain.size() < ADJUSTMENT_INTERVAL || chain.size() % ADJUSTMENT_INTERVAL != 0) {
            return currentDifficulty;
        }
        long first = chain.getBlock(chain.size() - ADJUSTMENT_INTERVAL).getHeader().timestamp();
        long last = chain.getLatestBlock().getHeader().timestamp();
        long actualTime = last - first;                                          // durée des 9 intervalles
        long targetTime = TARGET_BLOCK_TIME_MS * (ADJUSTMENT_INTERVAL - 1);
        return adjust(currentDifficulty, actualTime, targetTime);
    }
}