package koh.game.entities.item;

import koh.game.dao.api.ItemTemplateDAO;
import koh.game.entities.actors.character.GenericStats;
import koh.game.entities.spells.*;
import koh.protocol.client.enums.EffectGenerationType;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.data.items.ObjectEffect;
import koh.protocol.types.game.data.items.effects.*;
import koh.utils.Enumerable;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Neo-Craft
 */
public class ItemSet {

    private static final Logger logger = LogManager.getLogger(ItemSet.class);
    @Getter
    private int id;
    @Getter
    private int[] items;
    @Getter
    private boolean bonusIsSecret;
    @Getter
    private EffectInstance[][] effects; //Dice
    @Getter
    private GenericStats[] myStats;

    //TODO: Create dofusMaps  ObjectEffect[] toObjectEffects
    //TODO2: se rappeller poruquoi je voulais faire sa

    public ItemSet(ResultSet result) throws SQLException {
        this.id = result.getInt("id");
        this.items = Enumerable.stringToIntArray(result.getString("items"));
        this.bonusIsSecret = result.getBoolean("bonus_is_secret");
        IoBuffer buf = IoBuffer.wrap(result.getBytes("effects"));
        this.effects = new EffectInstance[buf.getInt()][];
        for (int i = 0; i < this.effects.length; ++i) {
            this.effects[i] = ItemTemplateDAO.readEffectInstance(buf);
        }
        buf.clear();
    }


    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private void parseStats() {
        StatsEnum Stat;
        this.myStats = new GenericStats[this.effects.length];
        for (int i = 0; i < this.effects.length; i++) {

            this.myStats[i] = new GenericStats();
            for (EffectInstance e : EffectHelper.generateIntegerEffectArray(this.effects[i], EffectGenerationType.NORMAL, false)) {
                if (e == null) {
                    continue;
                }
                if (e instanceof EffectInstanceInteger) {
                    Stat = StatsEnum.valueOf(e.effectId);
                    if (Stat == null) {
                        logger.error("Undefined Stat id {} ", e.effectId);
                        continue;
                    }
                    this.myStats[i].addItem(Stat, ((EffectInstanceInteger) e).value);
                }

            }
        }

        Stat = null;
    }

    public GenericStats getStats(int round) {
        try {
            if (this.myStats == null) {
                this.parseStats();
            }
            return myStats[round - 1];
        } catch (Exception e) {
            return null;
        }
    }

    public ObjectEffect[] toObjectEffects(int round) {
        try {
            ObjectEffect[] array = new ObjectEffect[effects[round - 1].length];
            for (int i = 0; i < array.length; ++i) {
                //EffectInstanceCreate 
                if (array[i] == null) {
                    return new ObjectEffect[0];
                }
                if (effects[round - 1][i] instanceof EffectInstanceLadder) {
                    array[i] = new ObjectEffectLadder((effects[round - 1][i]).effectId, ((EffectInstanceLadder) effects[round - 1][i]).monsterCount, ((EffectInstanceLadder) effects[round - 1][i]).monsterFamilyId);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceCreature) {
                    array[i] = new ObjectEffectCreature((effects[round - 1][i]).effectId, ((EffectInstanceCreature) effects[round - 1][i]).monsterFamilyId);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceMount) {
                    array[i] = new ObjectEffectMount((effects[round - 1][i]).effectId, ((EffectInstanceMount) effects[round - 1][i]).date, ((EffectInstanceMount) effects[round - 1][i]).modelId, ((EffectInstanceMount) effects[round - 1][i]).mountId);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceString) {
                    array[i] = new ObjectEffectString((effects[round - 1][i]).effectId, ((EffectInstanceString) effects[round - 1][i]).text);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceCreature) {
                    array[i] = new ObjectEffectCreature((effects[round - 1][i]).effectId, ((EffectInstanceCreature) effects[round - 1][i]).monsterFamilyId);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceMinMax) {
                    array[i] = new ObjectEffectMinMax((effects[round - 1][i]).effectId, ((EffectInstanceMinMax) effects[round - 1][i]).MinValue, ((EffectInstanceMinMax) effects[round - 1][i]).MaxValue);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceDate) {
                    array[i] = new ObjectEffectDate((effects[round - 1][i]).effectId, ((EffectInstanceDate) effects[round - 1][i]).Year, ((EffectInstanceDate) effects[round - 1][i]).Mounth, ((EffectInstanceDate) effects[round - 1][i]).Day, ((EffectInstanceDate) effects[round - 1][i]).Hour, ((EffectInstanceDate) effects[round - 1][i]).Minute);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceDice) {
                    array[i] = new ObjectEffectDice((effects[round - 1][i]).effectId, ((EffectInstanceDice) effects[round - 1][i]).diceNum, ((EffectInstanceDice) effects[round - 1][i]).diceSide, ((EffectInstanceInteger) effects[round - 1][i]).value);
                    continue;
                }
                if (effects[round - 1][i] instanceof EffectInstanceInteger) {
                    array[i] = new ObjectEffectInteger((effects[round - 1][i]).effectId, ((EffectInstanceInteger) effects[round - 1][i]).value);
                }

            }
            return array;
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            return new ObjectEffect[0];
        }
    }

}
