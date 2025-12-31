package mgo.echo.controller;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterBlocked;
import mgo.echo.data.entity.CharacterChatMacro;
import mgo.echo.data.entity.CharacterFriend;
import mgo.echo.data.entity.CharacterSetGear;
import mgo.echo.data.entity.CharacterSetSkills;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.handler.character.dto.GameplayOptionsDto;
import mgo.echo.handler.character.packet.CharacterCardPacket;
import mgo.echo.handler.character.packet.CharacterInfoPacket;
import mgo.echo.handler.character.packet.ChatMacrosPacket;
import mgo.echo.handler.character.packet.FriendsBlockedPacket;
import mgo.echo.handler.character.packet.GameplayOptionsPacket;
import mgo.echo.handler.character.packet.GearPacket;
import mgo.echo.handler.character.packet.PersonalInfoPacket;
import mgo.echo.handler.character.packet.PostGameInfoPacket;
import mgo.echo.handler.character.packet.SetsPacket;
import mgo.echo.handler.character.packet.SkillsPacket;
import mgo.echo.handler.character.packet.StatsPacket;
import mgo.echo.handler.character.service.CharacterService;
import mgo.echo.handler.character.service.FriendsService;
import mgo.echo.protocol.command.CharactersCmd;
import mgo.echo.protocol.dispatch.Command;
import mgo.echo.protocol.dispatch.CommandContext;
import mgo.echo.protocol.dispatch.Controller;
import mgo.echo.util.Error;
import mgo.echo.util.Packets;
import mgo.echo.util.Util;

/**
 * Character data operations controller.
 */
public class CharacterController implements Controller {
    private static final Logger logger = LogManager.getLogger(CharacterController.class);

    // ========================================================================
    // Character Info (0x4100)
    // ========================================================================

    @Command(0x4100)
    public boolean getCharacterInfo(CommandContext ctx) {
        User user = ctx.user();
        if (user == null) {
            logger.error("Error while getting character info: No User.");
            ctx.write(CharactersCmd.GET_CHARACTER_INFO_RESPONSE, Error.INVALID_SESSION);
            return true;
        }

        Character character = user.getCurrentCharacter();
        CharacterInfoPacket.write(ctx.nettyCtx(), user, character);
        sendGameplayOptions(ctx, character);
        sendChatMacros(ctx, character);
        sendPersonalInfo(ctx, character);
        sendGear(ctx);
        sendSkills(ctx);
        sendSkillSets(ctx, character);
        sendGearSets(ctx, character);

        return true;
    }

    private void sendGameplayOptions(CommandContext ctx, Character character) {
        GameplayOptionsPacket.write(ctx.nettyCtx(), character);
    }

    private void sendChatMacros(CommandContext ctx, Character character) {
        List<CharacterChatMacro> macros = character.getChatMacros();
        if (macros.size() <= 0) {
            macros = ChatMacrosPacket.initializeMacros(character);
        }

        ChatMacrosPacket.write(ctx.nettyCtx(), macros);
    }

    private void sendPersonalInfo(CommandContext ctx, Character character) {
        PersonalInfoPacket.write(ctx.nettyCtx(), character);
    }

    private void sendGear(CommandContext ctx) {
        GearPacket.write(ctx.nettyCtx());
    }

    private void sendSkills(CommandContext ctx) {
        SkillsPacket.write(ctx.nettyCtx());
    }

    private void sendSkillSets(CommandContext ctx, Character character) {
        List<CharacterSetSkills> sets = character.getSetsSkills();
        if (sets.size() <= 0) {
            sets = SetsPacket.initializeSkillSets(character);
        }

        SetsPacket.writeSkillSets(ctx.nettyCtx(), sets);
    }

    private void sendGearSets(CommandContext ctx, Character character) {
        List<CharacterSetGear> sets = character.getSetsGear();
        if (sets.size() <= 0) {
            sets = SetsPacket.initializeGearSets(character);
        }

        SetsPacket.writeGearSets(ctx.nettyCtx(), sets);
    }

