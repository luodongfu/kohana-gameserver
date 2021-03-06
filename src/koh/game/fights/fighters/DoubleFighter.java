package koh.game.fights.fighters;

import koh.game.entities.actors.Player;
import koh.game.entities.actors.character.GenericStats;
import koh.game.entities.spells.SpellLevel;
import koh.game.fights.Fight;
import koh.game.fights.FightState;
import koh.game.fights.IFightObject;
import koh.game.fights.types.AgressionFight;
import koh.look.EntityLookParser;
import koh.protocol.client.Message;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.context.GameContextActorInformations;
import koh.protocol.types.game.context.fight.*;
import koh.protocol.types.game.look.EntityLook;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;

/**
 * Created by Melancholia on 1/27/16.
 */
public class DoubleFighter extends VirtualFighter {

    public DoubleFighter(Fight fight, CharacterFighter summoner) {
        super(fight, summoner);
        final GenericStats stats = new GenericStats();
        this.summoner.getStats().getStats().entrySet()
                .parallelStream()
                .forEach(st -> {
                    stats.addBase(st.getKey(),st.getValue().base);
                    stats.addItem(st.getKey(),st.getValue().objectsAndMountBonus);
                });
        super.initFighter(stats,this.fight.getNextContextualId());
        super.setLife(summoner.asPlayer().getMaxLife());
        super.setLifeMax(summoner.asPlayer().getMaxLife());
        this.entityLook = EntityLookParser.copy(summoner.getEntityLook());
    }

    @Override
    public int getLevel() {
        return this.summoner.getPlayer().getLevel();
    }

    @Override
    public short getMapCell() {
        return 0;
    }

    @Override
    public void endFight() {
        super.endFight();
    }

    @Override
    public GameFightFighterLightInformations getGameFightFighterLightInformations() {
        return new GameFightFighterNamedLightInformations(this.getID(),wave, getLevel(), summoner.getPlayer().getBreed(), summoner.getPlayer().hasSexe(), isAlive(), summoner.getPlayer().getNickName());
    }

    @Override
    public GameFightMinimalStats getGameFightMinimalStats(Player character) {
        if (this.fight.getFightState() == FightState.STATE_PLACE) {
            return new GameFightMinimalStatsPreparation(this.getLife(), this.getMaxLife(), this.summoner.getPlayer().getMaxLife(), this.stats.getTotal(StatsEnum.PERMANENT_DAMAGE_PERCENT), this.shieldPoints, this.getAP(), this.getMaxAP(), this.getMP(), this.getMaxMP(), getSummonerID(), getSummonerID() != 0, this.stats.getTotal(StatsEnum.NEUTRAL_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.EARTH_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.WATER_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.AIR_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.FIRE_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.NEUTRAL_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.EARTH_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.WATER_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.AIR_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.FIRE_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.ADD_PUSH_DAMAGES_REDUCTION), this.stats.getTotal(StatsEnum.ADD_CRITICAL_DAMAGES_REDUCTION),  Math.max(this.stats.getTotal(StatsEnum.DODGE_PA_LOST_PROBABILITY),0), Math.max(this.stats.getTotal(StatsEnum.DODGE_PM_LOST_PROBABILITY),0), this.stats.getTotal(StatsEnum.ADD_TACKLE_BLOCK), this.stats.getTotal(StatsEnum.ADD_TACKLE_EVADE), character == null ? this.visibleState.value : this.getVisibleStateFor(character), this.getInitiative(false));
        }
        return new GameFightMinimalStats(this.getLife(), this.getMaxLife(), this.summoner.getPlayer().getMaxLife(), this.stats.getTotal(StatsEnum.PERMANENT_DAMAGE_PERCENT), this.shieldPoints, this.getAP(), this.getMaxAP(), this.getMP(), this.getMaxMP(), getSummonerID(), getSummonerID() != 0, this.stats.getTotal(StatsEnum.NEUTRAL_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.EARTH_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.WATER_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.AIR_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.FIRE_ELEMENT_RESIST_PERCENT), this.stats.getTotal(StatsEnum.NEUTRAL_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.EARTH_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.WATER_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.AIR_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.FIRE_ELEMENT_REDUCTION), this.stats.getTotal(StatsEnum.ADD_PUSH_DAMAGES_REDUCTION), this.stats.getTotal(StatsEnum.ADD_CRITICAL_DAMAGES_REDUCTION),  Math.max(this.stats.getTotal(StatsEnum.DODGE_PA_LOST_PROBABILITY),0), Math.max(this.stats.getTotal(StatsEnum.DODGE_PM_LOST_PROBABILITY),0), this.stats.getTotal(StatsEnum.ADD_TACKLE_BLOCK), this.stats.getTotal(StatsEnum.ADD_TACKLE_EVADE), character == null ? this.visibleState.value : this.getVisibleStateFor(character));
    }

