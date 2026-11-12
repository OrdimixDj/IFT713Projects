package ca.usherbrooke.ift605.blockchain;

class SimpleTx {

    private String data;

    SimpleTx(String data) {
        this.data = data;
    }

    void tamper() {
        data = data + " (modifiée)";
    }

    @Override
    public String toString() {
        return data;
    }
}