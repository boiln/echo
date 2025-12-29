package mgo.echo.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mgo2_characters_stats")
public class CharacterStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    private Integer id;

    @Column(name = "chara", nullable = false, insertable = false, updatable = false)
    private Integer charaId;

    @JoinColumn(name = "chara")
    @OneToOne(fetch = FetchType.LAZY)
    private Character character;

    // Core Statistics
    @Column(nullable = false)
    private Integer kills = 0;

    @Column(nullable = false)
    private Integer deaths = 0;

    @Column(nullable = false)
    private Integer wins = 0;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(nullable = false)
    private Integer rounds = 0;

    // Stun Statistics
    @Column(nullable = false)
    private Integer stuns = 0;

    @Column(name = "stuns_received", nullable = false)
    private Integer stunsReceived = 0;

    @Column(name = "stuns_friendly", nullable = false)
    private Integer stunsFriendly = 0;

    // Headshot Statistics
    @Column(name = "headshot_kills", nullable = false)
    private Integer headshotKills = 0;

    @Column(name = "headshot_deaths", nullable = false)
    private Integer headshotDeaths = 0;

    @Column(name = "headshot_stuns", nullable = false)
    private Integer headshotStuns = 0;

    @Column(name = "headshot_stuns_received", nullable = false)
    private Integer headshotStunsReceived = 0;

    // Lock-on Statistics
    @Column(name = "lock_kills", nullable = false)
    private Integer lockKills = 0;

    @Column(name = "lock_deaths", nullable = false)
    private Integer lockDeaths = 0;

    @Column(name = "lock_stuns", nullable = false)
    private Integer lockStuns = 0;

    @Column(name = "lock_stuns_received", nullable = false)
    private Integer lockStunsReceived = 0;

    // Consecutive/Streak Statistics
    @Column(name = "consecutive_kills", nullable = false)
    private Integer consecutiveKills = 0;

    @Column(name = "consecutive_deaths", nullable = false)
    private Integer consecutiveDeaths = 0;

    @Column(name = "consecutive_headshots", nullable = false)
    private Integer consecutiveHeadshots = 0;

    @Column(name = "consecutive_tdm", nullable = false)
    private Integer consecutiveTdm = 0;

    // Spotting
    @Column(nullable = false)
    private Integer spotted = 0;

    @Column(name = "self_spotted", nullable = false)
    private Integer selfSpotted = 0;

    @Column(name = "snake_spotted", nullable = false)
    private Integer snakeSpotted = 0;

    @Column(name = "snake_self_spotted", nullable = false)
    private Integer snakeSelfSpotted = 0;

    // Misc Combat
    @Column(nullable = false)
    private Integer suicides = 0;

    @Column(nullable = false)
    private Integer salutes = 0;

    @Column(nullable = false)
    private Integer radio = 0;

    @Column(nullable = false)
    private Integer chat = 0;

    @Column(name = "cqc_given", nullable = false)
    private Integer cqcGiven = 0;

    @Column(name = "cqc_taken", nullable = false)
    private Integer cqcTaken = 0;

    @Column(nullable = false)
    private Integer rolls = 0;

    @Column(nullable = false)
    private Integer catapult = 0;

    @Column(nullable = false)
    private Integer falls = 0;

    @Column(nullable = false)
    private Integer trapped = 0;

    @Column(nullable = false)
    private Integer melee = 0;

    @Column(name = "melee_rec", nullable = false)
    private Integer meleeRec = 0;

    // Box Usage
    @Column(name = "box_time", nullable = false)
    private Integer boxTime = 0;

    @Column(name = "box_uses", nullable = false)
    private Integer boxUses = 0;

    // Game Mode Specific - Base/Capture
    @Column(name = "bases_captured", nullable = false)
    private Integer basesCaptured = 0;

    @Column(name = "bases_destroyed", nullable = false)
    private Integer basesDestroyed = 0;

    @Column(name = "sop_destab", nullable = false)
    private Integer sopDestab = 0;

    // Game Mode Specific - Rescue
    @Column(name = "gako_saved", nullable = false)
    private Integer gakoSaved = 0;

    @Column(name = "gako_defended", nullable = false)
    private Integer gakoDefended = 0;

    @Column(name = "gako_first", nullable = false)
    private Integer gakoFirst = 0;

    @Column(name = "res_defend", nullable = false)
    private Integer resDefend = 0;

    @Column(name = "res_gako_time", nullable = false)
    private Integer resGakoTime = 0;

    @Column(name = "res_first_grab", nullable = false)
    private Integer resFirstGrab = 0;

    // Game Mode Specific - Bomb
    @Column(name = "bomb_disarms", nullable = false)
    private Integer bombDisarms = 0;

    // Game Mode Specific - SDM
    @Column(name = "sdm_survivals", nullable = false)
    private Integer sdmSurvivals = 0;

    // Game Mode Specific - Race
    @Column(name = "race_checkpoints", nullable = false)
    private Integer raceCheckpoints = 0;

    // Game Mode Specific - Snake/TSNE
    @Column(name = "wins_snake", nullable = false)
    private Integer winsSnake = 0;

    @Column(name = "kills_snake", nullable = false)
    private Integer killsSnake = 0;

    @Column(name = "snake_holdups", nullable = false)
    private Integer snakeHoldups = 0;

    @Column(name = "snake_tags_spawned", nullable = false)
    private Integer snakeTagsSpawned = 0;

    @Column(name = "snake_tags_taken", nullable = false)
    private Integer snakeTagsTaken = 0;

    @Column(name = "snake_injured", nullable = false)
    private Integer snakeInjured = 0;

    @Column(name = "tsne_grab1", nullable = false)
    private Integer tsneGrab1 = 0;

    @Column(name = "tsne_grab2", nullable = false)
    private Integer tsneGrab2 = 0;

    // Knife
    @Column(name = "knife_kills", nullable = false)
    private Integer knifeKills = 0;

    @Column(name = "knife_stuns", nullable = false)
    private Integer knifeStuns = 0;

    // Equipment/Skills
    @Column(nullable = false)
    private Integer boosts = 0;

    @Column(nullable = false)
    private Integer scans = 0;

    @Column(name = "evg_time", nullable = false)
    private Integer evgTime = 0;

    @Column(nullable = false)
    private Integer wakeups = 0;

    // Team/Misc
    @Column(name = "team_kills", nullable = false)
    private Integer teamKills = 0;

    @Column(nullable = false)
    private Integer withdrawals = 0;

    // Points breakdown
    @Column(name = "points_assist", nullable = false)
    private Integer pointsAssist = 0;

    @Column(name = "points_base", nullable = false)
    private Integer pointsBase = 0;

    // Training
    @Column(name = "trained_soldiers", nullable = false)
    private Integer trainedSoldiers = 0;

    @Column(name = "time_training", nullable = false)
    private Integer timeTraining = 0;

    @Column(name = "time_instructor", nullable = false)
    private Integer timeInstructor = 0;

    @Column(name = "time_student", nullable = false)
    private Integer timeStudent = 0;

    // Time Statistics (in seconds)
    @Column(nullable = false)
    private Integer time = 0;

    @Column(name = "time_snake", nullable = false)
    private Integer timeSnake = 0;

    @Column(name = "time_dedi", nullable = false)
    private Integer timeDedi = 0;

    // Per-Game Mode Stats (JSON format)
    @Column(name = "stats_dm", columnDefinition = "TEXT")
    private String statsDm;

    @Column(name = "stats_tdm", columnDefinition = "TEXT")
    private String statsTdm;

    @Column(name = "stats_res", columnDefinition = "TEXT")
    private String statsRes;

    @Column(name = "stats_cap", columnDefinition = "TEXT")
    private String statsCap;

    @Column(name = "stats_base", columnDefinition = "TEXT")
    private String statsBase;

    @Column(name = "stats_bomb", columnDefinition = "TEXT")
    private String statsBomb;

    @Column(name = "stats_sne", columnDefinition = "TEXT")
    private String statsSne;

    @Column(name = "stats_tsne", columnDefinition = "TEXT")
    private String statsTsne;

    @Column(name = "stats_sdm", columnDefinition = "TEXT")
    private String statsSdm;

    @Column(name = "stats_int", columnDefinition = "TEXT")
    private String statsInt;

    @Column(name = "stats_scap", columnDefinition = "TEXT")
    private String statsScap;

    @Column(name = "stats_race", columnDefinition = "TEXT")
    private String statsRace;

    // Unknown/Reserved fields
    @Column(nullable = false)
    private Integer unk1 = 0;

    @Column(nullable = false)
    private Integer unk2 = 0;

    @Column(nullable = false)
    private Integer unk3 = 0;

    // Metadata
    @Column(name = "last_updated")
    private Integer lastUpdated;

    public CharacterStats() {
        // Initialize default JSON stats
        String defaultStats = "{\"wins\":0,\"rounds\":0,\"score\":0,\"time\":0,\"kills\":0,\"deaths\":0,\"stuns\":0,\"stunsRec\":0,\"hsKills\":0,\"hsDeaths\":0,\"hsStuns\":0,\"hsStunsRec\":0,\"lockKills\":0,\"lockDeaths\":0,\"lockStuns\":0,\"lockStunsRec\":0}";
        this.statsDm = defaultStats;
        this.statsTdm = defaultStats;
        this.statsRes = defaultStats;
        this.statsCap = defaultStats;
        this.statsBase = defaultStats;
        this.statsBomb = defaultStats;
        this.statsSne = defaultStats;
        this.statsTsne = defaultStats;
        this.statsSdm = defaultStats;
        this.statsInt = defaultStats;
        this.statsScap = defaultStats;
        this.statsRace = defaultStats;
    }

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCharaId() {
        return charaId;
    }

    public void setCharaId(Integer charaId) {
        this.charaId = charaId;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public Integer getKills() {
        return kills;
    }

    public void setKills(Integer kills) {
        this.kills = kills;
    }

    public Integer getDeaths() {
        return deaths;
    }

    public void setDeaths(Integer deaths) {
        this.deaths = deaths;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getRounds() {
        return rounds;
    }

    public void setRounds(Integer rounds) {
        this.rounds = rounds;
    }

    public Integer getStuns() {
        return stuns;
    }

    public void setStuns(Integer stuns) {
        this.stuns = stuns;
    }

    public Integer getStunsReceived() {
        return stunsReceived;
    }

    public void setStunsReceived(Integer stunsReceived) {
        this.stunsReceived = stunsReceived;
    }

    public Integer getStunsFriendly() {
        return stunsFriendly;
    }

    public void setStunsFriendly(Integer stunsFriendly) {
        this.stunsFriendly = stunsFriendly;
    }

    public Integer getHeadshotKills() {
        return headshotKills;
    }

    public void setHeadshotKills(Integer headshotKills) {
        this.headshotKills = headshotKills;
    }

    public Integer getHeadshotDeaths() {
        return headshotDeaths;
    }

    public void setHeadshotDeaths(Integer headshotDeaths) {
        this.headshotDeaths = headshotDeaths;
    }

    public Integer getHeadshotStuns() {
        return headshotStuns;
    }

    public void setHeadshotStuns(Integer headshotStuns) {
        this.headshotStuns = headshotStuns;
    }

    public Integer getHeadshotStunsReceived() {
        return headshotStunsReceived;
    }

    public void setHeadshotStunsReceived(Integer headshotStunsReceived) {
        this.headshotStunsReceived = headshotStunsReceived;
    }

    public Integer getLockKills() {
        return lockKills;
    }

    public void setLockKills(Integer lockKills) {
        this.lockKills = lockKills;
    }

    public Integer getLockDeaths() {
        return lockDeaths;
    }

    public void setLockDeaths(Integer lockDeaths) {
        this.lockDeaths = lockDeaths;
    }

    public Integer getLockStuns() {
        return lockStuns;
    }

    public void setLockStuns(Integer lockStuns) {
        this.lockStuns = lockStuns;
    }

    public Integer getLockStunsReceived() {
        return lockStunsReceived;
    }

    public void setLockStunsReceived(Integer lockStunsReceived) {
        this.lockStunsReceived = lockStunsReceived;
    }

    public Integer getConsecutiveKills() {
        return consecutiveKills;
    }

    public void setConsecutiveKills(Integer consecutiveKills) {
        this.consecutiveKills = consecutiveKills;
    }

    public Integer getConsecutiveDeaths() {
        return consecutiveDeaths;
    }

    public void setConsecutiveDeaths(Integer consecutiveDeaths) {
        this.consecutiveDeaths = consecutiveDeaths;
    }

    public Integer getConsecutiveHeadshots() {
        return consecutiveHeadshots;
    }

    public void setConsecutiveHeadshots(Integer consecutiveHeadshots) {
        this.consecutiveHeadshots = consecutiveHeadshots;
    }

    public Integer getConsecutiveTdm() {
        return consecutiveTdm;
    }

    public void setConsecutiveTdm(Integer consecutiveTdm) {
        this.consecutiveTdm = consecutiveTdm;
    }

    public Integer getSpotted() {
        return spotted;
    }

    public void setSpotted(Integer spotted) {
        this.spotted = spotted;
    }

    public Integer getSelfSpotted() {
        return selfSpotted;
    }

    public void setSelfSpotted(Integer selfSpotted) {
        this.selfSpotted = selfSpotted;
    }

    public Integer getSnakeSpotted() {
        return snakeSpotted;
    }

    public void setSnakeSpotted(Integer snakeSpotted) {
        this.snakeSpotted = snakeSpotted;
    }

    public Integer getSnakeSelfSpotted() {
        return snakeSelfSpotted;
    }

    public void setSnakeSelfSpotted(Integer snakeSelfSpotted) {
        this.snakeSelfSpotted = snakeSelfSpotted;
    }

    public Integer getSuicides() {
        return suicides;
    }

    public void setSuicides(Integer suicides) {
        this.suicides = suicides;
    }

    public Integer getSalutes() {
        return salutes;
    }

    public void setSalutes(Integer salutes) {
        this.salutes = salutes;
    }

    public Integer getRadio() {
        return radio;
    }

    public void setRadio(Integer radio) {
        this.radio = radio;
    }

    public Integer getChat() {
        return chat;
    }

    public void setChat(Integer chat) {
        this.chat = chat;
    }

    public Integer getCqcGiven() {
        return cqcGiven;
    }

    public void setCqcGiven(Integer cqcGiven) {
        this.cqcGiven = cqcGiven;
    }

    public Integer getCqcTaken() {
        return cqcTaken;
    }

    public void setCqcTaken(Integer cqcTaken) {
        this.cqcTaken = cqcTaken;
    }

    public Integer getRolls() {
        return rolls;
    }

    public void setRolls(Integer rolls) {
        this.rolls = rolls;
    }

    public Integer getCatapult() {
        return catapult;
    }

    public void setCatapult(Integer catapult) {
        this.catapult = catapult;
    }

    public Integer getFalls() {
        return falls;
    }

    public void setFalls(Integer falls) {
        this.falls = falls;
    }

    public Integer getTrapped() {
        return trapped;
    }

    public void setTrapped(Integer trapped) {
        this.trapped = trapped;
    }

    public Integer getMelee() {
        return melee;
    }

    public void setMelee(Integer melee) {
        this.melee = melee;
    }

    public Integer getMeleeRec() {
        return meleeRec;
    }

    public void setMeleeRec(Integer meleeRec) {
        this.meleeRec = meleeRec;
    }

    public Integer getBoxTime() {
        return boxTime;
    }

    public void setBoxTime(Integer boxTime) {
        this.boxTime = boxTime;
    }

    public Integer getBoxUses() {
        return boxUses;
    }

    public void setBoxUses(Integer boxUses) {
        this.boxUses = boxUses;
    }

    public Integer getBasesCaptured() {
        return basesCaptured;
    }

    public void setBasesCaptured(Integer basesCaptured) {
        this.basesCaptured = basesCaptured;
    }

    public Integer getBasesDestroyed() {
        return basesDestroyed;
    }

    public void setBasesDestroyed(Integer basesDestroyed) {
        this.basesDestroyed = basesDestroyed;
    }

    public Integer getSopDestab() {
        return sopDestab;
    }

    public void setSopDestab(Integer sopDestab) {
        this.sopDestab = sopDestab;
    }

    public Integer getGakoSaved() {
        return gakoSaved;
    }

    public void setGakoSaved(Integer gakoSaved) {
        this.gakoSaved = gakoSaved;
    }

    public Integer getGakoDefended() {
        return gakoDefended;
    }

    public void setGakoDefended(Integer gakoDefended) {
        this.gakoDefended = gakoDefended;
    }

    public Integer getGakoFirst() {
        return gakoFirst;
    }

    public void setGakoFirst(Integer gakoFirst) {
        this.gakoFirst = gakoFirst;
    }

    public Integer getResDefend() {
        return resDefend;
    }

    public void setResDefend(Integer resDefend) {
        this.resDefend = resDefend;
    }

    public Integer getResGakoTime() {
        return resGakoTime;
    }

    public void setResGakoTime(Integer resGakoTime) {
        this.resGakoTime = resGakoTime;
    }

    public Integer getResFirstGrab() {
        return resFirstGrab;
    }

    public void setResFirstGrab(Integer resFirstGrab) {
        this.resFirstGrab = resFirstGrab;
    }

    public Integer getBombDisarms() {
        return bombDisarms;
    }

    public void setBombDisarms(Integer bombDisarms) {
        this.bombDisarms = bombDisarms;
    }

    public Integer getSdmSurvivals() {
        return sdmSurvivals;
    }

    public void setSdmSurvivals(Integer sdmSurvivals) {
        this.sdmSurvivals = sdmSurvivals;
    }

    public Integer getRaceCheckpoints() {
        return raceCheckpoints;
    }

    public void setRaceCheckpoints(Integer raceCheckpoints) {
        this.raceCheckpoints = raceCheckpoints;
    }

    public Integer getWinsSnake() {
        return winsSnake;
    }

    public void setWinsSnake(Integer winsSnake) {
        this.winsSnake = winsSnake;
    }

    public Integer getKillsSnake() {
        return killsSnake;
    }

    public void setKillsSnake(Integer killsSnake) {
        this.killsSnake = killsSnake;
    }

    public Integer getSnakeHoldups() {
        return snakeHoldups;
    }

    public void setSnakeHoldups(Integer snakeHoldups) {
        this.snakeHoldups = snakeHoldups;
    }

    public Integer getSnakeTagsSpawned() {
        return snakeTagsSpawned;
    }

    public void setSnakeTagsSpawned(Integer snakeTagsSpawned) {
        this.snakeTagsSpawned = snakeTagsSpawned;
    }

    public Integer getSnakeTagsTaken() {
        return snakeTagsTaken;
    }

    public void setSnakeTagsTaken(Integer snakeTagsTaken) {
        this.snakeTagsTaken = snakeTagsTaken;
    }

    public Integer getSnakeInjured() {
        return snakeInjured;
    }

    public void setSnakeInjured(Integer snakeInjured) {
        this.snakeInjured = snakeInjured;
    }

    public Integer getTsneGrab1() {
        return tsneGrab1;
    }

    public void setTsneGrab1(Integer tsneGrab1) {
        this.tsneGrab1 = tsneGrab1;
    }

    public Integer getTsneGrab2() {
        return tsneGrab2;
    }

    public void setTsneGrab2(Integer tsneGrab2) {
        this.tsneGrab2 = tsneGrab2;
    }

    public Integer getKnifeKills() {
        return knifeKills;
    }

    public void setKnifeKills(Integer knifeKills) {
        this.knifeKills = knifeKills;
    }

    public Integer getKnifeStuns() {
        return knifeStuns;
    }

    public void setKnifeStuns(Integer knifeStuns) {
        this.knifeStuns = knifeStuns;
    }

    public Integer getBoosts() {
        return boosts;
    }

    public void setBoosts(Integer boosts) {
        this.boosts = boosts;
    }

    public Integer getScans() {
        return scans;
    }

    public void setScans(Integer scans) {
        this.scans = scans;
    }

    public Integer getEvgTime() {
        return evgTime;
    }

    public void setEvgTime(Integer evgTime) {
        this.evgTime = evgTime;
    }

    public Integer getWakeups() {
        return wakeups;
    }

    public void setWakeups(Integer wakeups) {
        this.wakeups = wakeups;
    }

    public Integer getTeamKills() {
        return teamKills;
    }

    public void setTeamKills(Integer teamKills) {
        this.teamKills = teamKills;
    }

    public Integer getWithdrawals() {
        return withdrawals;
    }

    public void setWithdrawals(Integer withdrawals) {
        this.withdrawals = withdrawals;
    }

    public Integer getPointsAssist() {
        return pointsAssist;
    }

    public void setPointsAssist(Integer pointsAssist) {
        this.pointsAssist = pointsAssist;
    }

    public Integer getPointsBase() {
        return pointsBase;
    }

    public void setPointsBase(Integer pointsBase) {
        this.pointsBase = pointsBase;
    }

    public Integer getTrainedSoldiers() {
        return trainedSoldiers;
    }

    public void setTrainedSoldiers(Integer trainedSoldiers) {
        this.trainedSoldiers = trainedSoldiers;
    }

    public Integer getTimeTraining() {
        return timeTraining;
    }

    public void setTimeTraining(Integer timeTraining) {
        this.timeTraining = timeTraining;
    }

    public Integer getTimeInstructor() {
        return timeInstructor;
    }

    public void setTimeInstructor(Integer timeInstructor) {
        this.timeInstructor = timeInstructor;
    }

    public Integer getTimeStudent() {
        return timeStudent;
    }

    public void setTimeStudent(Integer timeStudent) {
        this.timeStudent = timeStudent;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Integer getTimeSnake() {
        return timeSnake;
    }

    public void setTimeSnake(Integer timeSnake) {
        this.timeSnake = timeSnake;
    }

    public Integer getTimeDedi() {
        return timeDedi;
    }

    public void setTimeDedi(Integer timeDedi) {
        this.timeDedi = timeDedi;
    }

    public String getStatsDm() {
        return statsDm;
    }

    public void setStatsDm(String statsDm) {
        this.statsDm = statsDm;
    }

    public String getStatsTdm() {
        return statsTdm;
    }

    public void setStatsTdm(String statsTdm) {
        this.statsTdm = statsTdm;
    }

    public String getStatsRes() {
        return statsRes;
    }

    public void setStatsRes(String statsRes) {
        this.statsRes = statsRes;
    }

    public String getStatsCap() {
        return statsCap;
    }

    public void setStatsCap(String statsCap) {
        this.statsCap = statsCap;
    }

    public String getStatsBase() {
        return statsBase;
    }

    public void setStatsBase(String statsBase) {
        this.statsBase = statsBase;
    }

    public String getStatsBomb() {
        return statsBomb;
    }

    public void setStatsBomb(String statsBomb) {
        this.statsBomb = statsBomb;
    }

    public String getStatsSne() {
        return statsSne;
    }

    public void setStatsSne(String statsSne) {
        this.statsSne = statsSne;
    }

    public String getStatsTsne() {
        return statsTsne;
    }

    public void setStatsTsne(String statsTsne) {
        this.statsTsne = statsTsne;
    }

    public String getStatsSdm() {
        return statsSdm;
    }

    public void setStatsSdm(String statsSdm) {
        this.statsSdm = statsSdm;
    }

    public String getStatsInt() {
        return statsInt;
    }

    public void setStatsInt(String statsInt) {
        this.statsInt = statsInt;
    }

    public String getStatsScap() {
        return statsScap;
    }

    public void setStatsScap(String statsScap) {
        this.statsScap = statsScap;
    }

    public String getStatsRace() {
        return statsRace;
    }

    public void setStatsRace(String statsRace) {
        this.statsRace = statsRace;
    }

    public Integer getUnk1() {
        return unk1;
    }

    public void setUnk1(Integer unk1) {
        this.unk1 = unk1;
    }

    public Integer getUnk2() {
        return unk2;
    }

    public void setUnk2(Integer unk2) {
        this.unk2 = unk2;
    }

    public Integer getUnk3() {
        return unk3;
    }

    public void setUnk3(Integer unk3) {
        this.unk3 = unk3;
    }

    public Integer getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Integer lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Helper methods for adding stats (accumulative updates)
    public void addKills(int value) {
        this.kills += value;
    }

    public void addDeaths(int value) {
        this.deaths += value;
    }

    public void addWins(int value) {
        this.wins += value;
    }

    public void addScore(int value) {
        this.score += value;
    }

    public void addRounds(int value) {
        this.rounds += value;
    }

    public void addStuns(int value) {
        this.stuns += value;
    }

    public void addStunsReceived(int value) {
        this.stunsReceived += value;
    }

    public void addHeadshotKills(int value) {
        this.headshotKills += value;
    }

    public void addHeadshotDeaths(int value) {
        this.headshotDeaths += value;
    }

    public void addTime(int value) {
        this.time += value;
    }

    public void updateConsecutiveKills(int value) {
        if (value > this.consecutiveKills) {
            this.consecutiveKills = value;
        }
    }

    public void updateConsecutiveDeaths(int value) {
        if (value > this.consecutiveDeaths) {
            this.consecutiveDeaths = value;
        }
    }

    public void updateConsecutiveHeadshots(int value) {
        if (value > this.consecutiveHeadshots) {
            this.consecutiveHeadshots = value;
        }
    }

    /**
     * Get the stats JSON string for a specific game mode
     * Game modes: 0=DM, 1=TDM, 2=SNE, 3=CAP, 4=BASE, 5=BOMB, 6=RES, 7=RACE, 8=TSNE,
     * 9=SDM, 10=SCAP
     */
    public String getStatsByMode(int mode) {
        switch (mode) {
            case 0:
                return statsDm;
            case 1:
                return statsTdm;
            case 2:
                return statsSne;
            case 3:
                return statsCap;
            case 4:
                return statsBase;
            case 5:
                return statsBomb;
            case 6:
                return statsRes;
            case 7:
                return statsRace;
            case 8:
                return statsTsne;
            case 9:
                return statsSdm;
            case 10:
                return statsScap;
            default:
                return statsDm;
        }
    }

    /**
     * Set the stats JSON string for a specific game mode
     */
    public void setStatsByMode(int mode, String json) {
        switch (mode) {
            case 0:
                statsDm = json;
                break;
            case 1:
                statsTdm = json;
                break;
            case 2:
                statsSne = json;
                break;
            case 3:
                statsCap = json;
                break;
            case 4:
                statsBase = json;
                break;
            case 5:
                statsBomb = json;
                break;
            case 6:
                statsRes = json;
                break;
            case 7:
                statsRace = json;
                break;
            case 8:
                statsTsne = json;
                break;
            case 9:
                statsSdm = json;
                break;
            case 10:
                statsScap = json;
                break;
        }
    }
}
