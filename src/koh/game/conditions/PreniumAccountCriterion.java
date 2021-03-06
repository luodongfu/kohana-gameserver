package koh.game.conditions;

import koh.game.entities.actors.Player;

/**
 *
 * @author Neo-Craft
 */
public class PreniumAccountCriterion extends Criterion {

    public static String Identifier = "Pe";
    public boolean IsPrenium;

    @Override
    public String toString() {
        return this.FormatToString("Pe");
    }

    @Override
    public void Build() {
        this.IsPrenium = Integer.parseInt(literal) == 1;
    }

    @Override
    public boolean eval(Player character) {
        return true;
    }
}
