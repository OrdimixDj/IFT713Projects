package ca.usherbrooke.ift605.network;

import java.util.function.BooleanSupplier;

// Outils communs aux tests réseau
final class NetworkTestUtils {

    private NetworkTestUtils() {
    }

    // Attend qu'une condition devienne vraie, en prenant en compte au plus
    // timeoutMs millisecondes
    static boolean waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (condition.getAsBoolean())
                return true;
            Thread.sleep(20); // revérifie toutes les 20 ms
        }
        return condition.getAsBoolean();
    }
}