package com.batiment.agent;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    private static final int PORT_RMI = 2199;
    private static AgentPiece agent;

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

    private static Registry obtenirRegistre() throws RemoteException {
        try {
            return LocateRegistry.createRegistry(PORT_RMI);
        } catch (RemoteException e) {
            return LocateRegistry.getRegistry(PORT_RMI);
        }
    }
}