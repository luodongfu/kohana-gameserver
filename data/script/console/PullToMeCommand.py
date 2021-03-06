from koh.game.entities.command import PlayerCommand
from koh.game.actions import GameActionTypeEnum;
from koh.game.network import WorldClient;
from koh.game.dao import DAO;
from koh.protocol.messages.authorized import ConsoleMessage;
from koh.game.entities.actors import Player

class PullToMeCommand(PlayerCommand):

    def getDescription(self):
        return "Telpeort the player arg1 to me";

    def apply(self,client,args):
        target = DAO.getPlayers().getCharacter(args[0]);
        if target is None:
            client.send(ConsoleMessage(0, "The target is missing"));
        else:
            target.teleport(client.getMapid(),client.getCell().getId());

    def can(self,client):
        return True;

    def roleRestrained(self):
        return 1;

    def argsNeeded(self):
        return 1;
