package ca.usherbrooke.ift605.transactions;

/**
 * Référence unique vers une sortie de transaction (id de la transaction, index
 * de la sortie).
 * Sert de clé dans l'{@link UTXOSet}.
 *
 * @param txId  l'id de la transaction qui a créé la sortie
 * @param index la position de la sortie dans cette transaction
 */
public record OutPoint(String txId, int index) {
}