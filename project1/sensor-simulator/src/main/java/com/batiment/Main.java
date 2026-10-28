package com.batiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Lance un simulateur de capteurs par pièce, chacun avec son propre client MQTT.
 * Par défaut, toutes les pièces sont à 21 °C. On peut forcer la température d'une
 * pièce pour créer volontairement un déséquilibre, par exemple {@code salleA=28}.
 */
public class Main {
    /**
     * Démarre un thread de simulation par pièce sélectionnée.
     * Lancer une pièce dans sa propre JVM permet de couper son capteur seul (Ctrl+C)
     * pour tester la tolérance aux pannes.
     * @param args pièces à simuler et/ou températures de base:
     * <ul>
     *   <li>un nom de pièce ({@code salleA}, {@code salleB}, {@code cuisine}) : ne simule
     *       que les pièces citées ; sans nom de pièce, les trois sont simulées ;</li>
     *   <li>{@code piece=valeur} : force la température de base d'une pièce,
     *       par exemple {@code salleA=28}.</li>
     * </ul>
     * @throws Exception si la connexion au broker MQTT échoue
     */
    public static void main(String[] args) throws Exception {
        List<String> pieces = new ArrayList<>();
        Map<String, Double> bases = new HashMap<>();

        for (String arg : args) {
            if (arg.contains("=")) {
                // Température de base forcée, par exemple salleA=28
                String[] kv = arg.split("=");
                bases.put(kv[0], Double.parseDouble(kv[1]));
            } else {
                // Nom d'une pièce à simuler
                pieces.add(arg);
            }
        }
        // Sans pièce précisée : on simule tout le bâtiment
        if (pieces.isEmpty()) {
            pieces = List.of("salleA", "salleB", "cuisine");
        }

        // Un client MQTT par pièce, avec un identifiant unique, et un thread par simulateur
        for (String piece : pieces) {
            MqttClient client = new MqttClient("tcp://localhost:1883", "simulateur-" + piece, new MemoryPersistence());
            client.connect();

            double base = bases.getOrDefault(piece, 21.0);
            new Thread(new CapteurSimulateur(piece, client, base)).start();
            System.out.println("Simulateur " + piece + " démarré (base " + base + " °C)");
        }
    }
}
