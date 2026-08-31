package net.kothplugin.storage;

import net.kothplugin.stats.PlayerStats;

import java.util.List;
import java.util.UUID;

/**
 * Abstrakce nad úložištěm statistik hráčů. Implementace: {@link YamlStorage} (výchozí)
 * a {@link MySQLStorage} (volitelné, pro síť serverů sdílejících jednu databázi).
 */
public interface StorageManager {

    /** Inicializuje úložiště (vytvoří soubory/tabulky, otevře spojení). */
    void init();

    /** Uzavře úložiště (spojení s DB, uložení na disk). */
    void close();

    /** Načte statistiky hráče, nebo vytvoří nový prázdný záznam, pokud neexistuje. */
    PlayerStats getOrCreate(UUID uuid, String name);

    /** Uloží / aktualizuje statistiky hráče. */
    void save(PlayerStats stats);

    /** Vrátí TOP N hráčů seřazených podle počtu výher sestupně. */
    List<PlayerStats> getTop(int limit);

    /** Vrátí pořadí (rank, 1-indexed) daného hráče v žebříčku, nebo -1 pokud nemá žádné výhry. */
    int getRank(UUID uuid);

    /** Vynutí okamžité uložení všech dat na disk / do DB (volané i periodicky autosave taskem). */
    void flush();
}
