package com.batiment.coordinateur;

public class Main {
    private static final long PERIODE_MS = 10_000;

    public static void main(String[] args) throws Exception {
        CoordinateurBatiment coordinateur = new CoordinateurBatiment();

        while (true) {
            coordinateur.equilibrerTemperatures();
            Thread.sleep(PERIODE_MS);
        }
    }
}