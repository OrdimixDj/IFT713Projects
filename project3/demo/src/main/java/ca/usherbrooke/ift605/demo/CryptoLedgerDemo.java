package ca.usherbrooke.ift605.demo;

import ca.usherbrooke.ift605.network.Node;
import ca.usherbrooke.ift605.network.Wallet;
import ca.usherbrooke.ift605.transactions.Transaction;
import ca.usherbrooke.ift605.transactions.TransactionInput;
import ca.usherbrooke.ift605.transactions.TransactionOutput;

import java.util.ArrayList;
import java.util.List;

import static ca.usherbrooke.ift605.transactions.Transaction.SATOSHIS_PER_BTC;

/**
 * Scénario de démonstration intégré : 3 nœuds locaux sur les ports 6001, 6002
 * et 6003.
 * <ol>
 * <li>Alice (nœud 6001) mine le bloc genesis et reçoit la récompense de 50 BTC
 * ;</li>
 * <li>Alice envoie 10 BTC à Bob (nœud 6002) : transaction diffusée par gossip,
 * puis minée ;</li>
 * <li>Bob envoie 5 BTC à Charlie (nœud 6003) : enchaînement de transactions
 * ;</li>
 * <li>un 4e nœud (6004) se connecte après coup et synchronise la chaîne ;</li>
 * <li>une tentative de double dépense est détectée et rejetée.</li>
 * </ol>
 */
public final class CryptoLedgerDemo {

    private CryptoLedgerDemo() {
    }

    /**
     * Lance le scénario complet, puis arrête les nœuds.
     *
     * @param args non utilisés
     * @throws Exception si un nœud ne peut pas démarrer (par exemples si le port
     *                   est déjà utilisé) ou si l'attente est interrompue
     */
    public static void main(String[] args) throws Exception {
        // Lancement des 3 nœuds ; 6001 sert de nœud d'amorçage (seed node)
        Node node1 = new Node(6001);
        Node node2 = new Node(6002);
        Node node3 = new Node(6003);
        node1.start();
        node2.start();
        node3.start();
        node2.connect(6001);
        node3.connect(6001);

        Wallet alice = new Wallet("Alice", node1);
        Wallet bob = new Wallet("Bob", node2);
        Wallet charlie = new Wallet("Charlie", node3);
        Wallet miner2 = new Wallet("Mineur-6002", node2);

        // Alice (nœud 6001) mine le bloc genesis et reçoit la récompense de 50 BTC
        node1.mineBlock(alice.getAddress());
        pause();

        // Alice envoie 10 BTC à Bob (nœud 6002) = diffusé par gossip, puis miné
        Transaction aliceToBob = alice.createTransaction(bob.getAddress(), 10 * SATOSHIS_PER_BTC);
        alice.broadcast(aliceToBob);
        pause();
        node2.mineBlock(miner2.getAddress());
        pause();

        // Bob envoie 5 BTC à Charlie (nœud 6003) = enchaînement de transactions
        bob.broadcast(bob.createTransaction(charlie.getAddress(), 5 * SATOSHIS_PER_BTC));
        pause();

        // Un 4e nœud se connecte après coup et synchronise la chaîne depuis ses pairs
        Node node4 = new Node(6004);
        node4.start();
        node4.connect(6001);
        node4.synchronize(6001);
        pause();

        // Tentative de double dépense : Alice redépense la sortie déjà payée à Bob
        List<TransactionInput> sameInputs = new ArrayList<>();
        // On recopie les entrées de Alice → Bob en gardant les mêmes UTXO, sans les
        // signatures, ce qui en fait une double dépense. Ces UTXO ont déjà été
        // dépensées par le bloc #1, le nœud 6001 doit donc rejeter la transaction
        for (TransactionInput in : aliceToBob.getInputs()) {
            sameInputs.add(new TransactionInput(in.getTxId(), in.getOutputIndex()));
        }
        Transaction doubleSpend = new Transaction(sameInputs, List.of(
                new TransactionOutput(10 * SATOSHIS_PER_BTC, charlie.getAddress()),
                new TransactionOutput(40 * SATOSHIS_PER_BTC, alice.getAddress())));
        alice.sign(doubleSpend);
        alice.broadcast(doubleSpend);
        pause();

        node1.stop();
        node2.stop();
        node3.stop();
        node4.stop();
    }

    // Laisse le temps à la diffusion gossip asynchrone de se propager
    private static void pause() throws InterruptedException {
        Thread.sleep(500);
    }
}