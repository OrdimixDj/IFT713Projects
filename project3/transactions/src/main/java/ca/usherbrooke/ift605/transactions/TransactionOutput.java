package ca.usherbrooke.ift605.transactions;

import java.io.Serializable;

/**
 * Sortie de transaction : un montant verrouillé sur une adresse.
 *
 * @param amount       le montant, en satoshis
 * @param scriptPubKey l'adresse du destinataire (script P2PKH simplifié)
 */
public record TransactionOutput(long amount, String scriptPubKey) implements Serializable {
}