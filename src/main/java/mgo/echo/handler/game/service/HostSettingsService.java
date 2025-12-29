package mgo.echo.handler.game.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterHostSettings;
import mgo.echo.data.entity.Lobby;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.game.dto.HostSettingsDto;

/**
 * Service for host settings business logic.
 * Handles settings retrieval and persistence.
 */
public class HostSettingsService {
    private static final String SETTINGS_DEFAULT = "{\"name\":\"{CHARACTER_NAME}\",\"password\":null,\"stance\":0,\"comment\":\"Good luck.\",\"games\":[],\"common\":{\"dedicated\":false,\"maxPlayers\":16,\"briefingTime\":2,\"nonStat\":false,\"friendlyFire\":false,\"autoAim\":true,\"uniques\":{\"enabled\":false,\"random\":false,\"red\":0,\"blue\":2},\"enemyNametags\":true,\"silentMode\":false,\"autoAssign\":true,\"teamsSwitch\":true,\"ghosts\":false,\"levelLimit\":{\"enabled\":false,\"base\":{CHARACTER_EXP},\"tolerance\":0},\"voiceChat\":true,\"teamKillKick\":3,\"idleKick\":5,\"weaponRestrictions\":{\"enabled\":false,\"primary\":{\"vz\":true,\"p90\":true,\"mp5\":true,\"patriot\":true,\"ak\":true,\"m4\":true,\"mk17\":true,\"xm8\":true,\"g3a3\":true,\"svd\":true,\"mosin\":true,\"m14\":true,\"vss\":true,\"dsr\":true,\"m870\":true,\"saiga\":true,\"m60\":true,\"shield\":true,\"rpg\":true,\"knife\":true},\"secondary\":{\"gsr\":true,\"mk2\":true,\"operator\":true,\"g18\":true,\"mk23\":true,\"de\":true},\"support\":{\"grenade\":true,\"wp\":true,\"stun\":true,\"chaff\":true,\"smoke\":true,\"smoke_r\":true,\"smoke_g\":true,\"smoke_y\":true,\"eloc\":true,\"claymore\":true,\"sgmine\":true,\"c4\":true,\"sgsatchel\":true,\"magazine\":true},\"custom\":{\"suppressor\":true,\"gp30\":true,\"xm320\":true,\"masterkey\":true,\"scope\":true,\"sight\":true,\"laser\":true,\"lighthg\":true,\"lightlg\":true,\"grip\":true},\"items\":{\"envg\":true,\"drum\":true}}},\"ruleSettings\":{\"dm\":{\"time\":5,\"rounds\":1,\"tickets\":30},\"tdm\":{\"time\":5,\"rounds\":2,\"tickets\":51},\"res\":{\"time\":7,\"rounds\":2},\"cap\":{\"time\":4,\"rounds\":2,\"extraTime\":false},\"sne\":{\"time\":7,\"rounds\":2,\"snake\":3},\"base\":{\"time\":5,\"rounds\":2},\"bomb\":{\"time\":7,\"rounds\":2},\"tsne\":{\"time\":10,\"rounds\":2},\"sdm\":{\"time\":3,\"rounds\":2},\"int\":{\"time\":20},\"scap\":{\"time\":5,\"rounds\":2,\"extraTime\":true},\"race\":{\"time\":5,\"rounds\":2,\"extraTime\":true}}}";

    /**
     * Get or create host settings for a character in a lobby
     */
    public static HostSettingsDto getOrCreateSettings(User user, Character character, Lobby lobby) {
        List<CharacterHostSettings> settingsList = character.getHostSettings();
        if (settingsList == null) {
            settingsList = new ArrayList<>();
            character.setHostSettings(settingsList);
        }

        CharacterHostSettings hostSettings = settingsList.stream()
                .filter((e) -> e.getType() == lobby.getSubtype())
                .findFirst()
                .orElse(null);

        if (hostSettings == null) {
            hostSettings = createDefaultSettings(user, character, lobby, settingsList);
        }

        return HostSettingsDto.fromJson(hostSettings.getSettings());
    }

    /**
     * Save host settings for a character
     */
    public static void saveSettings(User user, Character character, Lobby lobby, HostSettingsDto settings) {
        String json = settings.toJson();
        user.setSessionHostSettings(json);

        // Don't persist clan room settings
        if (hasClanRoom(settings)) {
            return;
        }

        List<CharacterHostSettings> settingsList = character.getHostSettings();
        if (settingsList == null) {
            settingsList = new ArrayList<>();
            character.setHostSettings(settingsList);
        }

        CharacterHostSettings hostSettings = settingsList.stream()
                .filter((e) -> e.getType() == lobby.getSubtype())
                .findFirst()
                .orElse(null);

        if (hostSettings == null) {
            hostSettings = new CharacterHostSettings();
            hostSettings.setCharacter(character);
            hostSettings.setType(lobby.getSubtype());
            settingsList.add(hostSettings);
        }

        hostSettings.setSettings(json);

        CharacterHostSettings finalSettings = hostSettings;
        DbManager.txVoid(session -> session.saveOrUpdate(finalSettings));
    }

    /**
     * Check if settings contain a clan room game
     */
    public static boolean hasClanRoom(HostSettingsDto settings) {
        for (int i = 0; i < settings.games.size(); i++) {
            JsonArray game = settings.games.get(i).getAsJsonArray();
            int rule = game.get(0).getAsInt();
            if (rule == 13) { // Clan room rule
                return true;
            }
        }

        return false;
    }

    /**
     * Get current experience for a user's character
     */
    public static int getCurrentExperience(User user, Character character) {
        if (user.getMainCharacterId() != null && character.getId().equals(user.getMainCharacterId())) {
            return user.getMainExp();
        }

        return user.getAltExp();
    }

    /**
     * Create default host settings for a new host
     */
    private static CharacterHostSettings createDefaultSettings(User user, Character character,
            Lobby lobby, List<CharacterHostSettings> settingsList) {
        int exp = getCurrentExperience(user, character);

        String settingsStr = SETTINGS_DEFAULT.replaceFirst(Pattern.quote("{CHARACTER_NAME}"),
                character.getName());
        settingsStr = settingsStr.replaceFirst(Pattern.quote("{CHARACTER_EXP}"), exp + "");

        CharacterHostSettings hostSettings = new CharacterHostSettings();
        hostSettings.setCharacter(character);
        hostSettings.setType(lobby.getSubtype());
        hostSettings.setSettings(settingsStr);

        settingsList.add(hostSettings);
        return hostSettings;
    }
}