    @Override
    public GameContextActorInformations getGameContextActorInformations(Player character) {
        return new GameFightCharacterInformations((this.ID), this.getEntityLook(), this.getEntityDispositionInformations(character), this.team.id, this.wave, this.isAlive(), this.getGameFightMinimalStats(character), this.previousPositions, this.summoner.getPlayer().getNickName(), this.summoner.getPlayer().getPlayerStatus(), (byte) this.getLevel(), this.summoner.getPlayer().getActorAlignmentInformations(), this.summoner.getPlayer().getBreed(), this.summoner.getPlayer().hasSexe());
    }

    @Override
    public FightTeamMemberInformations getFightTeamMemberInformations() {
        return new FightTeamMemberCharacterInformations(this.ID, this.summoner.getPlayer().getNickName(), (byte) this.summoner.getPlayer().getLevel());
    }

    @Override
    public void send(Message Packet) {

    }

    @Override
    public void computeReducedDamage(StatsEnum effect, MutableInt damages, boolean cc) {
        switch (effect) {
            case DAMAGE_NEUTRAL:
            case STEAL_NEUTRAL:
            case LIFE_LEFT_TO_THE_ATTACKER_NEUTRAL_DAMAGES:
                damages.setValue(damages.intValue() * (100 - Math.min((this.stats.getTotal(StatsEnum.NEUTRAL_ELEMENT_RESIST_PERCENT) + (fight instanceof AgressionFight ? stats.getTotal(StatsEnum.PVP_NEUTRAL_ELEMENT_RESIST_PERCENT) : 0)), 50)) / 100
                        - this.stats.getTotal(StatsEnum.NEUTRAL_ELEMENT_REDUCTION) - (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_NEUTRAL_ELEMENT_REDUCTION) : 0) - this.stats.getTotal(StatsEnum.ADD_MAGIC_REDUCTION));
                break;

            case DAMAGE_EARTH:
            case STEAL_EARTH:
            case LIFE_LEFT_TO_THE_ATTACKER_EARTH_DAMAGES:
                damages.setValue(damages.intValue() * (100 - Math.min((this.stats.getTotal(StatsEnum.EARTH_ELEMENT_RESIST_PERCENT) + (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_EARTH_ELEMENT_RESIST_PERCENT) : 0)), 50)) / 100
                        - this.stats.getTotal(StatsEnum.EARTH_ELEMENT_REDUCTION) - (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_EARTH_ELEMENT_REDUCTION) : 0) - this.stats.getTotal(StatsEnum.ADD_MAGIC_REDUCTION));
                break;

            case DAMAGE_FIRE:
            case STEAL_FIRE:
            case LIFE_LEFT_TO_THE_ATTACKER_FIRE_DAMAGES:
                damages.setValue(damages.intValue() * (100 - Math.min((this.stats.getTotal(StatsEnum.FIRE_ELEMENT_RESIST_PERCENT) + (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_FIRE_ELEMENT_RESIST_PERCENT) : 0)), 50)) / 100
                        - this.stats.getTotal(StatsEnum.FIRE_ELEMENT_REDUCTION) - (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_FIRE_ELEMENT_REDUCTION) : 0) - this.stats.getTotal(StatsEnum.ADD_MAGIC_REDUCTION));
                break;

            case DAMAGE_AIR:
            case STEAL_AIR:
            case LIFE_LEFT_TO_THE_ATTACKER_AIR_DAMAGES:
            case PA_USED_LOST_X_PDV:
                damages.setValue(damages.intValue() * (100 - Math.min((this.stats.getTotal(StatsEnum.AIR_ELEMENT_RESIST_PERCENT) + (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_AIR_ELEMENT_RESIST_PERCENT) : 0)), 50)) / 100
                        - this.stats.getTotal(StatsEnum.AIR_ELEMENT_REDUCTION) - (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_AIR_ELEMENT_REDUCTION) : 0) - this.stats.getTotal(StatsEnum.ADD_MAGIC_REDUCTION));
                break;

            case DAMAGE_WATER:
            case STEAL_WATER:
            case LIFE_LEFT_TO_THE_ATTACKER_WATER_DAMAGES:
                damages.setValue(damages.intValue() * (100 - Math.min((this.stats.getTotal(StatsEnum.WATER_ELEMENT_RESIST_PERCENT) + (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_WATER_ELEMENT_RESIST_PERCENT) : 0)), 50)) / 100
                        - this.stats.getTotal(StatsEnum.WATER_ELEMENT_REDUCTION) - (fight instanceof AgressionFight ? this.stats.getTotal(StatsEnum.PVP_WATER_ELEMENT_REDUCTION) : 0) - this.stats.getTotal(StatsEnum.ADD_MAGIC_REDUCTION));
                break;
        }
        if (cc) {
            damages.subtract(this.stats.getTotal(StatsEnum.ADD_CRITICAL_DAMAGES_REDUCTION));
        }
    }

    @Override
    public EntityLook getEntityLook() {
        return this.entityLook;
    }

    @Override
    public List<SpellLevel> getSpells() {
        return this.summoner.getSpells();
    }

    @Override
    public int compareTo(IFightObject obj) {
        return getPriority().compareTo(obj.getPriority());
    }

    @Override
    public int getInitiative(boolean base) {
        return this.summoner.getInitiative(base);
    }

    @Override
    public FightObjectType getObjectType() {
        return FightObjectType.OBJECT_FIGHTER;
    }
}
