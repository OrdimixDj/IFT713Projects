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

/**
 * Portefeuille simple, rattaché à un nœud local. Il contient une seule paire de
 * clés ECDSA, générée aléatoirement. Le solde est calculé à partir de
 * l'ensemble UTXO du nœud.
 */
public class Wallet {

    private final KeyPair keyPair;
    private final Node node;

    /**
     * Crée un portefeuille avec une nouvelle paire de clés et l'enregistre dans
     * l'{@link AddressBook}.
     *
     * @param name le nom du propriétaire
     * @param node le nœud local utilisé pour lire les UTXO et diffuser les
     *             transactions
     */
    public Wallet(String name, Node node) {
        this.keyPair = CryptoUtils.generateKeyPair();
        this.node = node;
        AddressBook.register(getAddress(), name);
    }

    /**
     * @return l'adresse du portefeuille : {@code Base58(SHA-256(pk))}
     */
    public String getAddress() {
        return CryptoUtils.publicKeyToAddress(keyPair.getPublic());
    }

    /**
     * Calcule le solde en interrogeant l'ensemble UTXO local.
     *
     * @return la somme des UTXO de l'adresse, en satoshis
     */
    public long getBalance() {
        long balance = 0;
        for (TransactionOutput out : node.findUtxos(getAddress()).values()) {
            balance += out.amount();
        }
        return balance;
    }

    /**
     * Construit et signe une transaction à partir des UTXO disponibles. La sortie 0
     * paie le destinataire ; s'il reste un surplus, la sortie 1 renvoie la monnaie
     * au portefeuille.
     *
     * @param toAddress l'adresse du destinataire
     * @param amount    le montant, en satoshis
     * @return la transaction signée, pas encore diffusée
     * @throws IllegalStateException si le solde est insuffisant
     */
    public Transaction createTransaction(String toAddress, long amount) {
        List<TransactionInput> inputs = new ArrayList<>();
        long collected = 0;
        // On prend des UTXO jusqu'à couvrir le montant
        for (Map.Entry<OutPoint, TransactionOutput> utxo : node.findUtxos(getAddress()).entrySet()) {
            if (collected >= amount)
                break;
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
        // Pas de frais : somme(entrées) = somme(sorties)
        Transaction tx = new Transaction(inputs, outputs);
        tx.sign(keyPair.getPrivate(), keyPair.getPublic());
        return tx;
    }

    /**
     * Diffuse la transaction au réseau P2P via le nœud local.
     *
     * @param tx la transaction signée
     * @return {@code true} si le nœud local l'a acceptée et diffusée
     */
    public boolean broadcast(Transaction tx) {
        return node.submitTransaction(tx);
    }

    /**
     * Signe une transaction construite à la main.
     *
     * @param tx la transaction à signer
     */
    public void sign(Transaction tx) {
        tx.sign(keyPair.getPrivate(), keyPair.getPublic());
    }
}