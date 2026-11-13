package ca.usherbrooke.ift605.consensus;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DoubleSpendSimulationTest {

    // Scénario de l'énoncé, partie 4.3 : attaquant à 30 %, la chaîne honnête
    // l'emporte (nous utilisons une graine 42 fixe pour que le résultat soit
    // reproductible)
    @Test
    void attaquant30pourcent_chaineHonneteGagne() {
        assertTrue(DoubleSpendSimulation.run(0.30, new Random(42)));
    }

    // Moins de 50 % : l'attaque échoue le plus souvent ; plus de 50 % : elle
    // réussit le plus souvent
    @Test
    void seuilDes50pourcent() {
        assertTrue(DoubleSpendSimulation.attackSuccessRate(0.30, 10_000, new Random(1)) < 0.5);
        assertTrue(DoubleSpendSimulation.attackSuccessRate(0.70, 10_000, new Random(1)) > 0.5);
    }
}