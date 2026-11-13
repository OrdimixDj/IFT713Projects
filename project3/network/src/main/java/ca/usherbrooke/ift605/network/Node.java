package ca.usherbrooke.ift605.network;

import ca.usherbrooke.ift605.blockchain.Block;
import ca.usherbrooke.ift605.blockchain.Blockchain;
import ca.usherbrooke.ift605.blockchain.MerkleTree;
import ca.usherbrooke.ift605.consensus.Miner;
import ca.usherbrooke.ift605.crypto.CryptoUtils;
import ca.usherbrooke.ift605.transactions.OutPoint;
import ca.usherbrooke.ift605.transactions.Transaction;
import ca.usherbrooke.ift605.transactions.TransactionInput;
import ca.usherbrooke.ift605.transactions.TransactionOutput;
import ca.usherbrooke.ift605.transactions.UTXOSet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ca.usherbrooke.ift605.transactions.Transaction.SATOSHIS_PER_BTC;

public class Node {

    public static final int MAX_PEERS = 8;
    public static final int DIFFICULTY = 3;
    public static final long BLOCK_REWARD = 50 * SATOSHIS_PER_BTC;

    private final int port;
    private final List<Integer> peers = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private final Blockchain<Transaction> chain = new Blockchain<>();
    private final UTXOSet utxoSet = new UTXOSet();
    private final List<Transaction> mempool = new ArrayList<>();
    private ServerSocket serverSocket;

