package com.batiment;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;

/**
 * Simule les trois capteurs d'une pièce (température, luminosité, occupation)
 * et publie une mesure de chaque type toutes les 5 secondes sur
 * {@code batiment/{piece}/capteur/{type}}.
 */
public class CapteurSimulateur implements Runnable {
    /** Nom de la pièce simulée, utilisé dans les topics MQTT. */
    private final String piece;
    /** Client MQTT déjà connecté, utilisé pour publier. */
    private final MqttClient client;
    /** Température moyenne de la pièce ; la mesure publiée varie de ±0,5 °C autour. */
    private final double temperatureBase;
    /** Générateur aléatoire des mesures. */
    private final Random rnd = new Random();

    /**
     * Crée le simulateur d'une pièce.
     * @param piece nom de la pièce (salleA, salleB ou cuisine)
     * @param client client MQTT déjà connecté au broker
     * @param temperatureBase température moyenne de la pièce, en °C
     */
    public CapteurSimulateur(String piece, MqttClient client, double temperatureBase) {
        this.piece = piece;
        this.client = client;
        this.temperatureBase = temperatureBase;
    }

    /**
     * Boucle de simulation : publie les trois mesures puis attend 5 secondes.
     * Une erreur de publication est journalisée sans arrêter la boucle.
     */
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

    /**
     * Publie une température aléatoire autour de la température de base.
     * {@code Locale.ROOT} force le point décimal (« 21.3 » et non « 21,3 »),
     * sinon les agents et openHAB ne pourraient pas lire la valeur.
     * @throws MqttException si la publication échoue
     */
    private void publishTemp() throws MqttException {
        double temp = temperatureBase + (rnd.nextDouble() - 0.5);
        String valeur = String.format(java.util.Locale.ROOT, "%.1f", temp);
        client.publish("batiment/" + piece + "/capteur/temperature", new MqttMessage(valeur.getBytes()));
    }

    /**
     * Publie une luminosité aléatoire entre 0 et 999 lux.
     * @throws MqttException si la publication échoue
     */
    private void publishLuminosite() throws MqttException {
        int lum = rnd.nextInt(1000);
        client.publish("batiment/" + piece + "/capteur/luminosite", new MqttMessage(String.valueOf(lum).getBytes()));
    }

    /**
     * Publie un état d'occupation aléatoire ({@code true} ou {@code false}).
     * @throws MqttException si la publication échoue
     */
    private void publishOccupation() throws MqttException {
        boolean occ = rnd.nextBoolean();
        client.publish("batiment/" + piece + "/capteur/occupation", new MqttMessage(String.valueOf(occ).getBytes()));
    }
}