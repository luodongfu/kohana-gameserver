package koh.game.fights.effects;

import koh.game.entities.actors.Player;
import koh.game.entities.environments.Pathfunction;
import koh.game.entities.environments.cells.Zone;
import koh.game.entities.maps.pathfinding.MapPoint;
import koh.game.fights.FightCell;
import koh.game.fights.Fighter;
import koh.game.fights.IFightObject;
import koh.game.fights.IFightObject.FightObjectType;
import koh.game.fights.effects.buff.BuffMaximiseEffects;
import koh.game.fights.effects.buff.BuffMinimizeEffects;
import koh.game.fights.effects.buff.BuffPorteur;
import koh.game.fights.fighters.BombFighter;
import koh.game.fights.fighters.MonsterFighter;
import koh.game.fights.fighters.SummonedFighter;
import koh.game.fights.layers.FightPortal;
import koh.protocol.client.enums.FightStateEnum;
import koh.protocol.client.enums.SpellIDEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.messages.game.actions.fight.GameActionFightSlideMessage;
import lombok.Getter;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Neo-Craft
 */
public class EffectPush extends EffectBase {

    @Getter
    private static final Random RANDOM_PUSHDAMAGE = new Random();

    @Override
    public int applyEffect(EffectCast castInfos) {
        byte direction = 0;
        for (Fighter target : castInfos.targets.stream().filter(tarrget -> /*!(target instanceof StaticFighter) &&*/
                !tarrget.getStates().hasState(FightStateEnum.CARRIED)
                        && (tarrget.getObjectType() != IFightObject.FightObjectType.OBJECT_STATIC)
                        && !tarrget.getStates().hasState(FightStateEnum.INÉBRANLABLE)
                        && !tarrget.getStates().hasState(FightStateEnum.ENRACINÉ)
                        && !tarrget.getStates().hasState(FightStateEnum.INDÉPLAÇABLE))
                .toArray(Fighter[]::new)) {
            switch (castInfos.effectType) {
                case PUSH_X_CELL:
                case PUSH_BACK:
                    if (castInfos.spellId == SpellIDEnum.BOTTE) {
                        if (castInfos.targetKnownCellId == target.getCellId()) {
                            continue;
                        }
                        final int ID = target.getID();
                        if (Arrays.stream((new Zone(castInfos.effect.getZoneShape(), castInfos.effect.zoneSize(), MapPoint.fromCellId(castInfos.caster.getCellId()).advancedOrientationTo(MapPoint.fromCellId(castInfos.cellId), true), castInfos.getFight().getMap()))
                                .getCells(castInfos.cellId))
                                .map(cell -> castInfos.getFight().getCell(cell))
                                .filter(cell -> cell != null && cell.hasGameObject(FightObjectType.OBJECT_FIGHTER, FightObjectType.OBJECT_STATIC))
                                .map(fightCell -> fightCell.getFighter())
                                .noneMatch(tr -> tr.getID() == ID)) {
                            continue;
                        }
                        if(!(target instanceof BombFighter)){
                            castInfos.effect.diceNum = 1;
                        }
                    }
                    if (castInfos.spellId == SpellIDEnum.DESTIN_ECA && Pathfunction.inLine(target.getFight().getMap(), castInfos.cellId, target.getCellId())) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), castInfos.caster.getCellId(), target.getCellId());
                    } else if (castInfos.caster instanceof SummonedFighter
                            && castInfos.caster.asSummon().getGrade().getMonsterId() == 3289 //Tactirelle
                            && target == castInfos.caster) {
                        continue;
                    } else if (Pathfunction.inLine(target.getFight().getMap(), castInfos.cellId, target.getCellId()) && castInfos.cellId != target.getCellId()) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), castInfos.cellId, target.getCellId());
                    } else if (Pathfunction.inLine(target.getFight().getMap(), castInfos.caster.getCellId(), target.getCellId())) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), castInfos.caster.getCellId(), target.getCellId());
                    } else if (target instanceof MonsterFighter && target.asMonster().getGrade().getMonster().isCanBePushed()) {
                        continue;
                    } else if (castInfos.caster instanceof SummonedFighter
                            && castInfos.caster.asSummon().getGrade().getMonsterId() == 3289) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), castInfos.cellId, target.getCellId());
                    } else {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), castInfos.caster.getCellId(), target.getCellId());
                        //return -1;
                    }
                    break;
                case PULL_FORWARD:
                    direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), castInfos.caster.getCellId());

                    if (castInfos.isTrap) {
                        if (target.getCellId() == castInfos.targetKnownCellId)
                            continue;
                        direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), castInfos.cellId);
                    } else if (castInfos.isGlyph) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), castInfos.cellId);
                    } else if (castInfos.spellId == 5390) { //Odysee
                        direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), castInfos.casterOldCell);
                    } else if (castInfos.spellId == 5382 || castInfos.spellId == 5475 /*|| castInfos.emoteId == 5390*/) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), castInfos.targetKnownCellId);
                    } else if (castInfos.spellId == 2801) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), castInfos.targetKnownCellId);
                        if (/*castInfos.caster == target || */castInfos.targetKnownCellId == target.getCellId())
                            continue;
                    } else if (castInfos.spellId == 181) {
                        if (castInfos.caster == target)
                            continue;
                        direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), castInfos.cellId);
                    }
                    else if(castInfos.getFight().getCell(castInfos.oldCell).hasGameObject(FightObjectType.OBJECT_PORTAL)
                        && !Arrays.stream(castInfos.spellLevel.getEffects()).anyMatch(effect -> effect.getEffectType().equals(StatsEnum.DISABLE_PORTAL))){
                        final FightPortal[] portals = castInfos.getFight().getPortalsThroughPortal(castInfos.caster,
                                castInfos.oldCell,
                                true,
                                castInfos.getFight().getCell(castInfos.oldCell).getObjects().stream()
                                        .filter(x -> x.getObjectType() == FightObjectType.OBJECT_PORTAL)
                                        .findFirst()
                                        .map(f -> (FightPortal) f)
                                        .get().caster.getTeam()
                        );
                        if(portals.length > 0){
                            direction = Pathfunction.getDirection(target.getFight().getMap(), target.getCellId(), portals[portals.length -1].getCellId());
                        }
                    }
                    //System.out.println(castInfos.spellId  + " "+castInfos.effect.effectUid);
                    break;
                case BACK_CELL:
                    final Fighter p = castInfos.caster;
                    castInfos.caster = target;
                    target = p;
                    castInfos.targets.remove(0);
                    if (Pathfunction.inLine(target.getFight().getMap(), castInfos.cellId, target.getCellId()) && castInfos.cellId != target.getCellId()) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), castInfos.cellId, target.getCellId());
                    } else if (Pathfunction.inLine(target.getFight().getMap(), castInfos.caster.getCellId(), target.getCellId())) {
                        direction = Pathfunction.getDirection(target.getFight().getMap(), castInfos.caster.getCellId(), target.getCellId());
                    }
                    break;
            }
            if (EffectPush.applyPush(castInfos, target, direction, castInfos.randomJet(target)) == -3) {
                return -3;
            }
        }
        return -1;
    }

    public static int applyPush(EffectCast castInfos, Fighter target, byte direction, int length) {
        FightCell currentCell = target.getMyCell();
        final short startCell = target.getCellId();
        for (int i = 0; i < length; i++) {
            final FightCell nextCell = target.getFight().getCell(Pathfunction.nextCell(currentCell.Id, direction));

            if (nextCell != null && nextCell.canWalk()) {
                if (nextCell.hasObject(FightObjectType.OBJECT_TRAP)) {
                    castInfos.getFight().observable$Stream(p -> p != null && target.isVisibleFor(p)).forEach(p -> p.send(new GameActionFightSlideMessage(castInfos.effect == null ? 5 : castInfos.effect.effectId, castInfos.caster.getID(), target.getID(), startCell, nextCell.Id)));
                    return target.setCell(nextCell);
                } else if (nextCell.hasObject(FightObjectType.OBJECT_PORTAL) && nextCell.getObjects().stream().anyMatch(o -> o instanceof FightPortal && ((FightPortal) o).enabled)) {
                    if (castInfos.effect != null && nextCell != null)
                        castInfos.getFight().observable$Stream(p -> p != null && target.isVisibleFor(p)).forEach(p -> p.send(new GameActionFightSlideMessage(castInfos.effect.effectId, castInfos.caster.getID(), target.getID(), startCell, nextCell.Id)));
                    return target.setCell(nextCell);
                }


            } else {
                int pushResult = -1;
                if (castInfos.effectType == StatsEnum.PUSH_BACK) {
                    pushResult = EffectPush.applyPushBackDamages(castInfos, target, length, i);
                    if (pushResult != -1) {
                        return pushResult;
                    }
                }

                if (i != 0) {
                    target.getBuff().getAllBuffs().filter(x -> x instanceof BuffPorteur && x.duration != 0).forEach(x -> x.target.setCell(target.getFight().getCell(startCell)));
                    for (Player player : castInfos.getFight().observable$Stream(p -> target.isVisibleFor(p))) {
                        player.send(new GameActionFightSlideMessage(castInfos.effect.effectId, castInfos.caster.getID(), target.getID(), startCell, currentCell.Id));
                    }
                }

                if (currentCell.getId() == target.getCellId()) { //StarckOverflow's correction by pushing same cell
                    return pushResult;
                }

                final int result = target.setCell(currentCell);

                if (pushResult < result) {
                    return pushResult;
                }
                return result;
            }

            currentCell = nextCell;
        }


        final int result = target.setCell(currentCell);

        if (target.getCellId() == currentCell.getId())
            for (Player player : castInfos.getFight().observable$Stream(p -> target.isVisibleFor(p))) {
                player.send(new GameActionFightSlideMessage(castInfos.effect == null ? 5 : castInfos.effect.effectId, castInfos.caster.getID(), target.getID(), startCell, currentCell.Id));
            }

        target.getBuff().getAllBuffs().filter(x -> x instanceof BuffPorteur && x.duration != 0).forEach(x -> x.target.setCell(target.getFight().getCell(startCell)));

        return result;
    }

    public static int applyPushBackDamages(EffectCast castInfos, Fighter target, int Length, int currentLength) {
        /*final int damageCoef;
        if (target.getBuff().getAllBuffs().anyMatch(x -> x instanceof BuffMaximiseEffects)) {
            damageCoef = 7;
        } else if (castInfos.caster.getBuff().getAllBuffs().anyMatch(x -> x instanceof BuffMinimizeEffects)) {
            damageCoef = 4;
        } else {
            damageCoef = 4 + EffectPush.RANDOM_PUSHDAMAGE.nextInt(3);
        }

        double levelCoef = castInfos.caster.getLevel() / 50;
        if (levelCoef < 0.1) {
            levelCoef = 0.1;
        }*/
        double pushDmg = (((castInfos.caster.getLevel() / 2) + (castInfos.caster.getStats().getTotal(StatsEnum.ADD_PUSH_DAMAGES_BONUS) - target.getStats().getTotal(StatsEnum.ADD_PUSH_DAMAGES_REDUCTION)) + 32)) * (castInfos.effect == null ? 1 : castInfos.effect.diceNum) / (4 * Math.pow(2, currentLength));
        final MutableInt damageValue = new MutableInt(pushDmg);
        //MutableInt damageValue = new MutableInt(Math.floor(DamageCoef * LevelCoef) * (Length - CurrentLength + 1));

        final EffectCast subInfos = new EffectCast(StatsEnum.DAMAGE_BRUT, castInfos.spellId, castInfos.cellId, 0, null, castInfos.caster, null, false, StatsEnum.NONE, 0, null);
        subInfos.isTrap = castInfos.isTrap;

        return EffectDamage.applyDamages(subInfos, target, damageValue);
    }

}
