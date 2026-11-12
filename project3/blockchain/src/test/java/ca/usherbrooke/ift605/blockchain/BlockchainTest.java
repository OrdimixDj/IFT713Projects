package ca.usherbrooke.ift605.blockchain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockchainTest {

    @Test
    void tamperEvident() {
        Block<SimpleTx> b1 = new Block<>(Blockchain.GENESIS_PREVIOUS_HASH,
                List.of(new SimpleTx("Alice → Bob : 10")));
        Block<SimpleTx> b2 = new Block<>(b1.computeHash(),
                List.of(new SimpleTx("Bob → Charlie : 5")));

        Blockchain<SimpleTx> chain = new Blockchain<>();
        chain.addBlock(b1); chain.addBlock(b2);
        assert chain.isValid(); // OK
        b1.getTransactions().get(0).tamper(); // modification frauduleuse
        assert !chain.isValid(); // chaîne corrompue détectée
    }

    // Un bloc qui ne pointe pas vers le hash du bloc précédent rend la chaîne invalide
    @Test
    void mauvaisPointeurDeHachage() {
        Block<SimpleTx> b1 = new Block<>(Blockchain.GENESIS_PREVIOUS_HASH, List.of(new SimpleTx("a")));
        Block<SimpleTx> b2 = new Block<>("faux-hash", List.of(new SimpleTx("b")));
        Blockchain<SimpleTx> chain = new Blockchain<>();
        chain.addBlock(b1); chain.addBlock(b2);
        assertFalse(chain.isValid());
    }

    // test de getBlock et getLatestBlock
    @Test
    void accesAuxBlocs() {
        Block<SimpleTx> b1 = new Block<>(Blockchain.GENESIS_PREVIOUS_HASH, List.of(new SimpleTx("a")));
        Block<SimpleTx> b2 = new Block<>(b1.computeHash(), List.of(new SimpleTx("b")));
        Blockchain<SimpleTx> chain = new Blockchain<>();
        chain.addBlock(b1); chain.addBlock(b2);
        assertSame(b1, chain.getBlock(0));
        assertSame(b2, chain.getLatestBlock());
    }
}