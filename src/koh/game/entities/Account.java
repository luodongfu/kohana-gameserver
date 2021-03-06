package koh.game.entities;

import koh.game.dao.DAO;
import koh.game.entities.actors.Player;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import koh.game.network.WorldClient;
import koh.inter.messages.PlayerComingMessage;
import koh.protocol.types.game.character.choice.CharacterBaseInformation;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author Neo-Craft
 */
public class Account {

    public Account() {

    }

    public Account(PlayerComingMessage message) {
        id = message.accountId;
        nickName = message.nickname;
        //right = message.rights;
        secretQuestion = message.secretQuestion;
        secretAnswer = message.secretAnswer;
        lastIP = message.lastAddress == null ? "" : message.lastAddress;
        last_login = message.lastLogin;
    }

    @Getter
    public int id;
    public String nickName;
    public String secretQuestion, secretAnswer, lastIP, currentIP;
    public Timestamp last_login;
    public ArrayList<Player> characters;
    public AccountData accountData;
    public Player currentCharacter = null;
    @Getter @Setter
    private WorldClient client;

    public List<CharacterBaseInformation> toBaseInformations() {

        return characters.stream().map(Player::toBaseInformations).collect(Collectors.toList());
    }

    public Stream<Player> getCharacters() {
        return characters.stream();
    }
    
    public Player getPlayerInFight(){
        return characters.stream().filter(x -> x.getFighter() != null && x.getFight() != null).findAny().orElse(null);
    }

    public Player getPlayer(int id) {
        return characters.stream().filter(x -> x.getID() == id).findFirst().orElse(null);
    }

    public boolean remove(int id){
        return characters.removeIf(c -> c.getID() == id);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public void continueClear() {
        try {
            accountData = null;
            id = 0;
            nickName = null;
            secretQuestion = null;
            secretAnswer = null;
            lastIP = null;
            last_login = null;
            characters.clear();
            characters = null;
            currentCharacter = null;
            this.finalize();
        } catch (Throwable tr) {
        }
    }

    public synchronized void totalClear() {
        if (this.accountData != null && accountData.columsToUpdate != null) {
            if (accountData.columsToUpdate != null) {
                DAO.getAccountDatas().save(this.accountData,this);
            } else {
                accountData.totalClear(this);
            }
        } else {
            this.continueClear();
        }
    }

}
