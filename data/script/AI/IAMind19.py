#
#		Bloqueuse IA Mind
#		@author: alleos13
#		@date: 08/06/2013
from koh.game.entities.mob import IAMind
from koh.game.fights.AI import AIProcessor

class IAMind19(IAMind):

    def play(self,IA):
        if IA.getUsedNeurons() == 1:
            IA.initCells()
            IA.rescueTree()
            IA.moveToEnnemy()
        elif IA.getUsedNeurons() == 2:
            IA.initCells()
            IA.rescueTree()
            IA.moveToEnnemy()
        else:
            IA.stop()
