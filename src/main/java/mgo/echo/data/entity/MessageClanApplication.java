package mgo.echo.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "mgo2_messages_clanapplications")
public class MessageClanApplication {
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @Column(name = "clan", nullable = false, insertable = false, updatable = false)
    private Integer clanId;

    @JoinColumn(name = "clan")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Clan clan;

    @Column(name = "chara", nullable = false, insertable = false, updatable = false)
    private Integer characterId;

    @JoinColumn(name = "chara")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Character character;

    @Column(nullable = false, length = 128)
    private String comment;

    @Column(nullable = false)
    private Integer time;

    public MessageClanApplication() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClanId() {
        return clanId;
    }

    public void setClanId(Integer clanId) {
        this.clanId = clanId;
    }

    public Clan getClan() {
        return clan;
    }

    public void setClan(Clan clan) {
        this.clan = clan;
    }

    public Integer getCharacterId() {
        return characterId;
    }

    public void setCharacterId(Integer characterId) {
        this.characterId = characterId;
    }

    public Character getCharacter() {
        return character;
    }

    public void setCharacter(Character character) {
        this.character = character;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }
}