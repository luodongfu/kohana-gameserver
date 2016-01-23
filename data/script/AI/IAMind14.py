#
#		IA Mind of "Prespic"
#		@author: alleos13
#		@date: 08/06/2013
from koh.game.entities.mob import IAMind
from koh.game.fights.AI import AIProcessor

class IAMind14(IAMind):

    def play(self,IA):
        if IA.getUsedNeurons() == 1:
            IA.initCells()
            IA.support()
        elif IA.getUsedNeurons() == 2:
            IA.initCells()
            IA.attack()
        elif IA.getUsedNeurons() == 3:
            IA.initCells()
            IA.moveFar(4)
        elif IA.getUsedNeurons() == 4:
            IA.initCells()
            IA.moveFar(4)
        else:
            IA.stop()
