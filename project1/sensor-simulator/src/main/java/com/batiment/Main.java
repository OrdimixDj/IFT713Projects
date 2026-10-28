package com.batiment;

import java.util.HashMap;
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
     * Démarre un thread de simulation par pièce.
     * @param args températures de base optionnelles au format {@code piece=valeur}
     * @throws Exception si la connexion au broker MQTT échoue
     */
    public static void main(String[] args) throws Exception {
        String[] pieces = {"salleA", "salleB", "cuisine"};

        // Température de base de chaque pièce : 21 °C, sauf si elle est précisée en argument
        Map<String, Double> bases = new HashMap<>();

        for (String piece : pieces) {
            bases.put(piece, 21.0);
        }

        for (String arg : args) {
            String[] kv = arg.split("=");
            bases.put(kv[0], Double.parseDouble(kv[1]));
        }

        // Un client MQTT par pièce, avec un identifiant unique, et un thread par simulateur
        for (String piece : pieces) {
            MqttClient client = new MqttClient("tcp://localhost:1883", "simulateur-" + piece, new MemoryPersistence());
            client.connect();

            Thread t = new Thread(new CapteurSimulateur(piece, client, bases.get(piece)));
            t.start();
        }
    }

}