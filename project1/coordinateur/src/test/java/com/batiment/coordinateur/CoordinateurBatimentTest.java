package com.batiment.coordinateur;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.rmi.RemoteException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.batiment.api.IAgentPiece;

/**
 * Tests de la politique d'équilibrage du coordinateur, avec de faux agents, sans utiliser ni RMI ni MQTT.
 */
class CoordinateurBatimentTest {

    /** Faux agent : renvoie une température fixée et mémorise la consigne reçue. */
    static class FauxAgent implements IAgentPiece {
        double temperature;
        boolean enPanne;
        boolean enTimeout;
        Double consigneRecue;

        FauxAgent(double temperature) {
            this.temperature = temperature;
        }

        @Override
        public double getTemperature() throws RemoteException {
            if (enPanne) {
                throw new RemoteException("agent arrêté");
            }
            return temperature;
        }

        @Override
        public int getLuminosite() {
            return 0;
        }

        @Override
        public boolean isOccupee() {
            return false;
        }

        @Override
        public void setConsigneTemperature(double consigne) {
            consigneRecue = consigne;
        }

        @Override
        public String getEtatJson() {
            return "{}";
        }

        @Override
        public boolean isCapteurEnTimeout() {
            return enTimeout;
        }
    }

    @Test
    void ecartInferieurA4DegresAucuneConsigne() {
        FauxAgent salleA = new FauxAgent(22.0);
        FauxAgent salleB = new FauxAgent(20.0);
        FauxAgent cuisine = new FauxAgent(21.0);

        new CoordinateurBatiment(Map.of("salleA", salleA, "salleB", salleB, "cuisine", cuisine))
                .equilibrerTemperatures();

        assertNull(salleA.consigneRecue);
        assertNull(salleB.consigneRecue);
        assertNull(cuisine.consigneRecue);
    }

    @Test
    void ecartSuperieurA4DegresConsigneEnvoyeeALaPieceLaPlusChaude() {
        FauxAgent salleA = new FauxAgent(28.0);
        FauxAgent salleB = new FauxAgent(21.0);
        FauxAgent cuisine = new FauxAgent(21.0);

        new CoordinateurBatiment(Map.of("salleA", salleA, "salleB", salleB, "cuisine", cuisine))
                .equilibrerTemperatures();

        assertEquals(24.5, salleA.consigneRecue); // 24.5 est la moyenne de 28 et 21
        assertNull(salleB.consigneRecue);
        assertNull(cuisine.consigneRecue);
    }

    @Test
    void agentEnPanneLeCycleContinueAvecLesAutres() {
        FauxAgent salleA = new FauxAgent(28.0);
        FauxAgent salleB = new FauxAgent(21.0);
        FauxAgent cuisine = new FauxAgent(21.0);
        cuisine.enPanne = true;

        CoordinateurBatiment coordinateur =
                new CoordinateurBatiment(Map.of("salleA", salleA, "salleB", salleB, "cuisine", cuisine));

        // La RemoteException de la cuisine ne doit pas remonter...
        assertDoesNotThrow(coordinateur::equilibrerTemperatures);

        // La paire salleA–salleB doit quand même être traitée
        assertEquals(24.5, salleA.consigneRecue);
    }

    @Test
    void capteurEnTimeoutLaDerniereValeurConnueEstUtilisee() {
        FauxAgent salleA = new FauxAgent(28.0);
        FauxAgent salleB = new FauxAgent(21.0);
        FauxAgent cuisine = new FauxAgent(21.0);
        salleA.enTimeout = true;

        new CoordinateurBatiment(Map.of("salleA", salleA, "salleB", salleB, "cuisine", cuisine))
                .equilibrerTemperatures();

        // 28 °C est la dernière valeur reçue avant la panne, alors la moyenne de 28 et 21 est de 24.5
        assertEquals(24.5, salleA.consigneRecue);
    }
}