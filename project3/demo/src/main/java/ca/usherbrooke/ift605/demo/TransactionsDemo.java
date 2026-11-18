package ca.usherbrooke.ift605.demo;

import ca.usherbrooke.ift605.crypto.CryptoUtils;
import ca.usherbrooke.ift605.transactions.Transaction;
import ca.usherbrooke.ift605.transactions.TransactionInput;
import ca.usherbrooke.ift605.transactions.TransactionOutput;
import ca.usherbrooke.ift605.transactions.UTXOSet;

import java.security.KeyPair;
import java.util.List;

/**
 * Démonstration du module 3, via le modèle UTXO, les frais de transaction et la
 * validation des signatures par le script P2PKH.
 */
public final class TransactionsDemo {

    private static final long BTC = Transaction.SATOSHIS_PER_BTC;

    private TransactionsDemo() {
    }

    /**
     * Affiche les deux démonstrations dans la console.
     *
     * @param args non utilisés
     */
    public static void main(String[] args) {
        KeyPair alice = CryptoUtils.generateKeyPair();
        KeyPair bob = CryptoUtils.generateKeyPair();
        String aliceAddr = CryptoUtils.publicKeyToAddress(alice.getPublic());
        String bobAddr = CryptoUtils.publicKeyToAddress(bob.getPublic());

        UTXOSet utxoSet = new UTXOSet();
        Transaction coinbase = Transaction.coinbase(aliceAddr, 50 * BTC, 0);
        utxoSet.addTransaction(coinbase);

        Transaction payment = utxoAndFees(utxoSet, coinbase, alice, aliceAddr, bobAddr);
        p2pkh(utxoSet, coinbase, payment, alice, bob, aliceAddr, bobAddr);
    }

    // Alice paie 10 BTC à Bob avec une UTXO de 50 BTC : la monnaie lui revient et
    // la différence restante correspond aux frais
    private static Transaction utxoAndFees(UTXOSet utxoSet, Transaction coinbase, KeyPair alice,
            String aliceAddr, String bobAddr) {
        System.out.println("=== Modèle UTXO et frais ===");
        System.out.println("Coinbase : Alice reçoit 50 BTC");
        System.out.println("Solde Alice : " + btc(balance(utxoSet, aliceAddr)) + " | Solde Bob : "
                + btc(balance(utxoSet, bobAddr)));

        Transaction payment = new Transaction(
                List.of(new TransactionInput(coinbase.getId(), 0)),
                List.of(new TransactionOutput(10 * BTC, bobAddr),
                        new TransactionOutput(3_990_000_000L, aliceAddr)));
        payment.sign(alice.getPrivate(), alice.getPublic());
        System.out.println("Alice → Bob : 10 BTC, monnaie rendue à Alice : 39.9 BTC");
        System.out.println("  Transaction valide : " + utxoSet.isValid(payment));
        System.out.println("  Frais : " + btc(payment.getFee(utxoSet)) + " (50 − 10 − 39.9)");

        utxoSet.addTransaction(payment);
        System.out.println("Solde Alice : " + btc(balance(utxoSet, aliceAddr)) + " | Solde Bob : "
                + btc(balance(utxoSet, bobAddr)));
        return payment;
    }

    // 4 transactions invalides, chacune refusée pour une raison différente
    private static void p2pkh(UTXOSet utxoSet, Transaction coinbase, Transaction payment, KeyPair alice,
            KeyPair bob, String aliceAddr, String bobAddr) {
        System.out.println();
        System.out.println("=== Script P2PKH : transactions refusées ===");

        // Bob signe avec sa propre clé une UTXO qui appartient à Alice : OP_EQUALVERIFY
        // échoue
        Transaction theft = new Transaction(
                List.of(new TransactionInput(payment.getId(), 1)),
                List.of(new TransactionOutput(39 * BTC, bobAddr)));
        theft.sign(bob.getPrivate(), bob.getPublic());
        System.out.println("Bob dépense l'UTXO d'Alice avec sa clé       → valide : " + utxoSet.isValid(theft));

        // Bob signe un envoi de 5 BTC depuis son UTXO, puis le montant est modifié :
        // l'id change, donc OP_CHECKSIG échoue
        Transaction signed = new Transaction(
                List.of(new TransactionInput(payment.getId(), 0)),
                List.of(new TransactionOutput(5 * BTC, aliceAddr)));
        signed.sign(bob.getPrivate(), bob.getPublic());
        Transaction modified = new Transaction(
                List.of(new TransactionInput(payment.getId(), 0)),
                List.of(new TransactionOutput(9 * BTC, aliceAddr)));
        TransactionInput signedInput = signed.getInputs().get(0);
        modified.getInputs().get(0).setScriptSig(signedInput.getSignature(), signedInput.getPublicKey());
        System.out.println("Bob signe 5 BTC, montant modifié en 9 BTC    → valide : " + utxoSet.isValid(modified));

        // Alice réutilise l'UTXO de la coinbase, qui est déjà dépensée
        Transaction doubleSpend = new Transaction(
                List.of(new TransactionInput(coinbase.getId(), 0)),
                List.of(new TransactionOutput(50 * BTC, aliceAddr)));
        doubleSpend.sign(alice.getPrivate(), alice.getPublic());
        System.out.println("Alice redépense la coinbase (double dépense) → valide : " + utxoSet.isValid(doubleSpend));

        // Alice envoie plus que ce que contient son UTXO
        Transaction overspend = new Transaction(
                List.of(new TransactionInput(payment.getId(), 1)),
                List.of(new TransactionOutput(100 * BTC, bobAddr)));
        overspend.sign(alice.getPrivate(), alice.getPublic());
        System.out.println("Alice envoie 100 BTC avec 39.9 BTC → valide : " + utxoSet.isValid(overspend));
    }

    // Somme des UTXO verrouillées sur une adresse
    private static long balance(UTXOSet utxoSet, String address) {
        long total = 0;
        for (TransactionOutput out : utxoSet.findByAddress(address).values()) {
            total += out.amount();
        }
        return total;
    }

    // Conversion des satoshis en BTC pour l'affichage
    private static String btc(long satoshis) {
        return (satoshis / (double) BTC) + " BTC";
    }
}