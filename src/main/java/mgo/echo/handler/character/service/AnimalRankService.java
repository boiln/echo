package mgo.echo.handler.character.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mgo.echo.data.entity.CharacterStats;

/**
 * Animal Rank Calculation Service
 * 
 * Calculates the animal rank for a character based on their weekly stats.
 * The animal rank is calculated from the previous week's statistics.
 * 
 * Rank IDs match the original MGO2 system:
 * 1 = Foxhound, 2 = Fox, 3 = Doberman, 4 = Hound, 5 = Crocodile,
 * 6 = Eagle, 7 = Jaws, 8 = Water Bear, 9 = Sloth, 10 = Flying Squirrel,
 * 11 = Pigeon, 12 = Owl, 13 = Tsuchinoko, 14 = Snake, 15 = Kerotan,
 * 16 = GA-KO, 17 = Chameleon, 18 = Chicken, 19 = Bear, 20 = Tortoise,
 * 21 = Bee, 22 = Rat, 23 = Fighting Fish, 24 = Komodo Dragon,
 * 26 = Killer Whale, 27 = Elephant, 28 = Cuckoo, 29 = Hog,
 * 40 = Octopus, 42 = Panda, 43 = Puma, 44 = Scorpion, 46 = Mantis,
 * 48 = Night Owl, 50 = Hawk, 52 = Ocelot
 * 
 * 0 = No rank (default)
 */
public class AnimalRankService {

