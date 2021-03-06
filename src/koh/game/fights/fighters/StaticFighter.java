package koh.game.fights.fighters;

import koh.game.entities.mob.MonsterGrade;
import koh.game.fights.Fighter;
import koh.game.fights.IFightObject;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.context.fight.GameFightFighterLightInformations;
import koh.protocol.types.game.context.fight.GameFightFighterMonsterLightInformations;
import koh.utils.Enumerable;
import lombok.Getter;

/**
 *
 * @author Neo-Craft
 */
public abstract class StaticFighter extends Fighter {

    @Getter
    protected MonsterGrade grade;

    public StaticFighter(koh.game.fights.Fight Fight, Fighter summoner, MonsterGrade grade) {
        super(Fight, summoner);
        this.grade = grade;
    }

    public void adjustStats() {
        this.stats.addBase(StatsEnum.VITALITY, (short) ((double) this.stats.getEffect(StatsEnum.VITALITY).base * ( (double) this.summoner.getLevel() / 100.0)));
        this.stats.addBase(StatsEnum.INTELLIGENCE, (short) ((double) this.stats.getEffect(StatsEnum.INTELLIGENCE).base * ((double) this.summoner.getLevel() / 100.0)));
        this.stats.addBase(StatsEnum.CHANCE, (short) ((double) this.stats.getEffect(StatsEnum.CHANCE).base * ((double) this.summoner.getLevel() / 100.0)));
        this.stats.addBase(StatsEnum.STRENGTH, (short) ((double) this.stats.getEffect(StatsEnum.STRENGTH).base * ( (double) this.summoner.getLevel() / 100.0)));
        this.stats.addBase(StatsEnum.AGILITY, (short) ((double) this.stats.getEffect(StatsEnum.AGILITY).base * ((double) this.summoner.getLevel() / 100.0)));
        this.stats.addBase(StatsEnum.WISDOM, (short) ((double) this.stats.getEffect(StatsEnum.WISDOM).base * ( (double) this.summoner.getLevel() / 100.0)));
    }
    

    @Override
    public int getMaxAP() {
        return 0;
    }

    @Override
    public int getMaxMP() {
        return 0;
    }

    @Override
    public int getAP() {
        return 0;

    }

    @Override
    public int getMP() {
        return 0;
    }

    private boolean firstTurn = true;

    public void onBeginTurn() {
        if (firstTurn) {
            this.fight.affectSpellTo(this, this, this.grade.getGrade(), this.grade.getMonster().getSpells());
            this.firstTurn = false;
        }
    }

    @Override
    public GameFightFighterLightInformations getGameFightFighterLightInformations() {
        return new GameFightFighterMonsterLightInformations(getID(), wave, getLevel(), (byte) 0, false, isAlive(), grade.getMonsterId());
    }

    @Override
    protected double getTacklePercent(Fighter tackler){
        return 0;
    }

    @Override
    public int compareTo(IFightObject obj) {
        return getPriority().compareTo(obj.getPriority());
    }

    @Override
    public FightObjectType getObjectType() {
        return FightObjectType.OBJECT_STATIC;
    }



}
