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
import jakarta.persistence.Version;

@Entity
@Table(name = "mgo2_clans_members")
public class ClanMember {
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

    @Version
    private Integer version;

    public ClanMember() {

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
}
