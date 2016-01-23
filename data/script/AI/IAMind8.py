#
#		IA Mind of "Coffre"
#		@author: alleos13
#		@date: 08/06/2013
from koh.game.entities.mob import IAMind
from koh.game.fights.AI import AIProcessor

class IAMind8(IAMind):

    def play(self,IA):
        if IA.getUsedNeurons() == 1:
            IA.initCells()
            IA.buff()
        elif IA.getUsedNeurons() == 2:
            IA.initCells()
            IA.buff()
        elif IA.getUsedNeurons() == 3:
            IA.initCells()
            IA.buff()
        elif IA.getUsedNeurons() == 4:
            IA.initCells()
            IA.moveFar(4)
        else:
            IA.stop()