    /**
     * Calculate the animal rank for a character based on their stats.
     * Stats should be from the previous week.
     * 
     * @param stats              The character's weekly stats
     * @param daysSinceLastLogin Days since last login (for Tsuchinoko)
     * @return The animal rank ID (0 if no rank qualifies)
     */
    public static int calculateAnimalRank(CharacterStats stats, int daysSinceLastLogin) {
        if (stats == null) {
            return 0;
        }

        // Parse per-mode stats
        ModeStats dm = parseModeStats(stats.getStatsDm());
        ModeStats tdm = parseModeStats(stats.getStatsTdm());
        ModeStats cap = parseModeStats(stats.getStatsCap());
        ModeStats base = parseModeStats(stats.getStatsBase());
        ModeStats bomb = parseModeStats(stats.getStatsBomb());
        ModeStats race = parseModeStats(stats.getStatsRace());
        ModeStats res = parseModeStats(stats.getStatsRes());
        ModeStats sne = parseModeStats(stats.getStatsSne());
        ModeStats tsne = parseModeStats(stats.getStatsTsne());
        ModeStats sdm = parseModeStats(stats.getStatsSdm());

        int totalRounds = stats.getRounds();
        if (totalRounds == 0) {
            totalRounds = 1; // Prevent division by zero
        }

        // Check ranks in priority order (highest rank first)
        // Priority: Special ranks first, then Foxhound family, then others

        // ===== FOXHOUND FAMILY (Multi-requirement ranks) =====

        // 1 = Foxhound (highest skill rank)
        if (checkFoxhound(stats, dm, tdm, cap, base, bomb, race, res, sne, tsne)) {
            return 1;
        }

        // 2 = Fox
        if (checkFox(stats, dm, tdm, cap, base, bomb, race, res, sne, tsne)) {
            return 2;
        }

        // 3 = Doberman
        if (checkDoberman(stats, dm, tdm, cap, base, bomb, race, res, sne, tsne)) {
            return 3;
        }

        // 4 = Hound
        if (checkHound(stats, dm, tdm, cap, base, bomb, race, res, sne, tsne)) {
            return 4;
        }

        // ===== COMBAT STYLE RANKS =====

        // 5 = Crocodile (K+S+D+SR / Rounds >= 1.50)
        if (checkCrocodile(stats, totalRounds)) {
            return 5;
        }

        // 6 = Eagle (K+S+D+SR >= 1.30 AND headshots/kills >= 0.50)
        if (checkEagle(stats, totalRounds)) {
            return 6;
        }

        // 7 = Jaws (K+S+D+SR >= 1.25 AND knife kills ratio >= 0.075)
        if (checkJaws(stats, totalRounds)) {
            return 7;
        }

        // 11 = Pigeon (stuns/kills >= 1.20 AND stuns/stunsRec >= 1.20)
        if (checkPigeon(stats)) {
            return 11;
        }

        // 44 = Scorpion (knife stuns / total rounds >= 1)
        if (checkScorpion(stats, totalRounds)) {
            return 44;
        }

        // 52 = Ocelot (lock-on kills ratio >= 0.40)
        if (checkOcelot(stats)) {
            return 52;
        }

        // ===== EQUIPMENT/PLAYSTYLE RANKS =====

        // 19 = Bear (CQC per round >= 5)
        if (checkBear(stats, totalRounds)) {
            return 19;
        }

        // 20 = Tortoise (box uses per round >= 15)
        if (checkTortoise(stats, totalRounds)) {
            return 20;
        }

        // 21 = Bee (scans per round >= 0.30)
        if (checkBee(stats, totalRounds)) {
            return 21;
        }

        // 22 = Rat (trapped per round >= 0.30)
        if (checkRat(stats, totalRounds)) {
            return 22;
        }

        // 12 = Owl (EVG time ratio >= 0.05)
        if (checkOwl(stats)) {
            return 12;
        }

        // 48 = Night Owl (spotted in TSNE + snake spotted in SNE / rounds <= 0.50)
        if (checkNightOwl(stats, tsne, sne)) {
            return 48;
        }

        // 50 = Hawk (spotted per TSNE round >= 0.30)
        if (checkHawk(stats, tsne)) {
            return 50;
        }

        // 10 = Flying Squirrel (rolls per round >= 15)
        if (checkFlyingSquirrel(stats, totalRounds)) {
            return 10;
        }

        // ===== GAME MODE SPECIALIST RANKS =====

        // 23 = Fighting Fish (DM specialist: 60% ratio, 30 rounds)
        if (checkFightingFish(dm, totalRounds)) {
            return 23;
        }

        // 24 = Komodo Dragon (SDM specialist: 60% ratio, 30 rounds)
        if (checkKomodoDragon(sdm, totalRounds)) {
            return 24;
        }

        // 26 = Killer Whale (TDM specialist: 60% ratio, 30 rounds)
        if (checkKillerWhale(tdm, totalRounds)) {
            return 26;
        }

        // 27 = Elephant (BASE specialist: 60% ratio, 30 rounds)
        if (checkElephant(base, totalRounds)) {
            return 27;
        }

        // 28 = Cuckoo (BOMB specialist: 60% ratio, 30 rounds)
        if (checkCuckoo(bomb, totalRounds)) {
            return 28;
        }

        // 29 = Hog (RACE specialist: 60% ratio, 30 rounds)
        if (checkHog(race, totalRounds)) {
            return 29;
        }

        // 14 = Snake (SNE specialist: 60% ratio, 30 rounds)
        if (checkSnake(sne, totalRounds)) {
            return 14;
        }

        // 15 = Kerotan (CAP specialist: 60% ratio, 30 rounds)
        if (checkKerotan(cap, totalRounds)) {
            return 15;
        }

        // 16 = GA-KO (RES specialist: 60% ratio, 30 rounds)
        if (checkGako(res, totalRounds)) {
            return 16;
        }

        // 17 = Chameleon (TSNE specialist: 60% ratio, 30 rounds)
        if (checkChameleon(tsne, totalRounds)) {
            return 17;
        }

        // ===== SNAKE MODE SPECIFIC RANKS =====

        // 43 = Puma (snake holdups / snake rounds >= 2)
        if (checkPuma(stats, sne)) {
            return 43;
        }

        // 40 = Octopus (spotted ratio in TSNE+SNE <= 0.15)
        if (checkOctopus(stats, sne, tsne)) {
            return 40;
        }

        // 42 = Panda (bases captured / BASE rounds >= 2.5)
        if (checkPanda(stats, base)) {
            return 42;
        }

        // 46 = Mantis (wakeups in TSNE / TSNE rounds >= 0.30)
        if (checkMantis(stats, tsne)) {
            return 46;
        }

        // ===== SURVIVAL/DEFENSE RANKS =====

        // 8 = Water Bear (survival ratio in RES + TSNE <= 0.50)
        if (checkWaterBear(stats, res, tsne)) {
            return 8;
        }

        // ===== NEGATIVE/PASSIVE RANKS =====

        // 9 = Sloth (K/D <= 0.85, headshot death ratio >= 0.60, stun ratio <= 0.85)
        if (checkSloth(stats)) {
            return 9;
        }

        // 18 = Chicken (kills/round <= 0.30, stuns/round <= 0.30, etc.)
        if (checkChicken(stats, totalRounds)) {
            return 18;
        }

        // 13 = Tsuchinoko (last login >= 30 days)
        if (daysSinceLastLogin >= 30) {
            return 13;
        }

        return 0; // No rank
    }

