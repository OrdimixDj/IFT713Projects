package ca.usherbrooke.ift605.network;

import ca.usherbrooke.ift605.transactions.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ca.usherbrooke.ift605.network.NetworkTestUtils.waitUntil;
import static ca.usherbrooke.ift605.transactions.Transaction.SATOSHIS_PER_BTC;
import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    private final List<Node> started = new ArrayList<>();

    private Node startNode(int port) throws IOException {
        Node node = new Node(port);
        node.start();
        started.add(node);
        return node;
    }

    @AfterEach
    void stopNodes() throws IOException {
        for (Node n : started) n.stop();
    }

    @Test
    void connexionViaNoeudAmorcage() throws Exception {
        Node seed = startNode(7101);
        Node b = startNode(7102);
        Node c = startNode(7103);
        b.connect(7101);
        c.connect(7101);
        assertEquals(List.of(7101, 7102), c.getPeers());
        assertTrue(waitUntil(() -> b.getPeers().contains(7103), 1000));
        assertTrue(seed.getPeers().containsAll(List.of(7102, 7103)));
    }

    @Test
    void gossipTransactionEtBloc() throws Exception {
        Node a = startNode(7201);
        Node b = startNode(7202);
        Node c = startNode(7203);
        b.connect(7201);
        c.connect(7201);
        Wallet alice = new Wallet("Alice", a);
        Wallet bob = new Wallet("Bob", b);

        a.mineBlock(alice.getAddress());
        assertTrue(waitUntil(() -> c.getChainHeight() == 1, 2000));

        Transaction tx = alice.createTransaction(bob.getAddress(), 10 * SATOSHIS_PER_BTC);
        assertTrue(alice.broadcast(tx));
        assertTrue(waitUntil(() -> b.hasInMempool(tx.getId()) && c.hasInMempool(tx.getId()), 2000));
    }

    @Test
    void synchronisationNouveauNoeud() throws Exception {
        Node a = startNode(7301);
        Wallet alice = new Wallet("Alice", a);
        a.mineBlock(alice.getAddress());
        a.mineBlock(alice.getAddress());

        Node late = startNode(7302);
        late.connect(7301);
        late.synchronize(7301);
        assertEquals(2, late.getChainHeight());
        assertEquals(a.getBlock(1).computeHash(), late.getBlock(1).computeHash());
    }

    @Test
    void doubleDepenseRejetee() throws Exception {
        Node a = startNode(7401);
        Wallet alice = new Wallet("Alice", a);
        Wallet bob = new Wallet("Bob", a);
        Wallet charlie = new Wallet("Charlie", a);
        a.mineBlock(alice.getAddress());

        Transaction toBob = alice.createTransaction(bob.getAddress(), 10 * SATOSHIS_PER_BTC);
        Transaction toCharlie = alice.createTransaction(charlie.getAddress(), 10 * SATOSHIS_PER_BTC);
        assertTrue(alice.broadcast(toBob));
        assertFalse(alice.broadcast(toCharlie));
    }
}