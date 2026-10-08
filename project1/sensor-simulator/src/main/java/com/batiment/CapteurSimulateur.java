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
        System.out.println("hello");
        while (true) {
            publishTemp();
            publishLuminosite();
            publishOccupation();
            Thread.sleep(5000);
        }
    }
    // TODO : implémenter publishTemp(), publishLuminosite(), publishOccupation()
}