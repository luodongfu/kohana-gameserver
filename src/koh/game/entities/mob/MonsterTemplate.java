package koh.game.entities.mob;

import koh.look.EntityLookParser;
import koh.protocol.types.game.look.EntityLook;

/**
 *
 * @author Neo-Craft
 */
public class MonsterTemplate {

    public int Id;
    public String nameId;
    public int gfxId, race;
    public MonsterGrade[] grades;
    public String look;
    public boolean useSummonSlot, useBombSlot, canPlay;
    public boolean canTackle, isBoss;
    public MonsterDrop[] drops;
    public int[] subareas, spells;
    public int favoriteSubareaId;
    public boolean isMiniBoss, isQuestMonster;
    public int correspondingMiniBossId, speedAdjust, creatureBoneId;
    public boolean canBePushed, fastAnimsFun, canSwitchPos;
    public int[] incompatibleIdols;

    private EntityLook myEntityLook;

    public MonsterGrade getLevelOrNear(int Level) {
        int Near = 10000;
        MonsterGrade objNear = null;
        for (MonsterGrade objLevel : grades) {
            if (objLevel.Grade == Level) {
                return objLevel;
            } else {
                int Diff = Math.abs(objLevel.Grade - Level);
                if (Near > Diff) {
                    Near = Diff;
                    objNear = objLevel;
                }
            }
        }
        return objNear;
    }


    public EntityLook getEntityLook() {
        if (myEntityLook == null) {
            myEntityLook = EntityLookParser.fromString(this.look);
        }
        return myEntityLook;
    }

}