    // ===== FOXHOUND FAMILY =====

    private static boolean checkFoxhound(CharacterStats stats, ModeStats dm, ModeStats tdm,
            ModeStats cap, ModeStats base, ModeStats bomb, ModeStats race,
            ModeStats res, ModeStats sne, ModeStats tsne) {
        int totalRounds = stats.getRounds();
        if (totalRounds < 100) {
            return false;
        }

        // K+S+D+SR ratio in DM, TDM, SNE >= 1.45
        double kdsrr = calculateKDSRR(stats, dm, tdm, sne);
        if (kdsrr < 1.45) {
            return false;
        }

        // Win % in CAP, BASE, BOMB, RES, TSNE >= 52.5%
        double winRate = calculateWinRate(cap, base, bomb, res, tsne);
        if (winRate < 0.525) {
            return false;
        }

        // Win % in RACE >= 50%
        if (race.rounds > 0 && (double) race.wins / race.rounds < 0.50) {
            return false;
        }

        // Bases captured / BASE rounds >= 1.60
        if (base.rounds > 0 && (double) stats.getBasesCaptured() / base.rounds < 1.60) {
            return false;
        }

        // Withdrawal % <= 2%
        if (totalRounds > 0 && (double) stats.getWithdrawals() / totalRounds > 0.02) {
            return false;
        }

        return true;
    }

    private static boolean checkFox(CharacterStats stats, ModeStats dm, ModeStats tdm,
            ModeStats cap, ModeStats base, ModeStats bomb, ModeStats race,
            ModeStats res, ModeStats sne, ModeStats tsne) {
        int totalRounds = stats.getRounds();
        if (totalRounds < 50) {
            return false;
        }

        double kdsrr = calculateKDSRR(stats, dm, tdm, sne);
        if (kdsrr < 1.40) {
            return false;
        }

        double winRate = calculateWinRate(cap, base, bomb, res, tsne);
        if (winRate < 0.475) {
            return false;
        }

        if (race.rounds > 0 && (double) race.wins / race.rounds < 0.45) {
            return false;
        }

        if (base.rounds > 0 && (double) stats.getBasesCaptured() / base.rounds < 1.40) {
            return false;
        }

        if (totalRounds > 0 && (double) stats.getWithdrawals() / totalRounds > 0.02) {
            return false;
        }

        return true;
    }

    private static boolean checkDoberman(CharacterStats stats, ModeStats dm, ModeStats tdm,
            ModeStats cap, ModeStats base, ModeStats bomb, ModeStats race,
            ModeStats res, ModeStats sne, ModeStats tsne) {
        int totalRounds = stats.getRounds();
        if (totalRounds < 25) {
            return false;
        }

        double kdsrr = calculateKDSRR(stats, dm, tdm, sne);
        if (kdsrr < 1.35) {
            return false;
        }

        double winRate = calculateWinRate(cap, base, bomb, res, tsne);
        if (winRate < 0.45) {
            return false;
        }

        if (race.rounds > 0 && (double) race.wins / race.rounds < 0.425) {
            return false;
        }

        if (base.rounds > 0 && (double) stats.getBasesCaptured() / base.rounds < 1.20) {
            return false;
        }

        if (totalRounds > 0 && (double) stats.getWithdrawals() / totalRounds > 0.04) {
            return false;
        }

        return true;
    }

    private static boolean checkHound(CharacterStats stats, ModeStats dm, ModeStats tdm,
            ModeStats cap, ModeStats base, ModeStats bomb, ModeStats race,
            ModeStats res, ModeStats sne, ModeStats tsne) {
        int totalRounds = stats.getRounds();
        if (totalRounds < 5) {
            return false;
        }

        double kdsrr = calculateKDSRR(stats, dm, tdm, sne);
        if (kdsrr < 1.30) {
            return false;
        }

        double winRate = calculateWinRate(cap, base, bomb, res, tsne);
        if (winRate < 0.425) {
            return false;
        }

        if (race.rounds > 0 && (double) race.wins / race.rounds < 0.40) {
            return false;
        }

        if (base.rounds > 0 && (double) stats.getBasesCaptured() / base.rounds < 1.00) {
            return false;
        }

        if (totalRounds > 0 && (double) stats.getWithdrawals() / totalRounds > 0.04) {
            return false;
        }

        return true;
    }

