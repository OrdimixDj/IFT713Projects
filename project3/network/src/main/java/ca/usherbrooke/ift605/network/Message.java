package ca.usherbrooke.ift605.network;

import java.io.Serializable;

/**
 * Message échangé entre deux nœuds, transmis par TCP avec la sérialisation
 * Java.
 *
 * @param type       le type du message
 * @param senderPort le port du nœud émetteur (son identifiant sur le réseau
 *                   local)
 * @param payload    le contenu : transaction, bloc, hauteur, ou {@code null}
 *                   pour HELLO
 */
public record Message(MessageType type, int senderPort, Serializable payload) implements Serializable {
}