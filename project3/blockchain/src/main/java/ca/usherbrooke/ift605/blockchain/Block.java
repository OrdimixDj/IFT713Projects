package ca.usherbrooke.ift605.blockchain;

import ca.usherbrooke.ift605.crypto.CryptoUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class Block<T> implements Serializable {
    
    private final BlockHeader header;
    private final List<T> transactions;

    public Block(String previousHash, List<T> transactions) {
        this.transactions = new ArrayList<>(transactions);
        String merkleRoot = HexFormat.of().formatHex(new MerkleTree<>(this.transactions).getMerkleRoot());
        this.header = new BlockHeader(1, previousHash, merkleRoot, System.currentTimeMillis(), 0);
    }

    private Block(BlockHeader header, List<T> transactions) {
        this.header = header;
        this.transactions = transactions;
    }

    public Block<T> withNonce(long nonce) {
        BlockHeader h = new BlockHeader(header.version(), header.previousHash(), header.merkleRoot(),
                header.timestamp(), nonce);
        return new Block<>(h, transactions);
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