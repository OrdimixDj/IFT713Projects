package com.batiment.coordinateur;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.batiment.api.IAgentPiece;

/**
 * Coordinateur central du bâtiment.
 * Il se connecte via RMI aux trois agents de pièce et applique une politique
 * d'équilibrage thermique : si l'écart de température entre deux pièces adjacentes
 * dépasse {@value #ECART_MAX} °C, il demande à la pièce la plus chaude de baisser
 * sa consigne.
 */
public class CoordinateurBatiment {

    /** Port du registre RMI imposé par le sujet. */
    private static final int PORT_RMI = 2199;

    /** Écart maximal toléré entre deux pièces adjacentes, en °C. */
    private static final double ECART_MAX = 4.0;

    /** Les trois pièces du bâtiment. */
    private static final String[] PIECES = {"salleA", "salleB", "cuisine"};

    /** Paires de pièces adjacentes surveillées (minimum imposé par le sujet). */
    private static final String[][] PAIRES_ADJACENTES = {
            {"salleA", "salleB"},
            {"salleB", "cuisine"}
    };

    /** Stubs RMI des agents, indexés par nom de pièce. */
    private final Map<String, IAgentPiece> agents = new HashMap<>();

    /**
     * Récupère les trois agents dans le registre RMI.
     * @throws RemoteException si le registre est injoignable
     * @throws NotBoundException si un agent n'est pas enregistré (pas encore lancé)
     */
    public CoordinateurBatiment() throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(PORT_RMI);
        for (String piece : PIECES) {
            agents.put(piece, (IAgentPiece) registry.lookup("agent-" + piece));
        }
    }

    /**
     * Vérifie chaque paire de pièces adjacentes. Si l'écart absolu dépasse
     * {@value #ECART_MAX} °C, envoie à la pièce la plus chaude une consigne égale
     * à la moyenne des deux températures, arrondie à 0,1 °C, pour la rapprocher de
     * sa voisine.
     * @throws RemoteException si un agent ne répond pas
     */
    public void equilibrerTemperatures() throws RemoteException {
        for (String[] paire : PAIRES_ADJACENTES) {
            String pieceA = paire[0];
            String pieceB = paire[1];

            // Lecture des deux températures via RMI
            double tA = agents.get(pieceA).getTemperature();
            double tB = agents.get(pieceB).getTemperature();
            double ecart = Math.abs(tA - tB);

            String entete = String.format(Locale.ROOT, "[Coordinateur] %s–%s : %.1f °C / %.1f °C (écart %.1f °C)",
                    pieceA, pieceB, tA, tB, ecart);

            if (ecart > ECART_MAX) {
                // On agit sur la pièce la plus chaude
                String pieceChaude = tA > tB ? pieceA : pieceB;

                // Moyenne arrondie à 0,1 °C
                double consigne = Math.round((tA + tB) / 2.0 * 10.0) / 10.0;

                agents.get(pieceChaude).setConsigneTemperature(consigne);
                System.out.println(entete + " → DÉSÉQUILIBRE : consigne " + consigne
                        + " °C envoyée à " + pieceChaude);
            } else {
                System.out.println(entete + " → équilibré, aucune action");
            }
        }
    }
}