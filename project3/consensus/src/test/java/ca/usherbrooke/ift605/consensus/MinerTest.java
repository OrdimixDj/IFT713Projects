package ca.usherbrooke.ift605.consensus;

import ca.usherbrooke.ift605.blockchain.Block;
import ca.usherbrooke.ift605.blockchain.Blockchain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MinerTest {

    @Test
    void blocMine_respecteLaDifficulte() {
        Block<String> mined = Miner.mine(new Block<>(Blockchain.GENESIS_PREVIOUS_HASH, List.of("tx")), 4);
        assertTrue(mined.computeHash().startsWith("0000"));
    }

    // Les blocs minés restent chaînés correctement
    @Test
    void chaineDeBlocsMines_valide() {
        Blockchain<String> chain = new Blockchain<>();
        chain.addBlock(Miner.mine(new Block<>(Blockchain.GENESIS_PREVIOUS_HASH, List.of("a")), 3));
        chain.addBlock(Miner.mine(new Block<>(chain.getLatestBlock().computeHash(), List.of("b")), 3));
        assertTrue(chain.isValid());
    }
}