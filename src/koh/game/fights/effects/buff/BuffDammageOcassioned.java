package koh.game.fights.effects.buff;

import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.protocol.client.enums.FightDispellableEnum;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTemporaryBoostEffect;
import koh.protocol.types.game.actions.fight.FightTriggeredEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class BuffDammageOcassioned extends BuffEffect {

    private int Jet;

    public BuffDammageOcassioned(EffectCast CastInfos, Fighter Target) {
        super(CastInfos, Target, BuffActiveType.ACTIVE_ATTACKED_AFTER_JET, BuffDecrementType.TYPE_ENDTURN);
        this.Jet = CastInfos.RandomJet(Target);
    }

    @Override
    public int ApplyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        DamageValue.setValue((DamageValue.intValue() * this.Jet) / 100);
        return super.ApplyEffect(DamageValue, DamageInfos);
    }

    @Override
    public AbstractFightDispellableEffect GetAbstractFightDispellableEffect() {
        return new FightTemporaryBoostEffect(this.GetId(), this.Target.ID, (short) this.Duration, FightDispellableEnum.DISPELLABLE, (short) this.CastInfos.SpellId, this.CastInfos.GetEffectUID(), this.CastInfos.ParentUID, (short) Math.abs(this.Jet));
    }

}