package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.protocol.client.enums.ActionIdEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightDispellMessage;

/**
 *
 * @author Neo-Craft
 */
public class EffectDebuff extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        for (Fighter Target : CastInfos.Targets) {
            if (Target.Buffs.Debuff() == -3) {
                return -3;
            }

            Target.Fight.sendToField(new GameActionFightDispellMessage(ActionIdEnum.ACTION_CHARACTER_REMOVE_ALL_EFFECTS, CastInfos.Caster.ID, Target.ID));
        }

        return -1;
    }

}