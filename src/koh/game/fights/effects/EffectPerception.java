package koh.game.fights.effects;

import koh.game.entities.environments.cells.Zone;
import koh.game.entities.maps.pathfinding.MapPoint;
import koh.game.fights.FightCell;
import koh.game.fights.Fighter;
import koh.game.fights.IFightObject.FightObjectType;
import koh.game.fights.fighters.CharacterFighter;
import koh.game.fights.fighters.IllusionFighter;
import koh.game.fights.layers.FightTrap;
import static koh.protocol.client.enums.ActionIdEnum.ACTION_CHARACTER_MAKE_INVISIBLE;
import koh.protocol.client.enums.GameActionFightInvisibilityStateEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightInvisibleDetectedMessage;
import koh.protocol.messages.game.context.fight.character.GameFightRefreshFighterMessage;

/**
 *
 * @author Neo-Craft
 */
public class EffectPerception extends EffectBase {

    //TODO dofusMaps AffectedCell in castInfos et nettoyer ce code
    @Override
    public int applyEffect(EffectCast castInfos) {
        for (short Cell : (new Zone(castInfos.effect.getZoneShape(), castInfos.effect.zoneSize(), MapPoint.fromCellId(castInfos.caster.getCellId()).advancedOrientationTo(MapPoint.fromCellId(castInfos.cellId), true), castInfos.caster.getFight().getMap())).getCells(castInfos.cellId)) {
            FightCell fightCell = castInfos.caster.getFight().getCell(Cell);
            if (fightCell != null) {
                fightCell.getObjects().stream().filter((fightObject) -> (fightObject.getCellId() == Cell)).forEach((fightObject) -> {
                    if (fightObject.getObjectType() == FightObjectType.OBJECT_TRAP && ((FightTrap) fightObject).visibileState == GameActionFightInvisibilityStateEnum.INVISIBLE && ((FightTrap) fightObject).caster.isEnnemyWith(castInfos.caster)) {
                        ((FightTrap) fightObject).visibileState = GameActionFightInvisibilityStateEnum.DETECTED;
                        ((FightTrap) fightObject).appearForAll();
                    } else if (fightObject instanceof IllusionFighter) {
                        ((IllusionFighter) fightObject).tryDie(castInfos.caster.getID());
                    } else if (fightObject.getObjectType() == FightObjectType.OBJECT_FIGHTER) {
                        Fighter fighter = (Fighter) fightObject;
                        if (fighter.isEnnemyWith(castInfos.caster)) {
                            if (fighter instanceof CharacterFighter && fighter.getTeam().getAliveFighters().anyMatch(Fighter -> (Fighter instanceof IllusionFighter) && Fighter.getSummoner() == fighter)) {
                                ((CharacterFighter) fighter).cleanClone();
                            } else if (fighter.getVisibleState() == GameActionFightInvisibilityStateEnum.INVISIBLE) {
                                fighter.setVisibleState(GameActionFightInvisibilityStateEnum.DETECTED);
                                castInfos.caster.getFight().sendToField(new GameActionFightInvisibleDetectedMessage(ACTION_CHARACTER_MAKE_INVISIBLE, castInfos.caster.getID(), fighter.getID(), fighter.getCellId()));
                                //castInfos.caster.fight.sendToField(new GameActionFightInvisibilityMessage(ACTION_CHARACTER_MAKE_INVISIBLE, castInfos.caster.getID(), fighter.id, fighter.visibleState.value));
                                castInfos.caster.getFight().sendToField(new GameFightRefreshFighterMessage(fighter.getGameContextActorInformations(null)));
                            }
                            /*if(fighter.StateManager.hasState(FighterStateEnum.STATE_STEALTH))
                             {
                             fighter.BuffManager.RemoveStealth();
                             }*/
                        }
                    }
                });
            }
        }
        return -1;
    }

}
