package ca.usherbrooke.ift605.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Associe une adresse à un nom lisible (Alice, Bob…), uniquement pour l'affichage dans la console
public final class AddressBook {

    private static final Map<String, String> NAMES = new ConcurrentHashMap<>();

    private AddressBook() {}

    public static void register(String address, String name) {
        NAMES.put(address, name);
    }

    public static String nameOf(String address) {
        return NAMES.getOrDefault(address, address.substring(0, 8) + "…");
    }
}