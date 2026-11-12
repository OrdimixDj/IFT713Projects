package ca.usherbrooke.ift605.blockchain;

import java.nio.charset.StandardCharsets;

public record BlockHeader(int version, String previousHash, String merkleRoot, long timestamp, long nonce) {

    public byte[] toBytes() {
        String s = version + previousHash + merkleRoot + timestamp + nonce;
        return s.getBytes(StandardCharsets.UTF_8);
    }
}