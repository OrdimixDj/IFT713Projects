package ca.usherbrooke.ift605.network;

import java.util.function.BooleanSupplier;

final class NetworkTestUtils {

    private NetworkTestUtils() {}

    static boolean waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(20);
        }
        return condition.getAsBoolean();
    }
}