# KothPlugin — profesionální King of the Hill plugin (1.21+)

Kompletní, vlastní KOTH systém pro Paper/Purpur servery 1.21+, inspirovaný
funkcemi VeloKoth (https://modrinth.com/plugin/velkoth), napsaný od nuly
v Javě jako standardní Maven projekt.

## ✅ Co plugin umí

- **Neomezený počet KOTH bodů** — vytváření/mazání/konfigurace přes příkazy,
  žádné WorldGuard není potřeba (vlastní cuboid zóny nastavené `/koth setpos1` a `setpos2`).
- **Warmup countdown** před startem eventu, s broadcastem, titulky i zvuky.
- **Capture logika**: postupné obsazování, bonus rychlosti za více hráčů v zóně,
  **kontest** (více znepřátelených hráčů/týmů zastaví capture), **decay**
  (progres postupně klesá, když bod nikdo nedrží) — vše nastavitelné v configu.
- **SOLO i TEAM režim** (týmy přes vestavěný Bukkit Scoreboard, žádný extra plugin).
- **Notifikace**: BossBar, ActionBar, Title/Subtitle, zvuky, chat broadcast — každá
  položka se dá jednotlivě zapnout/vypnout.
- **Odměny**: libovolné množství příkazů (`%player%` placeholder), volitelně
  odměna pro celý vítězný tým, Vault ekonomika (peníze) jako hook.
- **Statistiky a žebříček (`/koth top`)** — YAML nebo MySQL úložiště (pro síť serverů).
- **GUI menu** (`/koth gui`) — přehled všech bodů s barevným stavem a teleportací kliknutím.
- **PlaceholderAPI** expanze (`%koth_wins%`, `%koth_rank%`, `%koth_state_<jméno>%`,
  `%koth_progress_<jméno>%`, `%koth_owner_<jméno>%`, `%koth_active_count%`).
- **Auto-start scheduler** — náhodné/postupné automatické spouštění eventů v intervalu.
- **Anticheat volby**: zákaz letu a teleportačních příkazů v capture zóně,
  odebrání progresu při odpojení hráče.
- **Vlastní API eventy** pro napojení dalších pluginů: `KothStartEvent`,
  `KothEndEvent`, `KothCaptureProgressEvent`, `KothWinEvent` (zrušitelný).
- **20 jazyků "z krabice"** + podpora libovolných dalších vlastních jazyků.

## 🌍 Jazyky

V `config.yml` nastavíš:
```yaml
language: "cs"
```
a všechny zprávy hráčům (chat, GUI, tituly, bossbar…) se automaticky přeloží.
Vestavěné jazyky: `en, cs, sk, de, pl, ru, uk, fr, es, pt, it, nl, tr, ro, hu,
sv, fi, da, no, lt` (20 celkem). Soubory se automaticky vygenerují do
`plugins/KothPlugin/lang/messages_<kod>.yml` při prvním spuštění — klidně
uprav libovolný text dle svého. Chceš přidat vlastní jazyk (např. japonštinu)?
Stačí do té složky přidat `messages_ja.yml` se stejnou strukturou klíčů jako
`messages_en.yml` a nastavit `language: "ja"`.

Lze také zapnout `per-player-language.enabled: true` — pak si plugin vezme
jazyk podle klienta hráče (pokud pro něj existuje odpovídající soubor).

Jazyk lze změnit i za běhu příkazem: `/koth setlang cs`

## 🔨 Sestavení (build)

Potřebuješ **JDK 21** a **Maven**. V sandboxu, kde jsem tohle psal, jsem bohužel
neměl přístup k Maven Central / PaperMC repozitářům (síťová izolace), takže
jsem **nemohl projekt zkompilovat a otestovat zde** — dostáváš čistý, pečlivě
napsaný zdrojový kód, který si zkompiluješ během chvilky u sebe:

```bash
cd kothplugin
mvn clean package
```

Výsledný jar najdeš v `target/KothPlugin-1.0.0.jar` (shade plugin do něj
zabalí i drobné závislosti). Ten nahraj do složky `plugins/` na Paper/Purpur
serveru verze **1.21+** a restartuj server.

> 💡 Pokud narazíš při kompilaci na nějaký drobný problém (např. verzi Paper
> API), dej vědět a rád to doladím — kód jsem psal maximálně pečlivě podle
> aktuálního Paper 1.21 API, ale bez lokálního kompilátoru se občas může
> vloudit drobnost.

## 📋 Příkazy

| Příkaz | Popis |
|---|---|
| `/koth create <název>` | Vytvoří nový KOTH bod |
| `/koth delete <název>` | Smaže KOTH bod (vyžaduje potvrzení do 10s) |
| `/koth setpos1` / `setpos2 <název>` | Nastaví rohy capture zóny |
| `/koth setcapturepoint <název>` | Nastaví vlajku / capture bod |
| `/koth start` / `stop <název>` | Spustí / násilně ukončí event |
| `/koth list` | Seznam všech bodů |
| `/koth info <název>` | Detail bodu |
| `/koth tp <název>` | Teleport na bod |
| `/koth top` | Žebříček hráčů |
| `/koth gui` | GUI menu |
| `/koth reload` | Reload configu i jazyků |
| `/koth setwarmup <název> <s>` | Doba odpočtu před startem |
| `/koth setcapturetime <název> <s>` | Doba potřebná k obsazení |
| `/koth setreward <název> <příkaz>` | Přidá odměnový příkaz |
| `/koth setlang <kód>` | Změní jazyk pluginu |

## 🔐 Práva

- `koth.use` (výchozí: true) — základní použití
- `koth.join` (výchozí: true) — účast v eventech
- `koth.admin` (výchozí: op) — správa bodů
- `koth.bypass.cooldown` (výchozí: op)
- `koth.notify` (výchozí: op)

## ⚙️ Poznámky k rozsahu

Toggly `integrations.worldguard.enabled` a `integrations.holograms.enabled`
v configu jsou připravené jako **rozšiřitelné body** pro budoucí verzi (napojení
na WorldGuard regiony místo vlastních cuboidů, hologramy nad capture bodem) —
v této verzi zóny fungují na vlastním jednoduchém cuboid systému a bez
hologramů, což pokrývá naprostou většinu použití a nevyžaduje žádné další
pluginy. Pokud je budeš chtít doplnit, ozvi se a rád je dopíšu.

## 📁 Struktura projektu

```
kothplugin/
├── pom.xml
└── src/main/
    ├── java/net/kothplugin/
    │   ├── KothPlugin.java          (hlavní třída)
    │   ├── koth/                    (Koth, KothManager, CaptureTaskManager, KothState)
    │   ├── config/ConfigManager.java
    │   ├── lang/LangManager.java
    │   ├── commands/KothCommand.java
    │   ├── gui/KothGUI.java
    │   ├── listeners/               (GUI kliknutí, anticheat, odchod hráče)
    │   ├── hooks/                   (Vault, PlaceholderAPI)
    │   ├── storage/                 (YAML, MySQL)
    │   ├── stats/                   (StatsManager, PlayerStats)
    │   ├── scheduler/AutoStartScheduler.java
    │   ├── events/                  (vlastní API eventy)
    │   └── utils/                   (Cuboid, ItemBuilder, TeamUtils)
    └── resources/
        ├── plugin.yml
        ├── config.yml
        └── lang/messages_<20 jazyků>.yml
```
