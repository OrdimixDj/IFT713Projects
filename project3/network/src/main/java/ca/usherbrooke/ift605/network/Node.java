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

/**
 * Nœud du réseau P2P : serveur TCP qui écoute sur un port configurable, avec sa
 * propre copie de la chaîne, de l'ensemble UTXO et du mempool.
 */
public class Node {

    /**
     * Nombre maximal de pairs connus (selon l'énoncé : max 8 connexions actives).
     */
    public static final int MAX_PEERS = 8;
    /** Difficulté de minage fixe du réseau. */
    public static final int DIFFICULTY = 3;
    /** Récompense d'un bloc (50 BTC), en satoshis. */
    public static final long BLOCK_REWARD = 50 * SATOSHIS_PER_BTC;

    private final int port;
    private final List<Integer> peers = new ArrayList<>();
    private final Set<String> seen = new HashSet<>(); // hash des tx et blocs déjà traités (anti-boucle)
    private final Blockchain<Transaction> chain = new Blockchain<>();
    private final UTXOSet utxoSet = new UTXOSet();
    private final List<Transaction> mempool = new ArrayList<>();
    private ServerSocket serverSocket;

    /**
     * Crée un nœud.
     *
     * @param port le port TCP d'écoute, qui sert aussi d'identifiant du nœud
     */
    public Node(int port) {
        this.port = port;
    }

    /**
     * Démarre le serveur TCP sur localhost : un thread accepte les connexions,
     * et chaque connexion est traitée dans son propre thread.
     *
     * @throws IOException si le port est déjà utilisé
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket();
        // Permet de relancer un nœud sur le même port juste après l'avoir arrêté
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress("localhost", port));
        // Thread d'acceptation : attend les connexions entrantes ; chacune est traitée
        // dans son propre thread pour que le nœud puisse recevoir plusieurs messages en
        // même temps. Les threads "démons" n'empêchent pas le programme de se terminer
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

    /**
     * Arrête le serveur TCP.
     *
     * @throws IOException si la fermeture échoue
     */
    public void stop() throws IOException {
        serverSocket.close();
    }

