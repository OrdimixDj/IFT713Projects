package com.batiment.coordinateur;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.batiment.api.IAgentPiece;

/**
 * Coordinateur central du bâtiment :
 * Il se connecte via RMI aux trois agents de pièce et applique une politique
 * d'équilibrage thermique : si l'écart de température entre deux pièces adjacentes
 * dépasse {@value #ECART_MAX} °C, il demande à la pièce la plus chaude de baisser
 * sa consigne, et le signale également à openHAB via son API REST, ce qui déclenche la règle d’alerte.
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

    /** Item openHAB qui reçoit les alertes (via l'API REST). */
    private static final String URL_ALERTE_OPENHAB = "http://localhost:8080/rest/items/Coordinateur_Alerte";

    /** Client HTTP utilisé pour appeler l'API REST d'openHAB. */
    private final HttpClient http = HttpClient.newHttpClient();

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
     * Si un agent ne répond pas ({@link RemoteException}), la
     * paire concernée est ignorée et le cycle continue avec les autres. Si le capteur
     * d'une pièce est en timeout, la dernière valeur connue (renvoyée par l'agent) est
     * utilisée et le timeout est journalisé.
     */
    public void equilibrerTemperatures() {
        for (String[] paire : PAIRES_ADJACENTES) {
            String pieceA = paire[0];
            String pieceB = paire[1];

            try {
                // Lecture des deux températures via RMI
                double tA = agents.get(pieceA).getTemperature();
                double tB = agents.get(pieceB).getTemperature();

                // Timeout capteur : l'agent renvoie sa dernière valeur connue, on le signale
                signalerTimeout(pieceA, tA);
                signalerTimeout(pieceB, tB);

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
                    
                    // L'alerte déclenche la règle DSL "Alerte desequilibre thermique"
                    notifierOpenHab(String.format(Locale.ROOT, "%s-%s : écart %.1f °C, consigne %.1f °C envoyée à %s",
                                    pieceA, pieceB, ecart, consigne, pieceChaude));
                } else {
                    System.out.println(entete + " → équilibré, aucune action");
                }
            } catch (RemoteException e) {
                System.err.println("[Coordinateur] " + pieceA + "–" + pieceB + " : agent injoignable, paire ignorée (" + e.getClass().getSimpleName() + ")");
                reconnecter(pieceA);
                reconnecter(pieceB);
            }
        }
    }

    /**
     * Journalise un timeout capteur si l'agent de la pièce n'a reçu aucune mesure depuis plus de 30 secondes.
     * @param piece nom de la pièce
     * @param temperature dernière valeur connue de la température renvoyée par l'agent
     * @throws RemoteException si l'agent ne répond pas
     */
    private void signalerTimeout(String piece, double temperature) throws RemoteException {
        if (agents.get(piece).isCapteurEnTimeout()) {
            System.out.println("[Coordinateur] " + piece + " : capteur en timeout, dernière valeur connue utilisée : " + temperature + " °C");
        }
    }

    /**
     * Envoie un message à l'item openHAB Coordinateur_Alerte via l'API REST, qui déclenche la règle DSL d'alerte dans openHAB.
     * @param message le texte de l'alerte
     */
    private void notifierOpenHab(String message) {
        HttpRequest requete = HttpRequest.newBuilder(URI.create(URL_ALERTE_OPENHAB))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(message))
                .build();
        try {
            HttpResponse<Void> reponse = http.send(requete, HttpResponse.BodyHandlers.discarding());
            if (reponse.statusCode() >= 300) {
                System.err.println("[Coordinateur] openHAB a refusé l'alerte (HTTP " + reponse.statusCode() + ")");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[Coordinateur] openHAB injoignable : " + e.getMessage());
        }
    }

    /**
     * Constructeur utilisé par les tests : les agents sont fournis directement, sans passer par le registre RMI.
     * Le constructeur n'est pas public, pour être visible uniquement par les tests mais pas par le reste du projet.
     * @param agents agents indexés par nom de pièce
     */
    CoordinateurBatiment(Map<String, IAgentPiece> agents) {
        this.agents.putAll(agents);
    }

    /**
     * Relit le stub d'un agent dans le registre RMI après une panne.
     * Un agent relancé se réenregistre sous le même nom avec un nouveau stub : le coordinateur peut donc l'interroger de nouveau dès le cycle suivant.
     * Si l'agent est toujours arrêté, on réessaiera au prochain cycle.
     * @param piece nom de la pièce dont l'agent doit être retrouvé
     */
    private void reconnecter(String piece) {
        try {
            Registry registry = LocateRegistry.getRegistry(PORT_RMI);
            agents.put(piece, (IAgentPiece) registry.lookup("agent-" + piece));
        } catch (RemoteException | NotBoundException e) {
            // Registre ou agent toujours indisponible : nouvel essai au prochain cycle
        }
    }
}