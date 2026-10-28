package com.batiment.api;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface distante d'un agent de pièce, partagée entre les agents (serveurs RMI)
 * et le coordinateur (client RMI).
 */
public interface IAgentPiece extends Remote {

    /**
     * Renvoie la dernière température reçue du capteur de la pièce.
     * @return la température en °C
     * @throws RemoteException si l'agent est injoignable
     */
    double getTemperature() throws RemoteException;

    /**
     * Renvoie la dernière luminosité reçue du capteur de la pièce.
     * @return la luminosité en lux
     * @throws RemoteException si l'agent est injoignable
     */
    int getLuminosite() throws RemoteException;

    /**
     * Indique si la pièce est occupée, d'après le dernier message du capteur d'occupation.
     * @return {@code true} si la pièce est occupée
     * @throws RemoteException si l'agent est injoignable
     */
    boolean isOccupee() throws RemoteException;

    /**
     * Publie la nouvelle consigne du thermostat sur le topic MQTT
     * @param consigne nouvelle consigne en °C
     * @throws RemoteException si l'agent est injoignable ou si la publication MQTT échoue
     */
    void setConsigneTemperature(double consigne) throws RemoteException;

    /**
     * Renvoie l'état de la pièce au format JSON
     * (pièce, température, luminosité, occupation, consigne).
     * @return l'état de la pièce sous forme de chaîne JSON
     * @throws RemoteException si l'agent est injoignable
     */
    String getEtatJson() throws RemoteException;

    /**
     * Indique si les capteurs de la pièce sont en timeout, c'est-à-dire si aucune
     * mesure MQTT n'a été reçue depuis plus de 30 secondes.
     * @return {@code true} si la pièce est en timeout ; les valeurs renvoyées par les getters sont alors les dernières valeurs connues
     * @throws RemoteException si l'agent est injoignable
     */
    boolean isCapteurEnTimeout() throws RemoteException;
}
