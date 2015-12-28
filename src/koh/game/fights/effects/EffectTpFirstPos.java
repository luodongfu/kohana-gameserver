package koh.game.fights.effects;

import koh.game.fights.Fighter;
import koh.game.fights.effects.buff.BuffTpFirstPos;

/**
 *
 * @author Melancholia
 */
public class EffectTpFirstPos extends EffectBase {

    @Override
    public int applyEffect(EffectCast CastInfos) {
        for (Fighter Target : CastInfos.Targets) {
            Target.getBuff().addBuff(new BuffTpFirstPos(CastInfos, Target));
        }

        return -1;
    }
}