    // ========================================================================
    // Settings Updates (0x4110, 0x4112, 0x4114)
    // ========================================================================

    @Command(0x4110)
    public boolean updateGameplayOptions(CommandContext ctx) {
        User user = ctx.user();
        if (user == null) {
            logger.error("Error while updating gameplay options: No User.");
            ctx.write(CharactersCmd.UPDATE_SETTINGS_RESPONSE, Error.INVALID_SESSION);
            return true;
        }

        Character character = user.getCurrentCharacter();
        GameplayOptionsDto dto = GameplayOptionsPacket.read(ctx.packet());
        GameplayOptionsPacket.saveToDb(character, dto);
        ctx.write(CharactersCmd.UPDATE_SETTINGS_RESPONSE, 0);

        return true;
    }

    @Command(0x4112)
    public boolean updateUiSettings(CommandContext ctx) {
        ctx.write(CharactersCmd.UPDATE_UI_SETTINGS_RESPONSE, 0);
        return true;
    }

    @Command(0x4114)
    public boolean updateChatMacros(CommandContext ctx) {
        User user = ctx.user();
        if (user == null) {
            logger.error("Error while updating chat macros: No User.");
            ctx.write(CharactersCmd.UPDATE_SETTINGS_RESPONSE, Error.INVALID_SESSION);
            return true;
        }

        Character character = user.getCurrentCharacter();
        List<CharacterChatMacro> macros = character.getChatMacros();

        String[] texts = new String[12];
        int type = ChatMacrosPacket.readUpdate(ctx.packet(), texts);

        for (int i = 0; i < 12; i++) {
            final int index = i;
            CharacterChatMacro macro = macros.stream()
                    .filter((e) -> e.getIndex() == index && e.getType() == type)
                    .findAny().orElse(null);
            macro.setText(texts[i]);
        }

        DbManager.txVoid(session -> {
            for (CharacterChatMacro macro : macros) {
                session.saveOrUpdate(macro);
            }
        });

        if (type == 1) {
            ctx.write(CharactersCmd.UPDATE_SETTINGS_RESPONSE, 0);
        }

        return true;
    }

    // ========================================================================
    // Personal Info (0x4130)
    // ========================================================================

