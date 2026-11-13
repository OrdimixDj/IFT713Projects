package ca.usherbrooke.ift605.demo;

import ca.usherbrooke.ift605.network.Node;
import ca.usherbrooke.ift605.network.Wallet;
import ca.usherbrooke.ift605.transactions.Transaction;
import ca.usherbrooke.ift605.transactions.TransactionInput;
import ca.usherbrooke.ift605.transactions.TransactionOutput;

import java.util.ArrayList;
import java.util.List;

import static ca.usherbrooke.ift605.transactions.Transaction.SATOSHIS_PER_BTC;

public final class CryptoLedgerDemo {

    private CryptoLedgerDemo() {}

    public static void main(String[] args) throws Exception {
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

        node1.mineBlock(alice.getAddress());
        pause();

        Transaction aliceToBob = alice.createTransaction(bob.getAddress(), 10 * SATOSHIS_PER_BTC);
        alice.broadcast(aliceToBob);
        pause();
        node2.mineBlock(miner2.getAddress());
        pause();

        bob.broadcast(bob.createTransaction(charlie.getAddress(), 5 * SATOSHIS_PER_BTC));
        pause();

        Node node4 = new Node(6004);
        node4.start();
        node4.connect(6001);
        node4.synchronize(6001);
        pause();

        List<TransactionInput> sameInputs = new ArrayList<>();
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

    private static void pause() throws InterruptedException {
        Thread.sleep(500);
    }
}