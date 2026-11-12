package ca.usherbrooke.ift605.blockchain;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class Blockchain<T> {

    // previousHash du premier bloc (il n'a pas de prédécesseur)
    public static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);

    private final List<Block<T>> blocks = new ArrayList<>();

    public void addBlock(Block<T> b) {
        blocks.add(b);
    }

    public Block<T> getBlock(int height) {
        return blocks.get(height);
    }

    public Block<T> getLatestBlock() {
        return blocks.get(blocks.size() - 1);
    }

    public boolean isValid() {
        for (int i = 0; i < blocks.size(); i++) {
            Block<T> block = blocks.get(i);

            String merkleRoot = HexFormat.of().formatHex(
                    new MerkleTree<>(block.getTransactions()).getMerkleRoot());
            if (!merkleRoot.equals(block.getHeader().merkleRoot())) {
                return false;
            }

            String expectedPrevious = (i == 0) ? GENESIS_PREVIOUS_HASH : blocks.get(i - 1).computeHash();
            if (!expectedPrevious.equals(block.getHeader().previousHash())) {
                return false;
            }
        }
        return true;
    }
}