    @Command(0x4130)
    public boolean updatePersonalInfo(CommandContext ctx) {
        try {
            User user = ctx.user();
            if (user == null) {
                logger.error("Error while updating personal info: No User.");
                ctx.write(CharactersCmd.UPDATE_PERSONAL_INFO_RESPONSE, Error.INVALID_SESSION);
                return true;
            }

            Character character = user.getCurrentCharacter();
            CharacterService.PersonalInfoUpdate update = PersonalInfoPacket.readUpdate(ctx.packet());
            CharacterService.updatePersonalInfo(character, update);
            PersonalInfoPacket.writeUpdateResponse(ctx.nettyCtx(), update);
        } catch (Exception e) {
            logger.error("Exception while updating personal info.", e);
            ctx.write(CharactersCmd.UPDATE_PERSONAL_INFO_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Skill/Gear Sets (0x4141, 0x4143)
    // ========================================================================

    @Command(0x4141)
    public boolean updateSkillSets(CommandContext ctx) {
        User user = ctx.user();
        if (user == null) {
            logger.error("Error while updating skill sets: No User.");
            ctx.write(CharactersCmd.UPDATE_SKILL_SETS_RESPONSE, Error.INVALID_SESSION);
            return true;
        }

        Character character = user.getCurrentCharacter();
        List<CharacterSetSkills> sets = character.getSetsSkills();
        SetsPacket.readSkillSetsUpdate(ctx.packet(), sets);

        DbManager.txVoid(session -> {
            for (int i = 0; i < 3; i++) {
                session.saveOrUpdate(sets.get(i));
            }
        });

        ctx.write(CharactersCmd.UPDATE_SKILL_SETS_RESPONSE, 0);
        return true;
    }

    @Command(0x4143)
    public boolean updateGearSets(CommandContext ctx) {
        User user = ctx.user();
        if (user == null) {
            logger.error("Error while updating gear sets: No User.");
            ctx.write(CharactersCmd.UPDATE_GEAR_SETS_RESPONSE, Error.INVALID_SESSION);
            return true;
        }

        Character character = user.getCurrentCharacter();
        List<CharacterSetGear> sets = character.getSetsGear();
        SetsPacket.readGearSetsUpdate(ctx.packet(), sets);

        DbManager.txVoid(session -> {
            for (int i = 0; i < 4; i++) {
                session.saveOrUpdate(sets.get(i));
            }
        });

        ctx.write(CharactersCmd.UPDATE_GEAR_SETS_RESPONSE, 0);
        return true;
    }

    // ========================================================================
    // Post Game Info (0x4128)
    // ========================================================================

    @Command(0x4128)
    public boolean getPostGameInfo(CommandContext ctx) {
        User user = ctx.user();
        if (user == null) {
            logger.error("Error while getting post game info: No User.");
            ctx.write(CharactersCmd.GET_POST_GAME_INFO_RESPONSE, Error.INVALID_SESSION);
            return true;
        }

        Character character = user.getCurrentCharacter();
        PostGameInfoPacket.write(ctx.nettyCtx(), user, character);
        return true;
    }

    // ========================================================================
    // Personal Stats (0x4102)
    // ========================================================================

    @Command(0x4102)
    public boolean getPersonalStats(CommandContext ctx) {
        ByteBuf bi = ctx.payload();
        int targetCharaId = bi.readInt();

        CharacterService.PersonalStatsData data = CharacterService.getPersonalStats(targetCharaId);
        if (data == null) {
            logger.warn("getPersonalStats: Character not found: {}", targetCharaId);
            ctx.write(CharactersCmd.GET_PERSONAL_STATS_HEADER, Error.CHARACTER_DOESNOTEXIST);
            return true;
        }

        StatsPacket.write(ctx.nettyCtx(), targetCharaId, data.character, data.stats, data.playtimeSeconds,
                data.hasClan, data.clanName);

        CharacterService.CharacterCardData card = CharacterService.getCharacterCard(targetCharaId);
        if (card == null) {
            return true;
        }

        CharacterCardPacket.write(ctx.nettyCtx(), card);
        return true;
    }

    // ========================================================================
    // Connection Info (0x4700)
    // ========================================================================

    @Command(0x4700)
    public boolean updateConnectionInfo(CommandContext ctx) {
        try {
            User user = ctx.user();
            if (user == null) {
                logger.error("Error while updating connection info: No User.");
                ctx.write(CharactersCmd.UPDATE_CONNECTION_INFO_RESPONSE, Error.INVALID_SESSION);
                return true;
            }

            Character character = user.getCurrentCharacter();
            ByteBuf bi = ctx.payload();
            int privatePort = bi.readUnsignedShort();
            String privateIp = Util.readString(bi, 16);
            int publicPort = bi.readUnsignedShort();
            bi.skipBytes(2);

            String publicIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
            CharacterService.updateConnectionInfo(character, publicIp, publicPort, privateIp, privatePort);

            ctx.write(CharactersCmd.UPDATE_CONNECTION_INFO_RESPONSE, 0);
        } catch (Exception e) {
            logger.error("Exception while updating connection info.", e);
            ctx.write(CharactersCmd.UPDATE_CONNECTION_INFO_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Friends/Blocked (0x4580, 0x4500, 0x4510)
    // ========================================================================

    @Command(0x4580)
    public boolean getFriendsBlockedList(CommandContext ctx) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();

        try {
            User user = ctx.user();
            if (user == null) {
                ctx.write(CharactersCmd.GET_FRIENDS_BLOCKED_LIST_START, Error.INVALID_SESSION);
                return true;
            }

            Character character = user.getCurrentCharacter();
            ByteBuf bi = ctx.payload();
            int type = bi.readByte();

            DbManager.txVoidOrThrow(session -> {
                try {
                    if (type == 0) {
                        List<CharacterFriend> friends = FriendsService.getFriends(session, character);
                        Packets.handleMutliElementPayload(ctx.nettyCtx(), friends.size(), 23,
                                FriendsBlockedPacket.ENTRY_SIZE, payloads,
                                (i, bo) -> FriendsBlockedPacket.writeEntry(bo, friends.get(i).getTargetId(),
                                        friends.get(i).getTarget()));
                    } else {
                        List<CharacterBlocked> blocked = FriendsService.getBlocked(session, character);
                        Packets.handleMutliElementPayload(ctx.nettyCtx(), blocked.size(), 23,
                                FriendsBlockedPacket.ENTRY_SIZE, payloads,
                                (i, bo) -> FriendsBlockedPacket.writeEntry(bo, blocked.get(i).getTargetId(),
                                        blocked.get(i).getTarget()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Packets.write(ctx.nettyCtx(), CharactersCmd.GET_FRIENDS_BLOCKED_LIST_START, 0);
            Packets.write(ctx.nettyCtx(), CharactersCmd.GET_FRIENDS_BLOCKED_LIST_DATA, payloads);
            Packets.write(ctx.nettyCtx(), CharactersCmd.GET_FRIENDS_BLOCKED_LIST_END, 0);
        } catch (Exception e) {
            logger.error("Exception while getting friends/blocked list.", e);
            ctx.write(CharactersCmd.GET_FRIENDS_BLOCKED_LIST_START, Error.GENERAL);
            Util.releaseBuffers(payloads);
        }

        return true;
    }

    @Command(0x4500)
    public boolean addFriendsBlocked(CommandContext ctx) {
        ByteBuf bo = null;

        try {
            User user = ctx.user();
            if (user == null) {
                ctx.write(CharactersCmd.ADD_FRIENDS_BLOCKED_DATA, Error.INVALID_SESSION);
                return true;
            }

            Character character = user.getCurrentCharacter();
            ByteBuf bi = ctx.payload();
            int type = bi.readByte();
            int targetId = bi.readInt();

            FriendsService.AddResult result = FriendsService.add(type, character, targetId);

            if (result == FriendsService.AddResult.TARGET_NOT_FOUND) {
                logger.error("Error while adding to friends/blocked list: Target not found.");
                ctx.write(CharactersCmd.ADD_FRIENDS_BLOCKED_DATA, Error.CHARACTER_DOESNOTEXIST);
                return true;
            }

            if (result == FriendsService.AddResult.LIST_FULL) {
                logger.error("Error while adding to friends/blocked list: List is full.");
                Packets.writeError(ctx.nettyCtx(), CharactersCmd.ADD_FRIENDS_BLOCKED_DATA, 10);
                return true;
            }

            Character target = FriendsService.getTarget(targetId);
            bo = FriendsBlockedPacket.createAddResponse(ctx.nettyCtx(), target.getId(), type, target.getName());
            Packets.write(ctx.nettyCtx(), CharactersCmd.ADD_FRIENDS_BLOCKED_DATA, bo);
        } catch (Exception e) {
            logger.error("Exception while adding to friends/blocked list.", e);
            Util.safeRelease(bo);
            ctx.write(CharactersCmd.ADD_FRIENDS_BLOCKED_DATA, Error.GENERAL);
        }

        return true;
    }

    @Command(0x4510)
    public boolean removeFriendsBlocked(CommandContext ctx) {
        ByteBuf bo = null;

        try {
            User user = ctx.user();
            if (user == null) {
                ctx.write(CharactersCmd.REMOVE_FRIENDS_BLOCKED_DATA, Error.INVALID_SESSION);
                return true;
            }

            Character character = user.getCurrentCharacter();
            ByteBuf bi = ctx.payload();
            int type = bi.readByte();
            int targetId = bi.readInt();

            boolean removed = FriendsService.remove(type, character, targetId);
            if (!removed) {
                logger.error("Error while removing from friends/blocked list: No entry.");
                Packets.writeError(ctx.nettyCtx(), CharactersCmd.REMOVE_FRIENDS_BLOCKED_DATA, 3);
                return true;
            }

            bo = FriendsBlockedPacket.createRemoveResponse(ctx.nettyCtx(), type, targetId);
            Packets.write(ctx.nettyCtx(), CharactersCmd.REMOVE_FRIENDS_BLOCKED_DATA, bo);
        } catch (Exception e) {
            logger.error("Exception while removing from friends/blocked list.", e);
            ctx.write(CharactersCmd.REMOVE_FRIENDS_BLOCKED_DATA, Error.GENERAL);
            Util.safeRelease(bo);
        }

        return true;
    }

    // ========================================================================
    // Search (0x4600)
    // ========================================================================

    @Command(0x4600)
    public boolean search(CommandContext ctx) {
        AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();

        try {
            ByteBuf bi = ctx.payload();
            boolean exactOnly = bi.readBoolean();
            bi.readBoolean(); // caseSensitive (unused)
            String name = Util.readString(bi, 0x10);

            List<Character> characters = CharacterService.searchCharacters(name, exactOnly);

            int maxEntries = 14;
            int entrySize = FriendsBlockedPacket.SEARCH_ENTRY_SIZE;
            int count = Math.min(characters.size(), maxEntries);

            ByteBuf bo = ctx.nettyCtx().alloc().directBuffer(maxEntries * entrySize);
            for (int i = 0; i < count; i++) {
                FriendsBlockedPacket.writeSearchEntry(bo, characters.get(i));
            }
            bo.writeZero((maxEntries - count) * entrySize);
            payloads.set(new ByteBuf[] { bo });

            Packets.write(ctx.nettyCtx(), CharactersCmd.SEARCH_RESPONSE_START, 0);
            Packets.write(ctx.nettyCtx(), CharactersCmd.SEARCH_RESPONSE_DATA, payloads);
            Packets.write(ctx.nettyCtx(), CharactersCmd.SEARCH_RESPONSE_END, 0);
        } catch (Exception e) {
            logger.error("Exception while searching for player.", e);
            ctx.write(CharactersCmd.SEARCH_RESPONSE_START, Error.GENERAL);
            Util.releaseBuffers(payloads);
        }

        return true;
    }

    // ========================================================================
    // Character Card (0x4220)
    // ========================================================================

    @Command(0x4220)
    public boolean getCharacterCard(CommandContext ctx) {
        try {
            ByteBuf bi = ctx.payload();
            int targetCharaId = bi.readInt();

            CharacterService.CharacterCardData data = CharacterService.getCharacterCard(targetCharaId);
            if (data == null) {
                logger.error("Error while getting character card: Character not found.");
                ctx.write(CharactersCmd.GET_CHARACTER_CARD_RESPONSE, Error.GENERAL);
                return true;
            }

            CharacterCardPacket.write(ctx.nettyCtx(), data);
        } catch (Exception e) {
            logger.error("Exception while getting character card.", e);
            ctx.write(CharactersCmd.GET_CHARACTER_CARD_RESPONSE, Error.GENERAL);
        }

        return true;
    }

    // ========================================================================
    // Match History (0x4680, 0x4684)
    // ========================================================================

    @Command(0x4680)
    public boolean getMatchHistory(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), CharactersCmd.GET_MATCH_HISTORY_START, 0);
        Packets.write(ctx.nettyCtx(), CharactersCmd.GET_MATCH_HISTORY_END, 0);
        return true;
    }

    @Command(0x4684)
    public boolean getOfficialGameHistory(CommandContext ctx) {
        Packets.write(ctx.nettyCtx(), CharactersCmd.GET_OFFICIAL_GAME_HISTORY_START, 0);
        Packets.write(ctx.nettyCtx(), CharactersCmd.GET_OFFICIAL_GAME_HISTORY_END, 0);
        return true;
    }
}
