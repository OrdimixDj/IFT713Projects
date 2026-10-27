package com.batiment;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Main {
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

        for (String piece : pieces) {
            MqttClient client = new MqttClient("tcp://localhost:1883", "simulateur-" + piece, new MemoryPersistence());
            client.connect();

            Thread t = new Thread(new CapteurSimulateur(piece, client, bases.get(piece)));
            t.start();
        }
    }

}