    // ===== COMBAT STYLE RANKS =====

    private static boolean checkCrocodile(CharacterStats stats, int totalRounds) {
        // (kills + stuns + deaths + stunsReceived) / rounds >= 1.50
        double ratio = (double) (stats.getKills() + stats.getStuns() + stats.getDeaths()
                + stats.getStunsReceived()) / totalRounds;
        return ratio >= 1.50;
    }

    private static boolean checkEagle(CharacterStats stats, int totalRounds) {
        // KDSRR >= 1.30 AND headshots/body kills >= 0.50
        double kdsrr = (double) (stats.getKills() + stats.getStuns() + stats.getDeaths()
                + stats.getStunsReceived()) / totalRounds;
        if (kdsrr < 1.30) {
            return false;
        }

        int kills = stats.getKills();
        if (kills == 0) {
            return false;
        }

        double headshotRatio = (double) stats.getHeadshotKills() / kills;
        return headshotRatio >= 0.50;
    }

    private static boolean checkJaws(CharacterStats stats, int totalRounds) {
        // KDSRR >= 1.25 AND knife kills ratio >= 0.075
        double kdsrr = (double) (stats.getKills() + stats.getStuns() + stats.getDeaths()
                + stats.getStunsReceived()) / totalRounds;
        if (kdsrr < 1.25) {
            return false;
        }

        int kills = stats.getKills();
        if (kills == 0) {
            return false;
        }

        double knifeRatio = (double) stats.getKnifeKills() / kills;
        return knifeRatio >= 0.075;
    }

    private static boolean checkPigeon(CharacterStats stats) {
        // Stun/Kill ratio >= 1.20 AND Stun/StunRec ratio >= 1.20
        int kills = stats.getKills();
        int stuns = stats.getStuns();
        int stunsRec = stats.getStunsReceived();

        if (kills == 0 || stunsRec == 0) {
            return false;
        }

        double stunKillRatio = (double) stuns / kills;
        double stunRatio = (double) stuns / stunsRec;

        return stunKillRatio >= 1.20 && stunRatio >= 1.20;
    }

    private static boolean checkScorpion(CharacterStats stats, int totalRounds) {
        // Knife stuns / total rounds >= 1
        return (double) stats.getKnifeStuns() / totalRounds >= 1.0;
    }

    private static boolean checkOcelot(CharacterStats stats) {
        // Lock-on kills / total kills >= 0.40
        int kills = stats.getKills();
        if (kills == 0) {
            return false;
        }
        return (double) stats.getLockKills() / kills >= 0.40;
    }

    // ===== EQUIPMENT/PLAYSTYLE RANKS =====

    private static boolean checkBear(CharacterStats stats, int totalRounds) {
        // CQC given per round >= 5
        return (double) stats.getCqcGiven() / totalRounds >= 5.0;
    }

    private static boolean checkTortoise(CharacterStats stats, int totalRounds) {
        // Box uses per round >= 15
        return (double) stats.getBoxUses() / totalRounds >= 15.0;
    }

    private static boolean checkBee(CharacterStats stats, int totalRounds) {
        // Scans per round >= 0.30
        return (double) stats.getScans() / totalRounds >= 0.30;
    }

    private static boolean checkRat(CharacterStats stats, int totalRounds) {
        // Trapped per round >= 0.30
        return (double) stats.getTrapped() / totalRounds >= 0.30;
    }

    private static boolean checkOwl(CharacterStats stats) {
        // EVG time / total time >= 0.05
        int totalTime = stats.getTime();
        if (totalTime == 0) {
            return false;
        }
        return (double) stats.getEvgTime() / totalTime >= 0.05;
    }

    private static boolean checkNightOwl(CharacterStats stats, ModeStats tsne, ModeStats sne) {
        // (spotted in TSNE + snake spotted in SNE) / (TSNE rounds + SNE rounds) <= 0.50
        int totalRounds = tsne.rounds + sne.rounds;
        if (totalRounds < 15) {
            return false;
        }
        double ratio = (double) (stats.getSpotted() + stats.getSnakeSpotted()) / totalRounds;
        return ratio <= 0.50;
    }

