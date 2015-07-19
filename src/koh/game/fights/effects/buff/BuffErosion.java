package koh.game.fights.effects.buff;

import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.protocol.client.enums.FightDispellableEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTemporaryBoostEffect;
import koh.protocol.types.game.actions.fight.FightTriggeredEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class BuffErosion extends BuffEffect {

    public BuffErosion(EffectCast CastInfos, Fighter Target) {
        super(CastInfos, Target, BuffActiveType.ACTIVE_ATTACKED_AFTER_JET, BuffDecrementType.TYPE_ENDTURN);
    }

    @Override
    public int ApplyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        float pdamage = this.CastInfos.RandomJet(Target) / 100.00f;
        int BuffValue = this.Target.MaxLife() - (int) (DamageValue.getValue() * pdamage);
        if (BuffValue < 0) {
            BuffValue = 0;
        }
        this.Target.setLifeMax(BuffValue);

        return super.ApplyEffect(DamageValue, DamageInfos);
    }

    @Override
    public AbstractFightDispellableEffect GetAbstractFightDispellableEffect() {
         return new FightTemporaryBoostEffect(this.GetId(), this.Target.ID, (short) this.Duration, FightDispellableEnum.DISPELLABLE, (short) this.CastInfos.SpellId, this.CastInfos.GetEffectUID(), this.CastInfos.ParentUID, (short) Math.abs(this.CastInfos.RandomJet(Target)));
    }
}