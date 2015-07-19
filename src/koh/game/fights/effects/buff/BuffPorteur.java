package koh.game.fights.effects.buff;

import koh.game.fights.Fighter;
import koh.game.fights.effects.EffectCast;
import static koh.protocol.client.enums.ActionIdEnum.ACTION_CARRY_CHARACTER;
import koh.protocol.client.enums.FightDispellableEnum;
import koh.protocol.client.enums.FightStateEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightCarryCharacterMessage;
import koh.protocol.types.game.actions.fight.AbstractFightDispellableEffect;
import koh.protocol.types.game.actions.fight.FightTemporaryBoostStateEffect;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 *
 * @author Neo-Craft
 */
public class BuffPorteur extends BuffEffect {

    public BuffPorteur(EffectCast CastInfos, Fighter Target) {
        super(CastInfos, Target, BuffActiveType.ACTIVE_ENDMOVE, BuffDecrementType.TYPE_ENDMOVE);
        this.Duration = -1;
        CastInfos.Caster.States.FakeState(FightStateEnum.Porteur, true);
        this.CastInfos.EffectType = StatsEnum.Add_State;
        this.Caster.Fight.sendToField(new GameActionFightCarryCharacterMessage(ACTION_CARRY_CHARACTER,Caster.ID,Target.ID,Caster.CellId()));
    }

    @Override
    public AbstractFightDispellableEffect GetAbstractFightDispellableEffect() {
        return new FightTemporaryBoostStateEffect(this.GetId(), this.Caster.ID, (short) this.Duration, FightDispellableEnum.REALLY_NOT_DISPELLABLE, (short) this.CastInfos.SpellId, (short)/*this.CastInfos.GetEffectUID()*/ 2, this.CastInfos.ParentUID, (short) 1, (short) 3);
    }

    @Override
    public int ApplyEffect(MutableInt DamageValue, EffectCast DamageInfos) {
        // Si effet finis
        if (!this.Target.States.HasState(FightStateEnum.Porté)) {
            this.Duration = 0;
            return -1;
        }

        // On affecte la meme cell pour la cible porté
        return this.Target.SetCell(this.Caster.myCell);
    }

    @Override
    public int RemoveEffect() {
        CastInfos.Caster.States.FakeState(FightStateEnum.Porteur, false);
        this.Duration = 0;
        return super.RemoveEffect();
    }

}