package ca.usherbrooke.ift605.network;

/**
 * Types de messages échangés entre nœuds.
 */
public enum MessageType {
    /**
     * Connexion : "je suis le nœud X" ; la réponse est la liste des pairs connus
     */
    HELLO,
    /** Diffusion gossip d'une transaction ; pas de réponse. */
    TX,
    /** Diffusion gossip d'un bloc ; pas de réponse. */
    BLOCK,
    /**
     * Synchronisation : "envoie-moi les blocs à partir de la hauteur h" ; la
     * réponse est une liste de blocs.
     */
    GET_BLOCKS
}