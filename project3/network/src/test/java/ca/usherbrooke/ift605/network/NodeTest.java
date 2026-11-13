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

// Chaque test utilise ses propres ports pour ne pas entrer en conflit avec les autres tests ni la démo
class NodeTest {

    private final List<Node> started = new ArrayList<>();

    // Crée et démarre un nœud en le mémorisant pour l'arrêter après le test
    private Node startNode(int port) throws IOException {
        Node node = new Node(port);
        node.start();
        started.add(node);
        return node;
    }

    // Libère les ports après chaque test
    @AfterEach
    void stopNodes() throws IOException {
        for (Node n : started)
            n.stop();
    }

    // Le nœud d'amorçage donne la liste des pairs, et les liens existent dans les
    // deux sens (c'est en fait la procédure de connexion)
    @Test
    void connexionViaNoeudAmorcage() throws Exception {
        Node seed = startNode(7101);
        Node b = startNode(7102);
        Node c = startNode(7103);
        b.connect(7101);
        c.connect(7101);
        assertEquals(List.of(7101, 7102), c.getPeers()); // c a découvert b grâce au nœud d'amorçage
        assertTrue(waitUntil(() -> b.getPeers().contains(7103), 1000)); // b a été prévenu par c
        assertTrue(seed.getPeers().containsAll(List.of(7102, 7103)));
    }

    // Gossip entre 3 nœuds : un bloc et une transaction partis de a atteignent tous
    // les autres nœuds
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

    // Un nœud arrivé en retard télécharge les 2 blocs, et obtient exactement les
    // mêmes que le nœud source
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

    // Double dépense : la 2e transaction réutilise l'UTXO déjà dépensée par la 1re
    // (en attente dans le mempool)
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
        assertFalse(alice.broadcast(toCharlie)); // même UTXO déjà utilisée dans le mempool
    }
}