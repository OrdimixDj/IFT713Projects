package ca.usherbrooke.ift605.network;

import ca.usherbrooke.ift605.transactions.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static ca.usherbrooke.ift605.transactions.Transaction.SATOSHIS_PER_BTC;
import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    private Node node;
    private Wallet alice;
    private Wallet bob;

    // Avant chaque test : un nœud seul, et Alice possède 50 BTC
    @BeforeEach
    void setUp() throws IOException {
        node = new Node(7501);
        node.start();
        alice = new Wallet("Alice", node);
        bob = new Wallet("Bob", node);
        node.mineBlock(alice.getAddress()); // 50 BTC pour Alice
    }

    // Après chaque test : libère le port
    @AfterEach
    void tearDown() throws IOException {
        node.stop();
    }

    // Le solde est calculé à partir de l'ensemble UTXO du nœud local
    @Test
    void soldeDepuisUtxoSetLocal() {
        assertEquals(50 * SATOSHIS_PER_BTC, alice.getBalance());
        assertEquals(0, bob.getBalance());
    }

    // Paiement de 10 BTC : 10 pour Bob, 40 rendus à Alice ; les soldes sont à jour
    // après confirmation (Alice reçoit aussi la récompense de 50 BTC du 2e bloc :
    // 40 + 50)
    @Test
    void transactionAvecMonnaieRendue() {
        Transaction tx = alice.createTransaction(bob.getAddress(), 10 * SATOSHIS_PER_BTC);
        assertEquals(2, tx.getOutputs().size()); // paiement + monnaie rendue
        assertTrue(alice.broadcast(tx));
        node.mineBlock(alice.getAddress());
        assertEquals(10 * SATOSHIS_PER_BTC, bob.getBalance());
        assertEquals((40 + 50) * SATOSHIS_PER_BTC, alice.getBalance());
    }

    // Cas limite : Bob n'a rien, il ne peut pas payer 1 BTC
    @Test
    void soldeInsuffisant() {
        assertThrows(IllegalStateException.class,
                () -> bob.createTransaction(alice.getAddress(), SATOSHIS_PER_BTC));
    }
}