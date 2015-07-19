package koh.game.fights.effects;

import koh.game.fights.FightCell;
import koh.game.fights.Fighter;
import static koh.protocol.client.enums.ActionIdEnum.ACTION_CHARACTER_TELEPORT_ON_SAME_MAP;
import koh.protocol.messages.game.actions.fight.GameActionFightTeleportOnSameMapMessage;

/**
 *
 * @author Neo-Craft
 */
public class EffectTeleport extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        return ApplyTeleport(CastInfos);
    }

    public static int ApplyTeleport(EffectCast castInfos) {
        Fighter caster = castInfos.Caster;
        FightCell cell = caster.Fight.GetCell(castInfos.CellId);

        if (cell != null) {
            caster.Fight.sendToField(new GameActionFightTeleportOnSameMapMessage(ACTION_CHARACTER_TELEPORT_ON_SAME_MAP, caster.ID, caster.ID, castInfos.CellId));

            return caster.SetCell(cell);
        }

        return -1;
    }

}