package koh.game.fights.effects;

import koh.game.entities.environments.Pathfinder;
import koh.game.fights.FightCell;
import koh.game.fights.Fighter;
import koh.game.fights.IFightObject;
import koh.protocol.messages.game.actions.fight.GameActionFightSlideMessage;

/**
 *
 * @author Neo-Craft
 */
public class EffectPushFear extends EffectBase {

    @Override
    public int ApplyEffect(EffectCast CastInfos) { //TODO : Prise compte etat
        byte direction = Pathfinder.getDirection(CastInfos.Caster.fight.map, CastInfos.Caster.getCellId(), CastInfos.CellId);
        short targetFighterCell = Pathfinder.nextCell(CastInfos.Caster.getCellId(), direction);

        Fighter target = CastInfos.Caster.fight.getFighterOnCell(targetFighterCell);
        if (target == null) {
            return -1;
        }
        short StartCell = target.getCellId();
        int distance = Pathfinder.getGoalDistance(CastInfos.Caster.fight.map, target.getCellId(), CastInfos.CellId);
        FightCell currentCell = target.myCell;

        for (int i = 0; i < distance; i++) {
            FightCell nextCell = CastInfos.Caster.fight.getCell(Pathfinder.nextCell(currentCell.Id, direction));

            if (nextCell != null && nextCell.CanWalk()) {
                if (nextCell.HasObject(IFightObject.FightObjectType.OBJECT_TRAP)) {
                    target.fight.sendToField(new GameActionFightSlideMessage(CastInfos.Effect.effectId, CastInfos.Caster.ID, target.ID, StartCell, nextCell.Id));

                    return target.setCell(nextCell);
                }
            } else {
                if (i != 0) {
                    target.fight.sendToField(new GameActionFightSlideMessage(CastInfos.Effect.effectId, CastInfos.Caster.ID, target.ID, StartCell, currentCell.Id));
                }

                return target.setCell(currentCell);
            }

            currentCell = nextCell;
        }

        target.fight.sendToField(new GameActionFightSlideMessage(CastInfos.Effect.effectId, CastInfos.Caster.ID, target.ID, StartCell, currentCell.Id));

        return target.setCell(currentCell);
    }

}
