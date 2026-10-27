package com.batiment.coordinateur;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.batiment.api.IAgentPiece;

public class ClientTestRMI {

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