package com.batiment.agent;

import java.rmi.server.UnicastRemoteObject;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.rmi.RemoteException;

import com.batiment.api.IAgentPiece;

/**
 * Agent logiciel responsable d'une seule pièce du bâtiment.
 * Les champs de mesure sont écrits par le thread du callback MQTT et lus par les
 * threads RMI : ils sont donc {@code volatile}, ce qui garantit que chaque lecture
 * voit la dernière valeur écrite.
 */
public class AgentPiece extends UnicastRemoteObject implements IAgentPiece {
    /** Nom de la pièce (ex: salleA, salleB ou cuisine), utilisé dans les topics MQTT. */
    private final String nom;
    /** Dernière température reçue, en °C. */
    private volatile double temperature;
    /** Dernière luminosité reçue, en lux. */
    private volatile int luminosite;
    /** Dernier état d'occupation reçu. */
    private volatile boolean occupation;

    /** Dernière consigne de thermostat envoyée via {@link #setConsigneTemperature(double)}. */
    private volatile double consigne;

    /** Client MQTT propre à cet agent. */
    private final MqttClient client;

    /** Adresse du broker Mosquitto local. */
    private static final String BROKER_URL = "tcp://localhost:1883";

    /**
     * Crée l'agent, l'exporte comme objet RMI, se connecte au broker MQTT et s'abonne aux capteurs de la pièce.
     * @param nom nom de la pièce (ex: salleA, salleB ou cuisine)
     * @throws RemoteException si l'export RMI échoue ou si la connexion MQTT est impossible
     */
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

    /**
     * Callback MQTT : met à jour le champ correspondant au type de capteur.
     * Un message mal formé est ignoré et journalisé, pour ne pas interrompre l'agent.
     * @param topic topic MQTT sur lequel la mesure a été reçue
     * @param msg message MQTT reçu ; son contenu (payload) est la valeur mesurée sous forme de texte
     */
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

    /**
     * Renvoie la dernière température reçue du capteur de la pièce.
     * @return la température en °C
     * @throws RemoteException si l'agent est injoignable
     */
    public double getTemperature() throws RemoteException {
        return temperature;
    }

    /**
     * Renvoie la dernière luminosité reçue du capteur de la pièce.
     * @return la luminosité en lux
     * @throws RemoteException si l'agent est injoignable
     */
    public int getLuminosite() throws RemoteException {
        return luminosite;
    }

    /**
     * Indique si la pièce est occupée, d'après le dernier message du capteur d'occupation.
     * @return l'occupation sous forme de booléen
     * @throws RemoteException si l'agent est injoignable
     */
    public boolean isOccupee() throws RemoteException {
        return occupation;
    }

    /**
     * Publie la nouvelle consigne du thermostat sur le topic MQTT puis la mémorise
     * @param temperatureDemandee nouvelle consigne en °C
     * @throws RemoteException si la publication MQTT échoue
     */
    public void setConsigneTemperature(double temperatureDemandee) throws RemoteException {
        try {
            String topic = "batiment/" + nom + "/actuateur/thermostat";
            client.publish(topic, new MqttMessage(String.valueOf(temperatureDemandee).getBytes()));
            consigne = temperatureDemandee;
        } catch (MqttException e) {
            throw new RemoteException("Publication de la consigne impossible pour " + nom, e);
        }
    }

    /**
     * Renvoie l'état de la pièce au format JSON
     * (pièce, température, luminosité, occupation, consigne).
     * @return l'état de la pièce sous forme de chaîne JSON
     * @throws RemoteException si l'agent est injoignable
     */
    public String getEtatJson() throws RemoteException {
        return String.format(Locale.ROOT,
                "{\"piece\":\"%s\",\"temperature\":%.1f,\"luminosite\":%d,\"occupee\":%b,\"consigne\":%.1f}",
                nom, temperature, luminosite, occupation, consigne);
    }

}
