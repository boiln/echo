package mgo.echo.handler.account.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterAppearance;
import mgo.echo.data.entity.User;
import mgo.echo.data.repository.DbManager;
import mgo.echo.util.Error;
import mgo.echo.util.Util;

/**
 * Character CRUD operations service.
 */
public class CharacterCrudService {
    private static final Logger logger = LogManager.getLogger(CharacterCrudService.class);

    private static final int CHARACTER_DELETION_WAIT_DAYS = 7;
    private static final int CHARACTER_DELETION_WAIT_SECONDS = CHARACTER_DELETION_WAIT_DAYS * 24 * 60 * 60;

    private CharacterCrudService() {
    }

    // ========================================================================
    // Character Listing
    // ========================================================================

    public static List<Character> getCharacters(User user) {
        return DbManager.tx(session -> {
            Query<Character> query = session.createQuery(
                    "from Character as c inner join fetch c.appearance where user=:user and c.active=1",
                    Character.class);
            query.setParameter("user", user);
            return query.list();
        });
    }

    public static List<Character> sortByMain(List<Character> characters, Integer mainCharacterId) {
        if (mainCharacterId == null) {
            return characters;
        }

        for (int i = 0; i < characters.size(); i++) {
            Character character = characters.get(i);
            if (!character.getId().equals(mainCharacterId)) {
                continue;
            }
            characters.remove(i);
            characters.add(0, character);
            break;
        }

        return characters;
    }

    // ========================================================================
    // Character Creation
    // ========================================================================

    public static Integer validateName(String name) {
        if (name.startsWith(":#") || name.startsWith("GM_") || name.startsWith("GM-")
                || name.startsWith("GM.") || name.startsWith("GM,")) {
            logger.error("Error while creating character: Reserved prefix.");
            return Error.CHAR_NAMEPREFIX.getCode();
        }

        if (name.equalsIgnoreCase("EchoMGO")) {
            logger.error("Error while creating character: Reserved name.");
            return Error.CHAR_NAMERESERVED.getCode();
        }

        if (!Util.checkName(name)) {
            logger.error("Error while creating character: Invalid name.");
            return Error.CHAR_NAMEINVALID.getCode();
        }

        return null;
    }

    public static boolean isNameTaken(String name) {
        Character existing = DbManager.tx(session -> {
            Query<Character> query = session.createQuery("from Character c where c.name = :name", Character.class);
            query.setParameter("name", name);
            return query.uniqueResult();
        });

        return existing != null;
    }

    public static Character createCharacter(String name, User user, CharacterAppearance appearance) {
        long time = Instant.now().getEpochSecond();

        Character character = new Character();
        character.setName(name);
        character.setUser(user);
        character.setCreationTime((int) time);
        character.setActive(1);
        character.setAppearance(Arrays.asList(appearance));

        appearance.setCharacter(character);

        user.setCurrentCharacter(character);
        if (user.getMainCharacterId() == null) {
            user.setMainCharacter(character);
        }

        DbManager.txVoid(session -> {
            session.save(character);
            session.save(appearance);
            session.update(user);
        });

        return character;
    }

    // ========================================================================
    // Character Selection
    // ========================================================================

    public static Character selectCharacter(User user, int index) {
        List<Character> characters = DbManager.tx(session -> {
            Query<Character> query = session.createQuery("from Character c where user=:user and c.active=1",
                    Character.class);
            query.setParameter("user", user);
            return query.list();
        });

        characters = sortByMain(characters, user.getMainCharacterId());
        index = clampIndex(index, characters.size());

        Character character = characters.get(index);
        user.setCurrentCharacter(character);

        DbManager.txVoid(session -> session.update(user));

        return character;
    }

    private static int clampIndex(int index, int size) {
        if (index < 0 || index > size - 1) {
            return 0;
        }
        return index;
    }

    // ========================================================================
    // Character Deletion
    // ========================================================================

    public static class DeleteResult {
        public final boolean success;
        public final Integer errorCode;

        private DeleteResult(boolean success, Integer errorCode) {
            this.success = success;
            this.errorCode = errorCode;
        }

        public static DeleteResult success() {
            return new DeleteResult(true, null);
        }

        public static DeleteResult error(int code) {
            return new DeleteResult(false, code);
        }
    }

    public static DeleteResult deleteCharacter(User user, int index) {
        List<Character> characters = DbManager.tx(session -> {
            Query<Character> query = session.createQuery(
                    "from Character as c inner join fetch c.appearance where user=:user and c.active=1",
                    Character.class);
            query.setParameter("user", user);
            return query.list();
        });

        characters = sortByMain(characters, user.getMainCharacterId());
        index = clampIndexForDelete(index, characters.size());

        Character character = characters.get(index);

        if (!canDelete(character)) {
            logger.error("Error while deleting character: Can't delete yet.");
            return DeleteResult.error(Error.CHAR_CANTDELETEYET.getCode());
        }

        markDeleted(character);
        clearMainCharacterIfNeeded(user, character);

        return DeleteResult.success();
    }

    private static int clampIndexForDelete(int index, int size) {
        if (index < 0 || size - 1 < index) {
            return size - 1;
        }
        return index;
    }

    private static boolean canDelete(Character character) {
        if (character.getCreationTime() == null) {
            return true;
        }

        long time = Instant.now().getEpochSecond();
        long canDeleteTime = character.getCreationTime() + CHARACTER_DELETION_WAIT_SECONDS;
        return time >= canDeleteTime;
    }

    private static void markDeleted(Character character) {
        character.setActive(0);
        character.setOldName(character.getName());
        character.setName(":#" + character.getId());

        DbManager.txVoid(session -> session.update(character));
    }

    private static void clearMainCharacterIfNeeded(User user, Character character) {
        if (user.getMainCharacterId() == null) {
            return;
        }
        if (!character.getId().equals(user.getMainCharacterId())) {
            return;
        }

        DbManager.txVoid(session -> {
            User aUser = session.get(User.class, user.getId());
            aUser.setMainCharacter(null);
        });
    }
}
