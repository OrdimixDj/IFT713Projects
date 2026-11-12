package ca.usherbrooke.ift605.blockchain;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class Block<T> {

    private final BlockHeader header;
    private final List<T> transactions;

    public Block(String previousHash, List<T> transactions) {
        this.transactions = new ArrayList<>(transactions);
        String merkleRoot = HexFormat.of().formatHex(new MerkleTree<>(this.transactions).getMerkleRoot());
        this.header = new BlockHeader(1, previousHash, merkleRoot, System.currentTimeMillis(), 0);
    }

    public BlockHeader getHeader() {
        return header;
    }

    public List<T> getTransactions() {
        return transactions;
    }

    public String computeHash() {
        return HexFormat.of().formatHex(CryptoUtils.sha256(CryptoUtils.sha256(header.toBytes())));
    }
}