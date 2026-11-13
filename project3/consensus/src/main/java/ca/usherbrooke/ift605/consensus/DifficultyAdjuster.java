package ca.usherbrooke.ift605.consensus;

import ca.usherbrooke.ift605.blockchain.Blockchain;

/**
 * Ajustement de la difficulté : tous les 10 blocs, la difficulté est ajustée
 * pour garder un temps de minage cible de 10 secondes par bloc.
 */
public final class DifficultyAdjuster {

    /** Nombre de blocs entre deux ajustements. */
    public static final int ADJUSTMENT_INTERVAL = 10;

    /**
     * Temps de minage visé par bloc, en millisecondes (ici 10 000 millisecondes
     * donc 10 secondes).
     */
    public static final long TARGET_BLOCK_TIME_MS = 10_000;

    private DifficultyAdjuster() {
    }

    /**
     * Calcule la nouvelle difficulté : +1 si les blocs ont été minés deux fois trop
     * vite, −1 (minimum 1) s'ils ont été minés deux fois trop lentement.
     *
     * @param currentDifficulty la difficulté actuelle
     * @param actualTime        le temps réellement écoulé, en millisecondes
     * @param targetTime        le temps visé, en millisecondes
     * @return la nouvelle difficulté
     */
    public static int adjust(int currentDifficulty, long actualTime, long targetTime) {
        int newDifficulty = currentDifficulty;
        if (actualTime < targetTime / 2)
            newDifficulty++;
        if (actualTime > targetTime * 2)
            newDifficulty = Math.max(1, newDifficulty - 1);
        return newDifficulty;
    }

    /**
     * À appeler après chaque ajout de bloc : tous les 10 blocs, compare le temps
     * écoulé entre le 1er et le 10e bloc de la fenêtre (9 intervalles d'après les
     * timestamps) au temps visé, puis ajuste avec {@link #adjust}.
     *
     * @param chain             la chaîne
     * @param currentDifficulty la difficulté actuelle
     * @return la nouvelle difficulté, qui reste inchangée hors des multiples de 10
     *         blocs
     */
    public static int nextDifficulty(Blockchain<?> chain, int currentDifficulty) {
        // On n'ajuste qu'aux multiples de 10 blocs ; le reste du temps, la difficulté
        // ne change pas
        if (chain.size() < ADJUSTMENT_INTERVAL || chain.size() % ADJUSTMENT_INTERVAL != 0) {
            return currentDifficulty;
        }
        long first = chain.getBlock(chain.size() - ADJUSTMENT_INTERVAL).getHeader().timestamp();
        long last = chain.getLatestBlock().getHeader().timestamp();
        long actualTime = last - first; // durée des 9 intervalles
        // 10 blocs = 9 intervalles entre le 1er et le dernier timestamp, donc la cible
        // est de 9 × 10 s
        long targetTime = TARGET_BLOCK_TIME_MS * (ADJUSTMENT_INTERVAL - 1);
        return adjust(currentDifficulty, actualTime, targetTime);
    }
}