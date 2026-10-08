package com.batiment;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Main {
    public static void main(String[] args) throws Exception {
        String[] pieces = {"salleA", "salleB", "cuisine"};

        for (String piece : pieces) {
            MqttClient client = new MqttClient("tcp://localhost:1883", "simulateur-" + piece, new MemoryPersistence());
            client.connect();

            Thread t = new Thread(new CapteurSimulateur(piece, client));
            t.start();

            client.publish("batiment/hello", new MqttMessage("salut".getBytes()));
        }
    }
}