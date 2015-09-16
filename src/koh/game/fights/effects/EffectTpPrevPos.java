package koh.game.fights.effects;

import koh.game.fights.FightCell;
import koh.game.fights.Fighter;
import static koh.protocol.client.enums.ActionIdEnum.ACTION_CHARACTER_TELEPORT_ON_SAME_MAP;
import koh.protocol.messages.game.actions.fight.GameActionFightTeleportOnSameMapMessage;

/**
 *
 * @author Melancholia
 */
public class EffectTpPrevPos extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) {
        int toReturn = -1;
        for (Fighter Target : CastInfos.Targets) {
            if (Target.previousCellPos.isEmpty()) {
                continue;
            }
            FightCell cell = Target.Fight.GetCell(Target.previousCellPos.get(Target.previousCellPos.size() - 1));

            if (cell != null) {
                Target.Fight.sendToField(new GameActionFightTeleportOnSameMapMessage(ACTION_CHARACTER_TELEPORT_ON_SAME_MAP, CastInfos.Caster.ID, Target.ID, CastInfos.CellId));

                toReturn = Target.SetCell(cell);
            }
            if (toReturn != -1) {
                break;
            }
        }

        return toReturn;
    }

}