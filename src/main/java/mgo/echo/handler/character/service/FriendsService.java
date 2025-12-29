package mgo.echo.handler.character.service;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;

import mgo.echo.data.entity.Character;
import mgo.echo.data.entity.CharacterBlocked;
import mgo.echo.data.entity.CharacterFriend;
import mgo.echo.data.repository.DbManager;

/**
 * Service for friends/blocked list business logic.
 * Handles CRUD operations.
 */
public class FriendsService {
    private static final int MAX_FRIENDS = 64;
    private static final int MAX_BLOCKED = 64;

    /**
     * Result of an add operation
     */
    public enum AddResult {
        SUCCESS,
        TARGET_NOT_FOUND,
        LIST_FULL
    }

    /**
     * Get friends list for a character
     */
    public static List<CharacterFriend> getFriends(Session session, Character character) {
        Query<CharacterFriend> query = session.createQuery(
                "from CharacterFriend f join fetch f.target t left join fetch t.lobby where f.character = :chara",
                CharacterFriend.class);
        query.setParameter("chara", character);
        return query.list();
    }

    /**
     * Get blocked list for a character
     */
    public static List<CharacterBlocked> getBlocked(Session session, Character character) {
        Query<CharacterBlocked> query = session.createQuery(
                "from CharacterBlocked f join fetch f.target t left join fetch t.lobby where f.character = :chara",
                CharacterBlocked.class);
        query.setParameter("chara", character);
        return query.list();
    }

    /**
     * Add a friend or blocked entry
     * 
     * @param type 0 = friend, 1 = blocked
     * @return AddResult indicating success or failure reason
     */
    public static AddResult add(int type, Character character, int targetId) {
        return DbManager.tx(session -> {
            Character target = session.get(Character.class, targetId);
            if (target == null) {
                return AddResult.TARGET_NOT_FOUND;
            }

            if (type == 0) {
                return addFriend(session, character, target) ? AddResult.SUCCESS : AddResult.LIST_FULL;
            }

            return addBlocked(session, character, target) ? AddResult.SUCCESS : AddResult.LIST_FULL;
        });
    }

    /**
     * Remove a friend or blocked entry
     * 
     * @param type 0 = friend, 1 = blocked
     * @return true if removed, false if not found
     */
    public static boolean remove(int type, Character character, int targetId) {
        return DbManager.tx(session -> {
            if (type == 0) {
                return removeFriend(session, character, targetId);
            }

            return removeBlocked(session, character, targetId);
        });
    }

    /**
     * Get target character by ID
     */
    public static Character getTarget(int targetId) {
        return DbManager.tx(session -> session.get(Character.class, targetId));
    }

    /**
     * Add a friend to the character's friends list
     */
    private static boolean addFriend(Session session, Character character, Character target) {
        List<CharacterFriend> friends = character.getFriends();

        if (friends.size() >= MAX_FRIENDS) {
            return false;
        }

        CharacterFriend friend = new CharacterFriend();
        friend.setCharacterId(character.getId());
        friend.setCharacter(character);
        friend.setTargetId(target.getId());
        friend.setTarget(target);

        session.saveOrUpdate(friend);
        friends.add(friend);
        return true;
    }

    /**
     * Add a character to the blocked list
     */
    private static boolean addBlocked(Session session, Character character, Character target) {
        List<CharacterBlocked> blocked = character.getBlocked();

        if (blocked.size() >= MAX_BLOCKED) {
            return false;
        }

        CharacterBlocked block = new CharacterBlocked();
        block.setCharacterId(character.getId());
        block.setCharacter(character);
        block.setTargetId(target.getId());
        block.setTarget(target);

        session.saveOrUpdate(block);
        blocked.add(block);
        return true;
    }

    /**
     * Remove a friend from the character's friends list
     */
    private static boolean removeFriend(Session session, Character character, int targetId) {
        List<CharacterFriend> friends = character.getFriends();

        for (CharacterFriend friend : friends) {
            if (friend.getTargetId() == targetId) {
                friends.remove(friend);
                session.remove(friend);
                return true;
            }
        }

        return false;
    }

    /**
     * Remove a character from the blocked list
     */
    private static boolean removeBlocked(Session session, Character character, int targetId) {
        List<CharacterBlocked> blocked = character.getBlocked();

        for (CharacterBlocked block : blocked) {
            if (block.getTargetId() == targetId) {
                blocked.remove(block);
                session.remove(block);
                return true;
            }
        }

        return false;
    }
}
