package com.batiment.agent;

import java.rmi.server.UnicastRemoteObject;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.rmi.RemoteException;

import com.batiment.api.IAgentPiece;

public class AgentPiece extends UnicastRemoteObject implements IAgentPiece {
    private final String nom;
    private volatile double temperature;
    private volatile int luminosite;
    private volatile boolean occupation;

    private volatile double consigne;

    private final MqttClient client;
    private static final String BROKER_URL = "tcp://localhost:1883";

    public AgentPiece(String nom) throws RemoteException {
        super();
        this.nom = nom;
        this.temperature = 0.0;
        this.luminosite = 0;
        this.occupation = false;
        this.consigne = 21.0;

        try {
            client = new MqttClient(BROKER_URL, "agent-" + nom, new MemoryPersistence());
            client.connect();
            client.subscribe("batiment/" + nom + "/capteur/#", (topic, msg) -> traiterMesure(topic, msg));
        } catch (MqttException e) {
            throw new RemoteException("Connexion MQTT impossible pour " + nom, e);
        }
    }

    private void traiterMesure(String topic, MqttMessage msg) {
        String type = topic.substring(topic.lastIndexOf('/') + 1);
        String valeur = new String(msg.getPayload()).trim();

        try {
            switch (type) {
                case "temperature" -> temperature = Double.parseDouble(valeur);
                case "luminosite"  -> luminosite  = Integer.parseInt(valeur);
                case "occupation"  -> occupation  = Boolean.parseBoolean(valeur);
            }
        } catch (NumberFormatException e) {
            System.err.println("[" + nom + "] valeur invalide sur " + topic + " : " + valeur);
        }
    }

    public double getTemperature() throws RemoteException {
        return temperature;
    }

    public int getLuminosite() throws RemoteException {
        return luminosite;
    }

    public boolean isOccupee() throws RemoteException {
        return occupation;
    }

    public void setConsigneTemperature(double temperatureDemandee) throws RemoteException {
        try {
            String topic = "batiment/" + nom + "/actuateur/thermostat";
            client.publish(topic, new MqttMessage(String.valueOf(temperatureDemandee).getBytes()));
            consigne = temperatureDemandee;
        } catch (MqttException e) {
            throw new RemoteException("Publication de la consigne impossible pour " + nom, e);
        }
    }

    public String getEtatJson() throws RemoteException {
        return String.format(Locale.ROOT,
                "{\"piece\":\"%s\",\"temperature\":%.1f,\"luminosite\":%d,\"occupee\":%b,\"consigne\":%.1f}",
                nom, temperature, luminosite, occupation, consigne);
    }

}
