package com.batiment;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;

public class CapteurSimulateur implements Runnable {
    private final String piece;
    private final MqttClient client;
    private final Random rnd = new Random();

    public CapteurSimulateur(String piece, MqttClient client) {
        this.piece = piece;
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            try {
            publishTemp();
            publishLuminosite();
            publishOccupation();
            Thread.sleep(5000);
            } catch (MqttException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void publishTemp() throws MqttException {
        int temp = rnd.nextInt(12) + 18;
        client.publish("batiment/" + piece + "/capteur/temperature", new MqttMessage(String.valueOf(temp).getBytes()));
    }

    private void publishLuminosite() throws MqttException {
        int lum = rnd.nextInt(1000);
        client.publish("batiment/" + piece + "/capteur/luminosite", new MqttMessage(String.valueOf(lum).getBytes()));
    }

    private void publishOccupation() throws MqttException {
        boolean occ = rnd.nextBoolean();
        client.publish("batiment/" + piece + "/capteur/occupation", new MqttMessage(String.valueOf(occ).getBytes()));
    }
}