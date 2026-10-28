package com.batiment.coordinateur;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.batiment.api.IAgentPiece;

/**
 * Client RMI de validation de la tâche 2.2 : lit les trois états d'une pièce,
 * puis modifie la consigne de son thermostat.
 */
public class ClientTestRMI {

    /**
     * Lit la température, la luminosité et l'occupation d'un agent via RMI, puis
     * lui envoie une nouvelle consigne et affiche son état complet.
     * @param args {@code <piece> <consigne>} (par défaut : salleA et 19.0)
     * @throws Exception si l'agent est introuvable ou injoignable
     */
    public static void main(String[] args) throws Exception {
        String piece = args.length > 0 ? args[0] : "salleA";
        double consigne = args.length > 1 ? Double.parseDouble(args[1]) : 19.0;

        Registry registry = LocateRegistry.getRegistry(2199);

        // lookup renvoie un "stub" : chaque appel de méthode dessus est exécuté dans la JVM de l'agent
        IAgentPiece agent = (IAgentPiece) registry.lookup("agent-" + piece);

        // Lecture des trois états à distance
        System.out.println("Température : " + agent.getTemperature() + " °C");
        System.out.println("Luminosité  : " + agent.getLuminosite() + " lux");
        System.out.println("Occupée     : " + agent.isOccupee());

        // Modification de la consigne : l'agent la publie ensuite sur MQTT
        agent.setConsigneTemperature(consigne);
        System.out.println("Consigne " + consigne + " °C envoyée à agent-" + piece);

        // État complet après modification
        System.out.println("État : " + agent.getEtatJson());
    }
}