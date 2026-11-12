package ca.usherbrooke.ift605.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {

    // Vérifie que SHA-256 est correct
    @Test
    void sha256Hex_vecteurConnu() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                CryptoUtils.sha256Hex("abc"));
    }

    // Propriétés 1 et 2 : vérifie que l'entrée variable mais que la sortie de taille est fixe (256 bits = 32 octets)
    @Test
    void entreeVariable_tailleFixe() {
        byte[] vide = new byte[0];
        byte[] grand = new byte[1_000_000];
        assertEquals(32, CryptoUtils.sha256(vide).length);
        assertEquals(32, CryptoUtils.sha256("salut".getBytes(StandardCharsets.UTF_8)).length);
        assertEquals(32, CryptoUtils.sha256(grand).length);
        assertEquals(64, CryptoUtils.sha256Hex("n'importe quelle longueur").length());
    }

    // Propriété 3 : déterminisme
    @Test
    void deterministe() {
        assertEquals(CryptoUtils.sha256Hex("salut"), CryptoUtils.sha256Hex("salut"));
    }

    // Propriété 4 : résistance aux collisions (aucune collision trouvée sur 10 000 entrées distinctes)
    @Test
    void resistanceAuxCollisions() {
        Set<String> hashes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertTrue(hashes.add(CryptoUtils.sha256Hex("tx-" + i)), "collision trouvée");
        }
    }

    // Propriété 5 : résistance à la préimage (impossible de retrouver l'entrée par force brute)
    @Test
    void resistanceALaPreimage() {
        String cible = CryptoUtils.sha256Hex("secret");
        for (int i = 0; i < 100_000; i++) {
            assertNotEquals(cible, CryptoUtils.sha256Hex(String.valueOf(i)));
        }
    }

    // Propriété 6 : effet avalanche, c'est-à-dire qu'1 caractère modifié engendre qu'environ 50% des 256 bits changent
    @Test
    void effetAvalanche() {
        byte[] h1 = CryptoUtils.sha256("salut".getBytes(StandardCharsets.UTF_8));
        byte[] h2 = CryptoUtils.sha256("sakut".getBytes(StandardCharsets.UTF_8));
        int bitsDifferents = 0;
        for (int i = 0; i < 32; i++) {
            bitsDifferents += Integer.bitCount((h1[i] ^ h2[i]) & 0xff);
        }
        System.out.println("Avalanche : " + bitsDifferents + " bits sur 256 ont changé");
        assertTrue(bitsDifferents > 100 && bitsDifferents < 156);
    }

    @Test
    void engagement_pileOuFace() {
        String nonce  = CryptoUtils.randomNonce();
        String commit = CryptoUtils.commit("PILE", nonce);
        boolean ok = CryptoUtils.verify(commit, "PILE", nonce); // true
        boolean ko = CryptoUtils.verify(commit, "FACE", nonce); // false
        assertTrue(ok);
        assertFalse(ko);
    }

    @Test
    void engagement_dissimulation() {
        assertNotEquals(CryptoUtils.commit("PILE", CryptoUtils.randomNonce()),
                        CryptoUtils.commit("PILE", CryptoUtils.randomNonce()));
    }

    @Test
    void engagement_liaison() {
        String nonce  = CryptoUtils.randomNonce();
        String commit = CryptoUtils.commit("PILE", nonce);
        assertFalse(CryptoUtils.verify(commit, "PILE", CryptoUtils.randomNonce()));
    }

    @Test
    void ecdsa_signerVerifier() {
        KeyPair kp = CryptoUtils.generateKeyPair();
        byte[] sig = CryptoUtils.sign("hello".getBytes(), kp.getPrivate());
        assert CryptoUtils.verify("hello".getBytes(), sig, kp.getPublic()); // OK
        assert !CryptoUtils.verify("world".getBytes(), sig, kp.getPublic()); // KO
    }

    @Test
    void ecdsa_mauvaiseCle() {
        KeyPair alice = CryptoUtils.generateKeyPair();
        KeyPair bob = CryptoUtils.generateKeyPair();
        byte[] sig = CryptoUtils.sign("hello".getBytes(), bob.getPrivate());
        assertFalse(CryptoUtils.verify("hello".getBytes(), sig, alice.getPublic()));
    }

    @Test
    void adresse_base58() {
        KeyPair alice = CryptoUtils.generateKeyPair();
        KeyPair bob = CryptoUtils.generateKeyPair();
        String adresse = CryptoUtils.publicKeyToAddress(alice.getPublic());
        assertEquals(adresse, CryptoUtils.publicKeyToAddress(alice.getPublic()));
        assertNotEquals(adresse, CryptoUtils.publicKeyToAddress(bob.getPublic()));
        assertTrue(adresse.matches("[1-9A-HJ-NP-Za-km-z]+"));
    }
}