    // Lit un message reçu et le traite selon son type ; HELLO et GET_BLOCKS
    // reçoivent une réponse
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
            // La réponse repart sur la même connexion TCP
            if (reply != null) {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(reply);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            log("Message illisible : " + e.getMessage());
        }
    }

    // Envoie un message à un pair ; attend une réponse seulement si expectReply =
    // true
    private Object send(int peerPort, Message msg, boolean expectReply) {
        try (Socket socket = new Socket("localhost", peerPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(msg);
            out.flush();
            // Le flux de lecture est ouvert seulement si on attend une réponse (HELLO,
            // GET_BLOCKS)
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

    /**
     * Rejoint le réseau : contacte le nœud d'amorçage (seed node), obtient sa liste
     * de pairs, puis se présente à chacun d'eux.
     *
     * @param seedPort le port du nœud d'amorçage
     */
    @SuppressWarnings("unchecked")
    public void connect(int seedPort) {
        List<Integer> discovered = (List<Integer>) send(seedPort, new Message(MessageType.HELLO, port, null), true);
        // Le nœud d'amorçage devient notre premier pair
        addPeer(seedPort);
        if (discovered != null) {
            for (int peer : discovered) {
                if (addPeer(peer)) {
                    // On se présente à chaque pair découvert pour qu'il nous ajoute aussi
                    send(peer, new Message(MessageType.HELLO, port, null), true);
                }
            }
        }
        log("Connecté au réseau — pairs : " + getPeers());
    }

    // Un nouveau nœud se présente : on l'ajoute et on lui renvoie nos autres pairs
    private synchronized ArrayList<Integer> onHello(int senderPort) {
        ArrayList<Integer> known = new ArrayList<>(peers);
        known.remove(Integer.valueOf(senderPort));
        addPeer(senderPort);
        return known;
    }

    // Ajoute un pair s'il est nouveau et que la limite de 8 n'est pas atteinte
    private synchronized boolean addPeer(int peerPort) {
        if (peerPort == port || peers.contains(peerPort) || peers.size() >= MAX_PEERS) {
            return false;
        }
        peers.add(peerPort);
        return true;
    }

    // Gossip/flooding : diffuse à tous les pairs sauf à celui qui a envoyé le
    // message
    private void gossip(MessageType type, Serializable payload, int exceptPort) {
        List<Integer> targets = getPeers();
        targets.remove(Integer.valueOf(exceptPort));
        for (int peer : targets) {
            send(peer, new Message(type, port, payload), false);
        }
    }

    /**
     * Soumet une transaction créée localement (par un portefeuille) : vérification
     * locale, ajout au mempool, puis diffusion à tous les pairs.
     *
     * @param tx la transaction signée
     * @return {@code true} si elle est acceptée ; {@code false} si elle est déjà
     *         connue, invalide ou si c'est une double dépense
     */
    public boolean submitTransaction(Transaction tx) {
        // La validation se fait sous verrou, mais la diffusion se fait hors verrou : si
        // deux nœuds s'envoyaient des messages en gardant leur verrou, ils pourraient
        // se bloquer mutuellement
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

    // Transaction reçue d'un pair : si elle est acceptée, on la retransmet aux
    // autres
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

    // Critères de retransmission : nouveauté, pas de double dépense, script valide
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

    // Double dépense : une entrée absente des UTXO ou déjà dépensée par une tx du
    // mempool
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

    /**
     * Mine un bloc contenant la coinbase et toutes les transactions du mempool,
     * l'ajoute à la chaîne locale, puis le diffuse.
     *
     * @param rewardAddress l'adresse qui reçoit la récompense de 50 BTC
     * @return le bloc miné
     */
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

    // Bloc reçu d'un pair : s'il est accepté, on le retransmet aux autres
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

    // Vérification d'un bloc: prolonge la chaîne locale, PoW, racine de Merkle,
    // transactions valides sans double dépense ; puis mise à jour des UTXO et purge
    // du mempool.
    private boolean acceptBlock(Block<Transaction> block) {
        String hash = block.computeHash();
        // Bloc déjà traité, reçu par un autre chemin du gossip : on l'ignore
        if (!seen.add(hash)) {
            return false;
        }
        // Le bloc doit prolonger notre chaîne, un bloc concurrent est rejeté
        String expectedPrevious = chain.size() == 0
                ? Blockchain.GENESIS_PREVIOUS_HASH
                : chain.getLatestBlock().computeHash();
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
        // Chaque transaction doit être valide et aucune UTXO ne peut être dépensée deux
        // fois dans le même bloc
        Set<OutPoint> spentInBlock = new HashSet<>();
        for (Transaction tx : block.getTransactions()) {
            if (tx.isCoinbase())
                continue;
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

        // Bloc accepté, on l'ajoute et on applique ses transactions à l'ensemble UTXO
        chain.addBlock(block);
        for (Transaction tx : block.getTransactions()) {
            utxoSet.addTransaction(tx);
        }
        // Retire les transactions confirmées et celles devenues invalides
        Set<String> confirmed = new HashSet<>();
        for (Transaction tx : block.getTransactions())
            confirmed.add(tx.getId());
        mempool.removeIf(tx -> confirmed.contains(tx.getId()) || !utxoSet.isValid(tx));
        return true;
    }

    // Renvoie les blocs à partir de la hauteur demandée
    private synchronized ArrayList<Block<Transaction>> onGetBlocks(int fromHeight) {
        ArrayList<Block<Transaction>> blocks = new ArrayList<>();
        for (int h = fromHeight; h < chain.size(); h++) {
            blocks.add(chain.getBlock(h));
        }
        return blocks;
    }

    /**
     * Synchronisation d'un nœud arrivé en retard : télécharge et valide les blocs
     * qui lui manquent.
     *
     * @param fromPort le port du pair qui fournit les blocs
     */
    @SuppressWarnings("unchecked")
    public void synchronize(int fromPort) {
        int fromHeight;
        synchronized (this) {
            fromHeight = chain.size();
        }
        List<Block<Transaction>> blocks = (List<Block<Transaction>>) send(fromPort,
                new Message(MessageType.GET_BLOCKS, port, fromHeight), true);
        int added = 0;
        synchronized (this) {
            if (blocks != null) {
                // Chaque bloc téléchargé est vérifié comme un bloc reçu par gossip, on ne fait
                // pas confiance au pair
                for (Block<Transaction> b : blocks) {
                    if (acceptBlock(b))
                        added++;
                }
            }
        }
        log("Synchronisation : " + added + " blocs téléchargés depuis Node-" + fromPort);
    }

    /**
     * @param address l'adresse
     * @return les UTXO de cette adresse dans l'ensemble UTXO local
     */
    public synchronized Map<OutPoint, TransactionOutput> findUtxos(String address) {
        return utxoSet.findByAddress(address);
    }

    /** @return une copie de la liste des ports des pairs connus */
    public synchronized List<Integer> getPeers() {
        return new ArrayList<>(peers);
    }

    /** @return le nombre de blocs de la chaîne locale */
    public synchronized int getChainHeight() {
        return chain.size();
    }

    /**
     * @param height la hauteur du bloc (0 = genesis)
     * @return le bloc à cette hauteur dans la chaîne locale
     */
    public synchronized Block<Transaction> getBlock(int height) {
        return chain.getBlock(height);
    }

    /**
     * @param txId l'id de la transaction
     * @return {@code true} si la transaction est en attente dans le mempool local
     */
    public synchronized boolean hasInMempool(String txId) {
        return mempool.stream().anyMatch(tx -> tx.getId().equals(txId));
    }

    /** @return le port d'écoute du nœud */
    public int getPort() {
        return port;
    }

    // Affiche une ligne préfixée par Node-port
    private void log(String text) {
        System.out.println("[Node-" + port + "] " + text);
    }

    // "Alice → Bob (10 BTC)" : émetteur, destinataire et montant de la sortie 0
    static String describe(Transaction tx) {
        String from = AddressBook.nameOf(CryptoUtils.addressFromBytes(tx.getInputs().get(0).getPublicKey()));
        TransactionOutput payment = tx.getOutputs().get(0);
        return from + " → " + AddressBook.nameOf(payment.scriptPubKey()) + " (" + btc(payment.amount()) + " BTC)";
    }

    // Convertit des satoshis en BTC, sans zéros inutiles (5000000000 devient 50)
    static String btc(long satoshis) {
        return BigDecimal.valueOf(satoshis, 8).stripTrailingZeros().toPlainString();
    }

    // [6002, 6003] → "6002, 6003"
    private static String joinPorts(List<Integer> ports) {
        List<String> s = new ArrayList<>();
        for (int p : ports)
            s.add(String.valueOf(p));
        return String.join(", ", s);
    }
}