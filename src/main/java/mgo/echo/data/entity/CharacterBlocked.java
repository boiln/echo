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
@Table(name = "mgo2_characters_blocked")
public class CharacterBlocked {
    @Column(nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @Column(name = "chara", nullable = false, insertable = false, updatable = false)
    private Integer characterId;

    @JoinColumn(name = "chara")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Character character;

    @Column(name = "target", nullable = false, insertable = false, updatable = false)
    private Integer targetId;

    @JoinColumn(name = "target")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Character target;

    @Version
    private Integer version;

    public CharacterBlocked() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(Integer targetId) {
        this.targetId = targetId;
    }

    public Character getTarget() {
        return target;
    }

    public void setTarget(Character target) {
        this.target = target;
    }
}