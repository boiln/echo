package mgo.echo.handler.game.dto;

import com.google.gson.JsonObject;

/** Rule settings parsed from JSON rules field. */
public class RuleSettings {
    public int dmTime;
    public int dmRounds;
    public int dmTickets;
    public int tdmTime;
    public int tdmRounds;
    public int tdmTickets;
    public int resTime;
    public int resRounds;
    public int capTime;
    public int capRounds;
    public boolean capExtraTime;
    public int sneTime;
    public int sneRounds;
    public int sneSnake;
    public int baseTime;
    public int baseRounds;
    public int bombTime;
    public int bombRounds;
    public int tsneTime;
    public int tsneRounds;
    public int sdmTime;
    public int sdmRounds;
    public int intTime;
    public int scapTime;
    public int scapRounds;
    public boolean scapExtraTime;
    public int raceTime;
    public int raceRounds;
    public boolean raceExtraTime;

    public static RuleSettings parse(JsonObject rules) {
        RuleSettings s = new RuleSettings();

        JsonObject dm = rules.get("dm").getAsJsonObject();
        s.dmTime = dm.get("time").getAsInt();
        s.dmRounds = dm.get("rounds").getAsInt();
        s.dmTickets = dm.get("tickets").getAsInt();

        JsonObject tdm = rules.get("tdm").getAsJsonObject();
        s.tdmTime = tdm.get("time").getAsInt();
        s.tdmRounds = tdm.get("rounds").getAsInt();
        s.tdmTickets = tdm.get("tickets").getAsInt();

        JsonObject res = rules.get("res").getAsJsonObject();
        s.resTime = res.get("time").getAsInt();
        s.resRounds = res.get("rounds").getAsInt();

        JsonObject cap = rules.get("cap").getAsJsonObject();
        s.capTime = cap.get("time").getAsInt();
        s.capRounds = cap.get("rounds").getAsInt();
        s.capExtraTime = cap.get("extraTime").getAsBoolean();

        JsonObject sne = rules.get("sne").getAsJsonObject();
        s.sneTime = sne.get("time").getAsInt();
        s.sneRounds = sne.get("rounds").getAsInt();
        s.sneSnake = sne.get("snake").getAsInt();

        JsonObject base = rules.get("base").getAsJsonObject();
        s.baseTime = base.get("time").getAsInt();
        s.baseRounds = base.get("rounds").getAsInt();

        JsonObject bomb = rules.get("bomb").getAsJsonObject();
        s.bombTime = bomb.get("time").getAsInt();
        s.bombRounds = bomb.get("rounds").getAsInt();

        JsonObject tsne = rules.get("tsne").getAsJsonObject();
        s.tsneTime = tsne.get("time").getAsInt();
        s.tsneRounds = tsne.get("rounds").getAsInt();

        JsonObject sdm = rules.get("sdm").getAsJsonObject();
        s.sdmTime = sdm.get("time").getAsInt();
        s.sdmRounds = sdm.get("rounds").getAsInt();

        JsonObject intr = rules.get("int").getAsJsonObject();
        s.intTime = intr.get("time").getAsInt();

        JsonObject scap = rules.get("scap").getAsJsonObject();
        s.scapTime = scap.get("time").getAsInt();
        s.scapRounds = scap.get("rounds").getAsInt();
        s.scapExtraTime = scap.get("extraTime").getAsBoolean();

        JsonObject race = rules.get("race").getAsJsonObject();
        s.raceTime = race.get("time").getAsInt();
        s.raceRounds = race.get("rounds").getAsInt();
        s.raceExtraTime = race.get("extraTime").getAsBoolean();

        return s;
    }

    public int buildExtraTimeFlags() {
        int flags = 0;
        flags |= !scapExtraTime ? 0b1 : 0;
        flags |= !raceExtraTime ? 0b100 : 0;
        return flags;
    }
}
