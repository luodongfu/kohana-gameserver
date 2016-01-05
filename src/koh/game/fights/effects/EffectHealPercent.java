package koh.game.fights.effects;

import koh.game.fights.effects.buff.BuffHealPercent;
import koh.game.fights.Fighter;
import koh.protocol.client.enums.ActionIdEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightLifePointsGainMessage;

/**
 *
 * @author Neo-Craft
 */
public class EffectHealPercent extends EffectBase {

    @Override
    public int applyEffect(EffectCast CastInfos) {
        // Si > 0 alors c'est un buff
        if (CastInfos.duration > 0) {
            // L'effet est un poison
            CastInfos.isPoison = true;

            // Ajout du buff
            for (Fighter Target : CastInfos.targets) {
                Target.getBuff().addBuff(new BuffHealPercent(CastInfos, Target));
            }
        } else // HEAL direct
        {
            for (Fighter Target : CastInfos.targets) {
                if (EffectHealPercent.ApplyHealPercent(CastInfos, Target, CastInfos.randomJet(Target)) == -3) {
                    return -3;
                }
            }
        }

        return -1;
    }

    public static int ApplyHealPercent(EffectCast CastInfos, Fighter Target, int Heal) {
        Fighter Caster = CastInfos.caster;

        // boost soin etc
        Heal = Heal * (Target.getLife() / 100);

        // Si le soin est superieur a sa vie actuelle
        if ((Target.getLife() + Heal) > Target.getMaxLife()) {
            Heal = Target.getMaxLife() - Target.getLife();
        }

        if (Heal < 0) {
            Heal = 0;
        }

        // Affectation
        Target.setLife(Target.getLife() + Heal);

        // Envoi du packet
        if (Heal != 0) {
            Target.getFight().sendToField(new GameActionFightLifePointsGainMessage(ActionIdEnum.ACTION_CHARACTER_LIFE_POINTS_LOST, Caster.getID(), Target.getID(), Heal));
        }

        // Le soin entraine la fin du combat ?
        return Target.tryDie(Caster.getID());
    }

}
