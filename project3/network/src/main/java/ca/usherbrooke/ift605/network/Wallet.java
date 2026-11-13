package ca.usherbrooke.ift605.network;

import ca.usherbrooke.ift605.crypto.CryptoUtils;
import ca.usherbrooke.ift605.transactions.OutPoint;
import ca.usherbrooke.ift605.transactions.Transaction;
import ca.usherbrooke.ift605.transactions.TransactionInput;
import ca.usherbrooke.ift605.transactions.TransactionOutput;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Wallet {

    private final KeyPair keyPair;
    private final Node node;

    public Wallet(String name, Node node) {
        this.keyPair = CryptoUtils.generateKeyPair();
        this.node = node;
        AddressBook.register(getAddress(), name);
    }

    public String getAddress() {
        return CryptoUtils.publicKeyToAddress(keyPair.getPublic());
    }

    public long getBalance() {
        long balance = 0;
        for (TransactionOutput out : node.findUtxos(getAddress()).values()) {
            balance += out.amount();
        }
        return balance;
    }

    public Transaction createTransaction(String toAddress, long amount) {
        List<TransactionInput> inputs = new ArrayList<>();
        long collected = 0;
        for (Map.Entry<OutPoint, TransactionOutput> utxo : node.findUtxos(getAddress()).entrySet()) {
            if (collected >= amount) break;
            inputs.add(new TransactionInput(utxo.getKey().txId(), utxo.getKey().index()));
            collected += utxo.getValue().amount();
        }
        if (collected < amount) {
            throw new IllegalStateException("Solde insuffisant");
        }
        List<TransactionOutput> outputs = new ArrayList<>();
        outputs.add(new TransactionOutput(amount, toAddress));
        if (collected > amount) {
            outputs.add(new TransactionOutput(collected - amount, getAddress())); // monnaie rendue
        }
        Transaction tx = new Transaction(inputs, outputs);
        tx.sign(keyPair.getPrivate(), keyPair.getPublic());
        return tx;
    }

    public boolean broadcast(Transaction tx) {
        return node.submitTransaction(tx);
    }

    public void sign(Transaction tx) {
        tx.sign(keyPair.getPrivate(), keyPair.getPublic());
    }
}