    private static boolean checkHawk(CharacterStats stats, ModeStats tsne) {
        // Spotted per TSNE round >= 0.30
        if (tsne.rounds == 0) {
            return false;
        }
        return (double) stats.getSpotted() / tsne.rounds >= 0.30;
    }

    private static boolean checkFlyingSquirrel(CharacterStats stats, int totalRounds) {
        // Rolls per round >= 15
        return (double) stats.getRolls() / totalRounds >= 15.0;
    }

    // ===== GAME MODE SPECIALIST RANKS =====

    private static boolean checkModeSpecialist(ModeStats mode, int totalRounds, int minRounds) {
        // Mode rounds / total rounds >= 0.60 AND mode rounds >= minRounds
        if (mode.rounds < minRounds) {
            return false;
        }
        if (totalRounds == 0) {
            return false;
        }
        return (double) mode.rounds / totalRounds >= 0.60;
    }

    private static boolean checkFightingFish(ModeStats dm, int totalRounds) {
        return checkModeSpecialist(dm, totalRounds, 30);
    }

    private static boolean checkKomodoDragon(ModeStats sdm, int totalRounds) {
        return checkModeSpecialist(sdm, totalRounds, 30);
    }

    private static boolean checkKillerWhale(ModeStats tdm, int totalRounds) {
        return checkModeSpecialist(tdm, totalRounds, 30);
    }

    private static boolean checkElephant(ModeStats base, int totalRounds) {
        return checkModeSpecialist(base, totalRounds, 30);
    }

    private static boolean checkCuckoo(ModeStats bomb, int totalRounds) {
        return checkModeSpecialist(bomb, totalRounds, 30);
    }

    private static boolean checkHog(ModeStats race, int totalRounds) {
        return checkModeSpecialist(race, totalRounds, 30);
    }

    private static boolean checkSnake(ModeStats sne, int totalRounds) {
        return checkModeSpecialist(sne, totalRounds, 30);
    }

    private static boolean checkKerotan(ModeStats cap, int totalRounds) {
        return checkModeSpecialist(cap, totalRounds, 30);
    }

    private static boolean checkGako(ModeStats res, int totalRounds) {
        return checkModeSpecialist(res, totalRounds, 30);
    }

    private static boolean checkChameleon(ModeStats tsne, int totalRounds) {
        return checkModeSpecialist(tsne, totalRounds, 30);
    }

    // ===== SNAKE MODE SPECIFIC =====

    private static boolean checkPuma(CharacterStats stats, ModeStats sne) {
        // Snake holdups / snake rounds >= 2
        // Also requires at least 15 SNE rounds and 5 snake rounds
        if (sne.rounds < 15) {
            return false;
        }
        int snakeRounds = sne.rounds; // Approximate, ideally would track snake-specific rounds
        if (snakeRounds < 5) {
            return false;
        }
        return (double) stats.getSnakeHoldups() / snakeRounds >= 2.0;
    }

    private static boolean checkOctopus(CharacterStats stats, ModeStats sne, ModeStats tsne) {
        // (spotted in TSNE + snake spotted in SNE) / (TSNE rounds + SNE snake rounds)
        // <=
        // 0.15
        int totalRounds = tsne.rounds + sne.rounds;
        if (totalRounds < 15) {
            return false;
        }
        double ratio = (double) (stats.getSpotted() + stats.getSnakeSpotted()) / totalRounds;
        return ratio <= 0.15;
    }

    private static boolean checkPanda(CharacterStats stats, ModeStats base) {
        // Bases captured / BASE rounds >= 2.5
        if (base.rounds < 15) {
            return false;
        }
        return (double) stats.getBasesCaptured() / base.rounds >= 2.5;
    }

    private static boolean checkMantis(CharacterStats stats, ModeStats tsne) {
        // Wakeups in TSNE / TSNE rounds >= 0.30
        if (tsne.rounds == 0) {
            return false;
        }
        return (double) stats.getWakeups() / tsne.rounds >= 0.30;
    }

    // ===== SURVIVAL/DEFENSE =====

