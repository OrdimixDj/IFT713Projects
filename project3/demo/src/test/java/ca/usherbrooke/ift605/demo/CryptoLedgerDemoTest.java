package ca.usherbrooke.ift605.demo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CryptoLedgerDemoTest {

    private static String output;

    @BeforeAll
    static void runDemo() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            CryptoLedgerDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        output = buffer.toString(StandardCharsets.UTF_8);
        System.out.println(output);
    }

    @Test
    void genesisMineParAlice() {
        assertTrue(output.contains("[Node-6001] Bloc #0 (genesis) miné — récompense: 50 BTC → Alice"));
    }

    @Test
    void transactionDiffuseeParGossipPuisConfirmee() {
        assertTrue(output.contains("[Node-6001] Tx diffusée : Alice → Bob (10 BTC) [gossip → 6002, 6003]"));
        assertTrue(output.contains("[Node-6002] Tx reçue et validée  : Alice → Bob (10 BTC)"));
        assertTrue(output.contains("[Node-6003] Tx reçue et validée  : Alice → Bob (10 BTC)"));
        assertTrue(output.contains("[Node-6002] Bloc #1 miné — 1 transaction(s) confirmée(s)"));
    }

    @Test
    void synchronisationDuQuatriemeNoeud() {
        assertTrue(output.contains("[Node-6004] Synchronisation : 2 blocs téléchargés depuis Node-6001"));
    }

    @Test
    void doubleDepenseRejetee() {
        assertTrue(output.contains("[Node-6001] Double dépense détectée — transaction rejetée"));
    }
}