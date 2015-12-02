package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffEnableBomb;
import koh.game.fights.fighters.BombFighter;
import koh.game.fights.layer.FightBomb;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.client.enums.TextInformationTypeEnum;
import koh.protocol.messages.game.basic.TextInformationMessage;
import org.apache.commons.lang.ArrayUtils;

/**
 *
 * @author Neo-Craft
 */
public class EffectEnableBomb extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        for (Fighter Target : CastInfos.Targets) {
            if (Target instanceof BombFighter) {
                if (CastInfos.Duration > 0) {
                    Target.buff.addBuff(new BuffEnableBomb(CastInfos, Target));
                    continue;
                }
                if (Explose(Target, CastInfos) == -3) {
                    return -3;
                }
            }
        }
        return -1;
    }

    public static int Explose(Fighter Target, EffectCast CastInfos) {
        BombFighter[] FutureExplosedBombs = new BombFighter[0];
        if (((BombFighter) Target).FightBombs != null) {
            int TotalCombo = Target.stats.getBoost(StatsEnum.Combo_Dammages);
            for (FightBomb Bomb : ((BombFighter) Target).FightBombs) {
                FutureExplosedBombs = (BombFighter[]) ArrayUtils.add(FutureExplosedBombs, (BombFighter) ((Bomb.Owner[0].ID == Target.ID) ? Bomb.Owner[1] : Bomb.Owner[0]));
                TotalCombo += (Bomb.Owner[0].ID == Target.ID) ? Bomb.Owner[1].stats.getBoost(StatsEnum.Combo_Dammages) : Bomb.Owner[0].stats.getBoost(StatsEnum.Combo_Dammages);
            }
            for (BombFighter Bomb : FutureExplosedBombs) {
                if (Bomb.FightBombs != null) {
                    for (FightBomb Bomb2 : Bomb.FightBombs) {
                        if (Bomb2.Owner[0].ID != Target.ID && !ArrayUtils.contains(FutureExplosedBombs, Bomb2.Owner[0])) {
                            FutureExplosedBombs = (BombFighter[]) ArrayUtils.add(FutureExplosedBombs, Bomb2.Owner[0]);
                            TotalCombo += Bomb2.Owner[0].stats.getBoost(StatsEnum.Combo_Dammages);
                        }
                        if (Bomb2.Owner[1].ID != Target.ID && !ArrayUtils.contains(FutureExplosedBombs, Bomb2.Owner[1])) {
                            FutureExplosedBombs = (BombFighter[]) ArrayUtils.add(FutureExplosedBombs, Bomb2.Owner[1]);
                            TotalCombo += Bomb2.Owner[1].stats.getBoost(StatsEnum.Combo_Dammages);
                        }
                    }
                }
            }
            CastInfos.Caster.fight.sendToField(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 0, new String[]{"Combo : +" + TotalCombo + "% dommages d'explosion"}));
            Target.stats.getEffect(StatsEnum.Combo_Dammages).additionnal = TotalCombo;
            for (BombFighter Bomb : FutureExplosedBombs) {
                Bomb.stats.getEffect(StatsEnum.Combo_Dammages).additionnal = TotalCombo;
                Bomb.tryDie(CastInfos.Caster.ID, true);
            }
        }
        return Target.tryDie(CastInfos.Caster.ID, true);
    }

}
