package ca.usherbrooke.ift605.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Annuaire qui associe une adresse à un nom lisible (Alice, Bob, etc.). Sert
 * uniquement à l'affichage dans la console ; ne joue aucun rôle dans la
 * validation.
 */
public final class AddressBook {

    private static final Map<String, String> NAMES = new ConcurrentHashMap<>();

    private AddressBook() {
    }

    /**
     * Enregistre le nom associé à une adresse.
     *
     * @param address l'adresse
     * @param name    le nom à afficher
     */
    public static void register(String address, String name) {
        NAMES.put(address, name);
    }

    /**
     * @param address l'adresse
     * @return le nom enregistré, ou les 8 premiers caractères de l'adresse suivis
     *         de "..."
     */
    public static String nameOf(String address) {
        return NAMES.getOrDefault(address, address.substring(0, 8) + "…");
    }
}