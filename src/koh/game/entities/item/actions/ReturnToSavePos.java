package koh.game.entities.item.actions;

import koh.game.actions.GameActionTypeEnum;
import koh.game.entities.actors.Player;
import koh.game.entities.item.ItemAction;

/**
 * Created by Melancholia on 12/12/15.
 */
public class ReturnToSavePos  extends ItemAction {

    private int map;
    private short cell;

    public ReturnToSavePos(String[] args, String criteria) {
        super(args, criteria);
    }

    @Override
    public boolean execute(Player p) {
        if(!super.execute(p) || !p.client.canGameAction(GameActionTypeEnum.CHANGE_MAP))
            return false;
        p.teleport(p.savedMap, p.savedCell);
        return true;
    }
}
