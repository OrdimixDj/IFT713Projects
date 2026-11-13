package ca.usherbrooke.ift605.consensus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DifficultyAdjusterTest {

    private static final long TARGET = 100_000; // 10 blocs × 10 s

    // 20s au lieu de 100s, soit moins de la moitié de la cible : la difficulté
    // augmente de 1
    @Test
    void tropRapide_difficulteAugmente() {
        assertEquals(5, DifficultyAdjuster.adjust(4, 20_000, TARGET));
    }

    // 300s, soit plus du double de la cible : la difficulté baisse de 1, sans
    // jamais descendre sous 1, qui est le cas limite
    @Test
    void tropLent_difficulteDiminue_minimum1() {
        assertEquals(3, DifficultyAdjuster.adjust(4, 300_000, TARGET));
        assertEquals(1, DifficultyAdjuster.adjust(1, 300_000, TARGET));
    }

    // Temps égal à la cible : aucun changement
    @Test
    void dansLaCible_difficulteInchangee() {
        assertEquals(4, DifficultyAdjuster.adjust(4, 100_000, TARGET));
    }
}