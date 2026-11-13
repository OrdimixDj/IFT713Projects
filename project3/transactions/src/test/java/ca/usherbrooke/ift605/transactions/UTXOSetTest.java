package ca.usherbrooke.ift605.transactions;

import ca.usherbrooke.ift605.crypto.CryptoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.List;

import static ca.usherbrooke.ift605.transactions.Transaction.SATOSHIS_PER_BTC;
import static org.junit.jupiter.api.Assertions.*;

class UTXOSetTest {

    private KeyPair alice;
    private KeyPair bob;
    private String aliceAddress;
    private String bobAddress;
    private UTXOSet utxoSet;
    private Transaction reward; // 50 BTC pour Alice

    // Avant chaque test : Alice possède une seule UTXO de 50 BTC
    @BeforeEach
    void setUp() {
        alice = CryptoUtils.generateKeyPair();
        bob = CryptoUtils.generateKeyPair();
        aliceAddress = CryptoUtils.publicKeyToAddress(alice.getPublic());
        bobAddress = CryptoUtils.publicKeyToAddress(bob.getPublic());
        utxoSet = new UTXOSet();
        reward = Transaction.coinbase(aliceAddress, 50 * SATOSHIS_PER_BTC, 0);
        utxoSet.addTransaction(reward);
    }

    // Construit une transaction qui dépense les 50 BTC d'Alice : amountToBob pour
    // Bob, le reste moins 0,1 BTC de frais rendu à Alice. signer permet de tester
    // une signature faite avec la mauvaise clé
    private Transaction aliceToBob(KeyPair signer, long amountToBob) {
        Transaction tx = new Transaction(
                List.of(new TransactionInput(reward.getId(), 0)),
                List.of(new TransactionOutput(amountToBob, bobAddress),
                        new TransactionOutput(50 * SATOSHIS_PER_BTC - amountToBob - SATOSHIS_PER_BTC / 10,
                                aliceAddress)));
        tx.sign(signer.getPrivate(), signer.getPublic());
        return tx;
    }

    // Cas normal : transaction valide, et frais = entrées − sorties = 0,1 BTC
    @Test
    void transactionValide_etFrais() {
        Transaction tx = aliceToBob(alice, 10 * SATOSHIS_PER_BTC);
        assertTrue(utxoSet.isValid(tx));
        assertEquals(SATOSHIS_PER_BTC / 10, tx.getFee(utxoSet));
    }

    // Règle a) : l'entrée référence une UTXO qui n'existe pas
    @Test
    void rejet_utxoInexistante() {
        Transaction tx = new Transaction(
                List.of(new TransactionInput("inexistant", 0)),
                List.of(new TransactionOutput(SATOSHIS_PER_BTC, bobAddress)));
        tx.sign(alice.getPrivate(), alice.getPublic());
        assertFalse(utxoSet.isValid(tx));
    }

    // Règle b) : on essaie de créer 51 BTC à partir de 50 BTC
    @Test
    void rejet_sortiesSuperieuresAuxEntrees() {
        Transaction tx = new Transaction(
                List.of(new TransactionInput(reward.getId(), 0)),
                List.of(new TransactionOutput(51 * SATOSHIS_PER_BTC, bobAddress)));
        tx.sign(alice.getPrivate(), alice.getPublic());
        assertFalse(utxoSet.isValid(tx));
    }

    // Règle c) : Bob signe une transaction qui dépense l'UTXO d'Alice
    @Test
    void rejet_mauvaiseSignature() {
        assertFalse(utxoSet.isValid(aliceToBob(bob, 10 * SATOSHIS_PER_BTC)));
    }

    // Une fois la 1re transaction appliquée, l'UTXO d'Alice est dépensée : la 2e,
    // qui la réutilise, est rejetée
    @Test
    void rejet_doubleDepense() {
        Transaction first = aliceToBob(alice, 10 * SATOSHIS_PER_BTC);
        utxoSet.addTransaction(first);
        Transaction second = aliceToBob(alice, 20 * SATOSHIS_PER_BTC);
        assertFalse(utxoSet.isValid(second));
        assertNotNull(utxoSet.get(first.getId(), 0)); // les 10 BTC de Bob existent
    }
}