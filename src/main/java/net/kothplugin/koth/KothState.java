package net.kothplugin.koth;

/**
 * Reprezentuje aktuální stav jednoho KOTH bodu.
 */
public enum KothState {
    /** Bod je vypnutý / nenastavený, nelze jej spustit. */
    DISABLED,
    /** Bod čeká na spuštění (event neběží). */
    WAITING,
    /** Probíhá odpočet (warmup) před samotným startem capture fáze. */
    WARMUP,
    /** Event běží, bod ještě nikdo nezačal obsazovat. */
    ACTIVE,
    /** Bod se právě obsazuje nějakým hráčem/týmem. */
    CAPTURING,
    /** Bod byl plně obsazen - vítěz byl určen. */
    CAPTURED,
    /** Event doznívá (např. zobrazení výsledků) před návratem do WAITING. */
    ENDING;

    public boolean isRunning() {
        return this == WARMUP || this == ACTIVE || this == CAPTURING || this == CAPTURED || this == ENDING;
    }
}