    private static boolean checkWaterBear(CharacterStats stats, ModeStats res, ModeStats tsne) {
        // Survival ratio in RES + TSNE <= 0.50
        // (deaths in these modes / rounds in these modes)
        int totalRounds = res.rounds + tsne.rounds;
        if (totalRounds == 0) {
            return false;
        }
        int totalDeaths = res.deaths + tsne.deaths;
        return (double) totalDeaths / totalRounds <= 0.50;
    }

    // ===== NEGATIVE/PASSIVE RANKS =====

    private static boolean checkSloth(CharacterStats stats) {
        // K/D <= 0.85, headshot death ratio >= 0.60, stun ratio <= 0.85
        int kills = stats.getKills();
        int deaths = stats.getDeaths();
        int stuns = stats.getStuns();
        int stunsRec = stats.getStunsReceived();
        int hsDeaths = stats.getHeadshotDeaths();

        if (deaths == 0 || stunsRec == 0) {
            return false;
        }

        double kd = (double) kills / deaths;
        double hsDeathRatio = (double) hsDeaths / deaths;
        double stunRatio = (double) stuns / stunsRec;

        return kd <= 0.85 && hsDeathRatio >= 0.60 && stunRatio <= 0.85;
    }

    private static boolean checkChicken(CharacterStats stats, int totalRounds) {
        // Kills per round <= 0.30
        // Stuns per round <= 0.30
        // Stuns received per round <= 0.50
        // Deaths per round <= 0.50
        double killsPerRound = (double) stats.getKills() / totalRounds;
        double stunsPerRound = (double) stats.getStuns() / totalRounds;
        double stunsRecPerRound = (double) stats.getStunsReceived() / totalRounds;
        double deathsPerRound = (double) stats.getDeaths() / totalRounds;

        return killsPerRound <= 0.30 && stunsPerRound <= 0.30
                && stunsRecPerRound <= 0.50 && deathsPerRound <= 0.50;
    }

    // ===== HELPER METHODS =====

    /**
     * Calculate KDSRR (Kills + Stuns - Deaths - Stuns Received) ratio
     * for specific modes (DM, TDM, SNE)
     */
    private static double calculateKDSRR(CharacterStats stats, ModeStats dm, ModeStats tdm, ModeStats sne) {
        int totalRounds = dm.rounds + tdm.rounds + sne.rounds;
        if (totalRounds == 0) {
            return 0;
        }

        int kills = dm.kills + tdm.kills + sne.kills;
        int stuns = dm.stuns + tdm.stuns + sne.stuns;
        int deaths = dm.deaths + tdm.deaths + sne.deaths;
        int stunsRec = dm.stunsRec + tdm.stunsRec + sne.stunsRec;

        return (double) (kills + stuns + deaths + stunsRec) / totalRounds;
    }

    /**
     * Calculate average win rate across multiple modes
     */
    private static double calculateWinRate(ModeStats... modes) {
        int totalWins = 0;
        int totalRounds = 0;

        for (ModeStats mode : modes) {
            totalWins += mode.wins;
            totalRounds += mode.rounds;
        }

        if (totalRounds == 0) {
            return 0;
        }

        return (double) totalWins / totalRounds;
    }

    /**
     * Parse per-mode stats JSON string into ModeStats object
     */
    private static ModeStats parseModeStats(String json) {
        ModeStats stats = new ModeStats();
        if (json == null || json.isEmpty()) {
            return stats;
        }

        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            stats.wins = getIntSafe(obj, "wins");
            stats.rounds = getIntSafe(obj, "rounds");
            stats.kills = getIntSafe(obj, "kills");
            stats.deaths = getIntSafe(obj, "deaths");
            stats.stuns = getIntSafe(obj, "stuns");
            stats.stunsRec = getIntSafe(obj, "stunsRec");
            stats.score = getIntSafe(obj, "score");
            stats.time = getIntSafe(obj, "time");
        } catch (Exception e) {
            // Return default empty stats on parse error
        }

        return stats;
    }

    private static int getIntSafe(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return 0;
    }

    /**
     * Per-mode statistics container
     */
    private static class ModeStats {
        int wins = 0;
        int rounds = 0;
        int kills = 0;
        int deaths = 0;
        int stuns = 0;
        int stunsRec = 0;
        int score = 0;
        int time = 0;
    }
}
