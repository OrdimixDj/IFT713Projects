package com.batiment.agent;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Point d'entrée d'un agent de pièce : une JVM par pièce.
 * La première JVM lancée crée le registre RMI sur le port 2199, les suivantes réutilisent le même.
 */
public class Main {
    /** Port du registre RMI imposé par le sujet. */
    private static final int PORT_RMI = 2199;

    /** Référence statique : empêche le ramasse-miettes de supprimer l'objet RMI exporté. */
    private static AgentPiece agent;

    /**
     * Crée l'agent de la pièce donnée et l'enregistre dans le registre RMI.
     * @param args un seul argument : le nom de la pièce (salleA, salleB ou cuisine)
     * @throws Exception si l'agent ne peut pas être créé ou enregistré
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage : Main <salleA|salleB|cuisine>");
            System.exit(1);
        }
        String nom = args[0];

        Registry registry = obtenirRegistre();

        agent = new AgentPiece(nom);
        registry.rebind("agent-" + nom, agent);
    }

    /**
     * Crée le registre RMI s'il n'existe pas encore ; sinon, récupère celui qu'une autre JVM d'agent a déjà créé sur le même port.
     * @return le registre RMI du port {@value #PORT_RMI}
     * @throws RemoteException si le registre ne peut être ni créé ni récupéré
     */
    private static Registry obtenirRegistre() throws RemoteException {
        try {
            return LocateRegistry.createRegistry(PORT_RMI);
        } catch (RemoteException e) {
            return LocateRegistry.getRegistry(PORT_RMI);
        }
    }
}