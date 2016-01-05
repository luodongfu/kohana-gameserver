package koh.game.fights.effects.buff;

import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import koh.protocol.client.enums.FightDispellableEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTriggeredEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class BuffChatiment extends BuffEffect {

    public BuffChatiment(EffectCast CastInfos, Fighter Target) {
        super(CastInfos, Target, BuffActiveType.ACTIVE_ATTACKED_AFTER_JET, BuffDecrementType.TYPE_ENDTURN);
    }

    @Override
    public int applyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        MutableInt BuffValue = new MutableInt(DamageValue.getValue() / 2); // Divise par deux les stats a boost car c'est un personnage.
        //var StatsType = (EffectEnum)this.castInfos.value1 == EffectEnum.HEAL ? EffectEnum.AddVitalite : (EffectEnum)this.castInfos.value1;
        int MaxValue = this.castInfos.effect.diceSide;
        int Duration = this.castInfos.effect.value;

        if (this.target.getFight().getCurrentFighter().getID() == this.castInfos.fakeValue) {
            if (this.castInfos.damageValue < MaxValue) {
                if (this.castInfos.damageValue + BuffValue.getValue() > MaxValue) {
                    BuffValue.setValue(MaxValue - this.castInfos.damageValue);
                }
            } else {
                BuffValue.setValue(0);
            }
        } else {
            this.castInfos.damageValue = 0;
            this.castInfos.fakeValue = (int) this.target.getFight().getCurrentFighter().getID();

            if (this.castInfos.damageValue + BuffValue.getValue() > MaxValue) {
                BuffValue.setValue(MaxValue);
            }
        }
        if (BuffValue.getValue() > 0) {
            this.castInfos.damageValue += BuffValue.getValue();
            BuffStats BuffStats = new BuffStats(new EffectCast(StatsEnum.valueOf(this.castInfos.effect.diceNum), this.castInfos.spellId, this.castInfos.cellId, 0, null, castInfos.caster, null, false, this.castInfos.effectType, BuffValue.getValue(), null, Duration, this.GetId()), this.target);

            BuffStats.applyEffect(BuffValue, DamageInfos);
            this.target.getBuff().addBuff(BuffStats);
        }

        return super.applyEffect(DamageValue, DamageInfos);
    }

    @Override
    public AbstractFightDispellableEffect getAbstractFightDispellableEffect() {
        return new FightTriggeredEffect(this.GetId(), this.target.getID(), (short) this.duration, FightDispellableEnum.REALLY_NOT_DISPELLABLE, this.castInfos.spellId, this.castInfos.effect.effectUid, 0, (short) this.castInfos.effect.diceNum, (short) this.castInfos.effect.diceSide, (short) this.castInfos.effect.value, (short) 0/*(this.castInfos.effect.delay)*/);
    }

}
