package koh.game.fights.effects;

import java.util.HashMap;
import koh.game.entities.spells.SpellLevel;
import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffStats;
import koh.protocol.client.enums.StatsEnum;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class EffectStatsSteal extends EffectBase {

    public static final HashMap<StatsEnum, StatsEnum> TargetMalus = new HashMap<StatsEnum, StatsEnum>() {
        {
            this.put(StatsEnum.Steal_Vitality, StatsEnum.Sub_Vitality);
            this.put(StatsEnum.Steal_Strength, StatsEnum.Sub_Strength);
            this.put(StatsEnum.Steal_Intelligence, StatsEnum.Sub_Intelligence);
            this.put(StatsEnum.Steal_Agility, StatsEnum.Sub_Agility);
            this.put(StatsEnum.Steal_Wisdom, StatsEnum.Sub_Wisdom);
            this.put(StatsEnum.Steal_Chance, StatsEnum.Sub_Chance);
            this.put(StatsEnum.Steal_PA, StatsEnum.Sub_PA);
            this.put(StatsEnum.Steal_PM, StatsEnum.Sub_PM);
            this.put(StatsEnum.Steal_Range, StatsEnum.Sub_Range);
        }
    };

    public static final HashMap<StatsEnum, StatsEnum> CasterBonus = new HashMap<StatsEnum, StatsEnum>() {
        {
            this.put(StatsEnum.Steal_Vitality, StatsEnum.Vitality);
            this.put(StatsEnum.Steal_Strength, StatsEnum.Strength);
            this.put(StatsEnum.Steal_Intelligence, StatsEnum.Intelligence);
            this.put(StatsEnum.Steal_Agility, StatsEnum.Agility);
            this.put(StatsEnum.Steal_Wisdom, StatsEnum.Wisdom);
            this.put(StatsEnum.Steal_Chance, StatsEnum.Chance);
            this.put(StatsEnum.Steal_PA, StatsEnum.ActionPoints);
            this.put(StatsEnum.Steal_PM, StatsEnum.MovementPoints);
            this.put(StatsEnum.Steal_Range, StatsEnum.Add_Range);
        }
    };

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        StatsEnum MalusType = TargetMalus.get(CastInfos.EffectType);
        StatsEnum BonusType = CasterBonus.get(CastInfos.EffectType);

        EffectCast MalusInfos = new EffectCast(MalusType, CastInfos.SpellId, CastInfos.CellId, CastInfos.Chance, CastInfos.Effect, CastInfos.Caster, CastInfos.Targets, false, StatsEnum.NONE, 0, CastInfos.SpellLevel, CastInfos.Duration, 0);
        EffectCast BonusInfos = new EffectCast(BonusType, CastInfos.SpellId, CastInfos.CellId, CastInfos.Chance, CastInfos.Effect, CastInfos.Caster, CastInfos.Targets, false, StatsEnum.NONE, 0, CastInfos.SpellLevel, CastInfos.Duration - 1, 0);
        MutableInt DamageValue = new MutableInt();

        for (Fighter Target : CastInfos.Targets) {
            if (Target == CastInfos.Caster) {
                continue;
            }

            // Malus a la cible
            BuffStats BuffStats = new BuffStats(MalusInfos, Target);
            if (BuffStats.ApplyEffect(DamageValue, null) == -3) {
                return -3;
            }

            Target.Buffs.AddBuff(BuffStats);

            // Bonus au lanceur
            BuffStats = new BuffStats(BonusInfos, CastInfos.Caster);
            if (BuffStats.ApplyEffect(DamageValue, null) == -3) {
                return -3;
            }

            CastInfos.Caster.Buffs.AddBuff(BuffStats);
        }

        return -1;
    }

}