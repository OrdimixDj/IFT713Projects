package ca.usherbrooke.ift605.transactions;

import ca.usherbrooke.ift605.crypto.CryptoUtils;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.*;

class P2PKHScriptTest {

    // Données signées communes aux tests (joue le rôle de l'id de la transaction)
    private final byte[] data = "transaction".getBytes();

    // Cas normal : Alice signe et fournit sa clé publique, l'UTXO est à son adresse
    // : le script réussit
    @Test
    void scriptValide() {
        KeyPair alice = CryptoUtils.generateKeyPair();
        byte[] sig = CryptoUtils.sign(data, alice.getPrivate());
        String address = CryptoUtils.publicKeyToAddress(alice.getPublic());
        assertTrue(P2PKHScript.execute(sig, alice.getPublic().getEncoded(), address, data));
    }

    // OP_EQUALVERIFY échoue : Bob essaie de dépenser une UTXO à l'adresse d'Alice
    // avec sa propre clé
    @Test
    void echec_cleQuiNeCorrespondPasALAdresse() {
        KeyPair alice = CryptoUtils.generateKeyPair();
        KeyPair bob = CryptoUtils.generateKeyPair();
        byte[] sig = CryptoUtils.sign(data, bob.getPrivate());
        String aliceAddress = CryptoUtils.publicKeyToAddress(alice.getPublic());
        assertFalse(P2PKHScript.execute(sig, bob.getPublic().getEncoded(), aliceAddress, data));
    }

    // OP_CHECKSIG échoue : la bonne clé, mais la signature porte sur d'autres
    // données
    @Test
    void echec_signatureInvalide() {
        KeyPair alice = CryptoUtils.generateKeyPair();
        byte[] sig = CryptoUtils.sign("autre".getBytes(), alice.getPrivate());
        String address = CryptoUtils.publicKeyToAddress(alice.getPublic());
        assertFalse(P2PKHScript.execute(sig, alice.getPublic().getEncoded(), address, data));
    }
}