    public Node(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("localhost", port));
        Thread acceptor = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    Thread handler = new Thread(() -> handle(socket));
                    handler.setDaemon(true);
                    handler.start();
                } catch (IOException e) {
                    // serveur fermé
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public void stop() throws IOException {
        serverSocket.close();
    }

    @SuppressWarnings("unchecked")
    private void handle(Socket socket) {
        try (socket; ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Message msg = (Message) in.readObject();
            Object reply = switch (msg.type()) {
                case HELLO -> onHello(msg.senderPort());
                case GET_BLOCKS -> onGetBlocks((Integer) msg.payload());
                case TX -> {
                    onTransaction((Transaction) msg.payload(), msg.senderPort());
                    yield null;
                }
                case BLOCK -> {
                    onBlock((Block<Transaction>) msg.payload(), msg.senderPort());
                    yield null;
                }
            };
            if (reply != null) {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(reply);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            log("Message illisible : " + e.getMessage());
        }
    }

    private Object send(int peerPort, Message msg, boolean expectReply) {
        try (Socket socket = new Socket("localhost", peerPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(msg);
            out.flush();
            if (expectReply) {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                return in.readObject();
            }
            return null;
        } catch (IOException | ClassNotFoundException e) {
            log("Pair " + peerPort + " injoignable");
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public void connect(int seedPort) {
        List<Integer> discovered = (List<Integer>) send(seedPort, new Message(MessageType.HELLO, port, null), true);
        addPeer(seedPort);
        if (discovered != null) {
            for (int peer : discovered) {
                if (addPeer(peer)) {
                    send(peer, new Message(MessageType.HELLO, port, null), true);
                }
            }
        }
        log("Connecté au réseau — pairs : " + getPeers());
    }

    private synchronized ArrayList<Integer> onHello(int senderPort) {
        ArrayList<Integer> known = new ArrayList<>(peers);
        known.remove(Integer.valueOf(senderPort));
        addPeer(senderPort);
        return known;
    }

    private synchronized boolean addPeer(int peerPort) {
        if (peerPort == port || peers.contains(peerPort) || peers.size() >= MAX_PEERS) {
            return false;
        }
        peers.add(peerPort);
        return true;
    }

    private void gossip(MessageType type, Serializable payload, int exceptPort) {
        List<Integer> targets = getPeers();
        targets.remove(Integer.valueOf(exceptPort));
        for (int peer : targets) {
            send(peer, new Message(type, port, payload), false);
        }
    }

    public boolean submitTransaction(Transaction tx) {
        boolean accepted;
        synchronized (this) {
            accepted = acceptTransaction(tx);
        }
        if (accepted) {
            log("Tx diffusée : " + describe(tx) + " [gossip → " + joinPorts(getPeers()) + "]");
            gossip(MessageType.TX, tx, -1);
        }
        return accepted;
    }

    private void onTransaction(Transaction tx, int senderPort) {
        boolean accepted;
        synchronized (this) {
            accepted = acceptTransaction(tx);
            if (accepted) {
                log("Tx reçue et validée  : " + describe(tx));
            }
        }
        if (accepted) {
            gossip(MessageType.TX, tx, senderPort);
        }
    }

    private boolean acceptTransaction(Transaction tx) {
        if (!seen.add(tx.getId())) {
            return false;
        }
        if (isDoubleSpend(tx)) {
            log("Double dépense détectée — transaction rejetée");
            return false;
        }
        if (!utxoSet.isValid(tx)) {
            log("Transaction invalide — rejetée");
            return false;
        }
        mempool.add(tx);
        return true;
    }

    private boolean isDoubleSpend(Transaction tx) {
        Set<OutPoint> spentInMempool = new HashSet<>();
        for (Transaction pending : mempool) {
            for (TransactionInput in : pending.getInputs()) {
                spentInMempool.add(new OutPoint(in.getTxId(), in.getOutputIndex()));
            }
        }
        for (TransactionInput in : tx.getInputs()) {
            OutPoint outPoint = new OutPoint(in.getTxId(), in.getOutputIndex());
            if (utxoSet.get(in.getTxId(), in.getOutputIndex()) == null || spentInMempool.contains(outPoint)) {
                return true;
            }
        }
        return false;
    }

    public Block<Transaction> mineBlock(String rewardAddress) {
        Block<Transaction> mined;
        synchronized (this) {
            int height = chain.size();
            String previousHash = height == 0 ? Blockchain.GENESIS_PREVIOUS_HASH : chain.getLatestBlock().computeHash();
            List<Transaction> txs = new ArrayList<>();
            txs.add(Transaction.coinbase(rewardAddress, BLOCK_REWARD, height));
            txs.addAll(mempool);
            mined = Miner.mine(new Block<>(previousHash, txs), DIFFICULTY);
            acceptBlock(mined);
            if (height == 0) {
                log("Bloc #0 (genesis) miné — récompense: " + btc(BLOCK_REWARD) + " BTC → "
                        + AddressBook.nameOf(rewardAddress));
            } else {
                log("Bloc #" + height + " miné — " + (txs.size() - 1) + " transaction(s) confirmée(s)");
            }
        }
        gossip(MessageType.BLOCK, mined, -1);
        return mined;
    }

    private void onBlock(Block<Transaction> block, int senderPort) {
        boolean accepted;
        synchronized (this) {
            accepted = acceptBlock(block);
            if (accepted) {
                log("Bloc #" + (chain.size() - 1) + " reçu et ajouté à la chaîne");
            }
        }
        if (accepted) {
            gossip(MessageType.BLOCK, block, senderPort);
        }
    }

    private boolean acceptBlock(Block<Transaction> block) {
        String hash = block.computeHash();
        if (!seen.add(hash)) {
            return false;
        }
        String expectedPrevious = chain.size() == 0
                ? Blockchain.GENESIS_PREVIOUS_HASH : chain.getLatestBlock().computeHash();
        if (!expectedPrevious.equals(block.getHeader().previousHash())) {
            log("Bloc rejeté : ne prolonge pas la chaîne locale");
            return false;
        }
        if (!Miner.meetsDifficulty(hash, DIFFICULTY)) {
            log("Bloc rejeté : preuve de travail invalide");
            return false;
        }
        String merkleRoot = HexFormat.of().formatHex(new MerkleTree<>(block.getTransactions()).getMerkleRoot());
        if (!merkleRoot.equals(block.getHeader().merkleRoot())) {
            log("Bloc rejeté : racine de Merkle incorrecte");
            return false;
        }
        Set<OutPoint> spentInBlock = new HashSet<>();
        for (Transaction tx : block.getTransactions()) {
            if (tx.isCoinbase()) continue;
            for (TransactionInput in : tx.getInputs()) {
                if (!spentInBlock.add(new OutPoint(in.getTxId(), in.getOutputIndex()))) {
                    log("Bloc rejeté : double dépense dans le bloc");
                    return false;
                }
            }
            if (!utxoSet.isValid(tx)) {
                log("Bloc rejeté : transaction invalide");
                return false;
            }
        }

        chain.addBlock(block);
        for (Transaction tx : block.getTransactions()) {
            utxoSet.addTransaction(tx);
        }
        Set<String> confirmed = new HashSet<>();
        for (Transaction tx : block.getTransactions()) confirmed.add(tx.getId());
        mempool.removeIf(tx -> confirmed.contains(tx.getId()) || !utxoSet.isValid(tx));
        return true;
    }

    private synchronized ArrayList<Block<Transaction>> onGetBlocks(int fromHeight) {
        ArrayList<Block<Transaction>> blocks = new ArrayList<>();
        for (int h = fromHeight; h < chain.size(); h++) {
            blocks.add(chain.getBlock(h));
        }
        return blocks;
    }

    @SuppressWarnings("unchecked")
    public void synchronize(int fromPort) {
        int fromHeight;
        synchronized (this) {
            fromHeight = chain.size();
        }
        List<Block<Transaction>> blocks =
                (List<Block<Transaction>>) send(fromPort, new Message(MessageType.GET_BLOCKS, port, fromHeight), true);
        int added = 0;
        synchronized (this) {
            if (blocks != null) {
                for (Block<Transaction> b : blocks) {
                    if (acceptBlock(b)) added++;
                }
            }
        }
        log("Synchronisation : " + added + " blocs téléchargés depuis Node-" + fromPort);
    }

    public synchronized Map<OutPoint, TransactionOutput> findUtxos(String address) {
        return utxoSet.findByAddress(address);
    }

    public synchronized List<Integer> getPeers() {
        return new ArrayList<>(peers);
    }

    public synchronized int getChainHeight() {
        return chain.size();
    }

    public synchronized Block<Transaction> getBlock(int height) {
        return chain.getBlock(height);
    }

    public synchronized boolean hasInMempool(String txId) {
        return mempool.stream().anyMatch(tx -> tx.getId().equals(txId));
    }

    public int getPort() {
        return port;
    }

    // ===== Affichage =====

    private void log(String text) {
        System.out.println("[Node-" + port + "] " + text);
    }

    static String describe(Transaction tx) {
        String from = AddressBook.nameOf(CryptoUtils.addressFromBytes(tx.getInputs().get(0).getPublicKey()));
        TransactionOutput payment = tx.getOutputs().get(0);
        return from + " → " + AddressBook.nameOf(payment.scriptPubKey()) + " (" + btc(payment.amount()) + " BTC)";
    }

    static String btc(long satoshis) {
        return BigDecimal.valueOf(satoshis, 8).stripTrailingZeros().toPlainString();
    }

    private static String joinPorts(List<Integer> ports) {
        List<String> s = new ArrayList<>();
        for (int p : ports) s.add(String.valueOf(p));
        return String.join(", ", s);
    }
}