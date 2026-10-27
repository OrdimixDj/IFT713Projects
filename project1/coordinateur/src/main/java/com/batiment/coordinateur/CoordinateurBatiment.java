package com.batiment.coordinateur;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.batiment.api.IAgentPiece;

public class CoordinateurBatiment {

    private static final int PORT_RMI = 2199;

    private static final double ECART_MAX = 4.0;

    private static final String[] PIECES = {"salleA", "salleB", "cuisine"};

    private static final String[][] PAIRES_ADJACENTES = {
            {"salleA", "salleB"},
            {"salleB", "cuisine"}
    };

    private final Map<String, IAgentPiece> agents = new HashMap<>();

    public CoordinateurBatiment() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(PORT_RMI);
        for (String piece : PIECES) {
            agents.put(piece, (IAgentPiece) registry.lookup("agent-" + piece));
        }
    }

    public void equilibrerTemperatures() throws RemoteException {
        for (String[] paire : PAIRES_ADJACENTES) {
            String pieceA = paire[0];
            String pieceB = paire[1];

            double tA = agents.get(pieceA).getTemperature();
            double tB = agents.get(pieceB).getTemperature();
            double ecart = Math.abs(tA - tB);

            String entete = String.format(Locale.ROOT, "[Coordinateur] %s–%s : %.1f °C / %.1f °C (écart %.1f °C)",
                    pieceA, pieceB, tA, tB, ecart);

            if (ecart > ECART_MAX) {
                String pieceChaude = tA > tB ? pieceA : pieceB;

                double consigne = (tA + tB) / 2.0;

                agents.get(pieceChaude).setConsigneTemperature(consigne);
                System.out.println(entete + " → DÉSÉQUILIBRE : consigne " + consigne
                        + " °C envoyée à " + pieceChaude);
            } else {
                System.out.println(entete + " → équilibré, aucune action");
            }
        }
    }
}