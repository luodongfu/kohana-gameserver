package koh.game.entities.item.actions;

import koh.game.actions.GameActionTypeEnum;
import koh.game.entities.actors.Player;
import koh.game.entities.item.ItemAction;

/**
 * Created by Melancholia on 12/13/15.
 */
public class AddSpellPoint extends ItemAction {

    private int spellPoints;

    public AddSpellPoint(String[] args, String criteria, int template) {
        super(args, criteria, template);
        this.spellPoints = Integer.parseInt(args[0]);
    }

    @Override
    public boolean execute(Player possessor,Player p, int cell) {
        if(!super.execute(possessor,p, cell) || !p.getClient().canGameAction(GameActionTypeEnum.CHANGE_MAP))
            return false;
        p.setSpellPoints(p.getSpellPoints() + spellPoints);
        p.refreshStats();
        return true;
    }
}
