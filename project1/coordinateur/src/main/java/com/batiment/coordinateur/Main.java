package com.batiment.coordinateur;

/**
 * Point d'entrée du coordinateur : applique l'équilibrage thermique en boucle.
 */
public class Main {
    /** Période entre deux cycles d'équilibrage, en millisecondes. */
    private static final long PERIODE_MS = 10_000;

    /**
     * Se connecte aux trois agents puis lance un cycle d'équilibrage toutes les
     * {@value #PERIODE_MS} ms.
     * @param args non utilisés
     * @throws Exception si la connexion aux agents échoue ou si un agent ne répond plus
     */
    public static void main(String[] args) throws Exception {
        CoordinateurBatiment coordinateur = new CoordinateurBatiment();

        while (true) {
            coordinateur.equilibrerTemperatures();
            Thread.sleep(PERIODE_MS);
        }
    }
}