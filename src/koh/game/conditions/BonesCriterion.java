package koh.game.conditions;

import koh.game.entities.actors.Player;

/**
 *
 * @author Neo-Craft
 */
public class BonesCriterion extends Criterion {

    public static final String Identifier = "PU";
    public short BonesId;

    @Override
    public void Build() {
        if (this.literal.equalsIgnoreCase("B")) {
            this.BonesId = (short) 1;
        } else {
            short result;
            try {
                result = Short.parseShort(literal);
            } catch (Exception e) {
                throw new Error(String.format("Cannot build BonesCriterion, {0} is not a valid bones id", this.literal));
            }
            this.BonesId = (int) result != 0 ? result : (short) 1;
        }
    }

    @Override
    public boolean eval(Player character) {
        return this.Compare((Comparable<Short>) character.getEntityLook().bonesId, this.BonesId);
    }

    @Override
    public String toString() {
        return this.FormatToString("PU");
    }

}
