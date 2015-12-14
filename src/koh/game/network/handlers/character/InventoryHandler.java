package koh.game.network.handlers.character;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import koh.game.actions.GameActionTypeEnum;
import koh.game.entities.actors.Player;
import koh.game.entities.item.InventoryItem;
import koh.game.entities.item.ItemLivingObject;
import koh.game.entities.item.animal.PetsInventoryItem;
import koh.game.network.WorldClient;
import koh.game.network.handlers.HandlerAttribute;
import koh.protocol.client.enums.CharacterInventoryPositionEnum;
import koh.protocol.client.enums.ObjectErrorEnum;
import koh.protocol.client.enums.TextInformationTypeEnum;
import koh.protocol.messages.connection.BasicNoOperationMessage;
import koh.protocol.messages.game.basic.TextInformationMessage;
import koh.protocol.messages.game.inventory.InventoryWeightMessage;
import koh.protocol.messages.game.inventory.items.*;
import koh.protocol.types.game.data.items.ObjectEffect;
import koh.protocol.types.game.data.items.effects.ObjectEffectDate;
import koh.protocol.types.game.data.items.effects.ObjectEffectInteger;

/**
 *
 * @author Neo-Craft
 */
public class InventoryHandler {

    @HandlerAttribute(ID = ObjectUseMultipleMessage.MESSAGE_ID)
    public static void handleObjectUseMultipleMessage(WorldClient client,ObjectUseMultipleMessage message){
        if (client.isGameAction(GameActionTypeEnum.FIGHT)) {
            client.send(new BasicNoOperationMessage());
            return;
        }
        InventoryItem item = client.character.inventoryCache.find(message.objectUID);
        if(item.getQuantity() < message.quantity || item == null || !item.areConditionFilled(client.character)){
            client.send(new ObjectErrorMessage(ObjectErrorEnum.CANNOT_DESTROY));
            return;
        }
        int i = 0;
        for(i = 0; message.quantity > i ; i++){
            if(!item.getTemplate().use(client.character)){
                client.send(new ObjectErrorMessage(ObjectErrorEnum.CANNOT_DESTROY));
                break;
            }
        }
        client.character.inventoryCache.safeDelete(item, i);
    }

    @HandlerAttribute(ID = ObjectUseOnCharacterMessage.MESSAGE_ID)
    public static void handleObjectUseOnCharacterMessage(WorldClient client,ObjectUseOnCharacterMessage message){
        if (client.isGameAction(GameActionTypeEnum.FIGHT)) {
            client.send(new BasicNoOperationMessage());
            return;
        }
        Player target = client.character.currentMap.getPlayer(message.characterId);
        InventoryItem item = client.character.inventoryCache.find(message.objectUID);

        if(target == null || item == null || !item.areConditionFilled(target) || item.getTemplate().use(target)){
            client.send(new ObjectErrorMessage(ObjectErrorEnum.CANNOT_DESTROY));
            return;
        }
        client.character.inventoryCache.safeDelete(item,1);

    }

    @HandlerAttribute(ID = ObjectUseMessage.MESSAGE_ID)
    public static void handleObjectUseMessage(WorldClient client,ObjectUseMessage message){
        if (client.isGameAction(GameActionTypeEnum.FIGHT)) {
            client.send(new BasicNoOperationMessage());
            return;
        }
        InventoryItem item = client.character.inventoryCache.find(message.objectUID);
        if(!client.character.inventoryCache.hasItemId(message.objectUID) || item == null || !item.areConditionFilled(client.character) || item.getTemplate().use(client.character)){
            client.send(new ObjectErrorMessage(ObjectErrorEnum.CANNOT_DESTROY));
            return;
        }
        client.character.inventoryCache.safeDelete(item,1);
    }

    @HandlerAttribute(ID = ObjectDeleteMessage.MESSAGE_ID)
    public static void HandleObjectDeleteMessage(WorldClient Client, ObjectDeleteMessage Message) {
        if (Client.isGameAction(GameActionTypeEnum.FIGHT)) {
            Client.send(new BasicNoOperationMessage());
            return;
        }
        Client.character.inventoryCache.safeDelete(Client.character.inventoryCache.find(Message.objectUID), Message.quantity);

    }

    @HandlerAttribute(ID = ObjectSetPositionMessage.MESSAGE_ID)
    public synchronized static void HandleObjectSetPositionMessage(WorldClient Client, ObjectSetPositionMessage Message) {
        if (Client.isGameAction(GameActionTypeEnum.FIGHT)) {
            Client.send(new BasicNoOperationMessage());
            return;
        }
        Client.character.inventoryCache.moveItem(Message.objectUID, CharacterInventoryPositionEnum.valueOf(Message.position), Message.quantity);
    }

    @HandlerAttribute(ID = LivingObjectMessageRequestMessage.MESSAGE_ID)
    public static void HandleLivingObjectMessageRequestMessage(WorldClient Client, LivingObjectMessageRequestMessage Message) {
        if (Client.isGameAction(GameActionTypeEnum.FIGHT)) {
            Client.send(new BasicNoOperationMessage());
            return;
        }
        InventoryItem Item = Client.character.inventoryCache.find(Message.livingObject);
        if (Item == null) {
            Client.send(new BasicNoOperationMessage());
            return;
        }
        //int msgId, int timeStamp, String owner, int objectGenericId
        Client.send(new LivingObjectMessageMessage(Message.msgId, (int) Instant.now().getEpochSecond(), Client.character.nickName, Item.getID()));
        Client.send(new BasicNoOperationMessage());
    }

    @HandlerAttribute(ID = LivingObjectChangeSkinRequestMessage.MESSAGE_ID)
    public static void HandleLivingObjectChangeSkinRequestMessage(WorldClient Client, LivingObjectChangeSkinRequestMessage Message) {
        if (Client.isGameAction(GameActionTypeEnum.FIGHT)) {
            Client.send(new BasicNoOperationMessage());
            return;
        }
        InventoryItem Item = Client.character.inventoryCache.find(Message.livingUID);
        if (Item == null) {
            Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
            return;
        }
        ObjectEffectInteger obviXp = (ObjectEffectInteger) Item.getEffect(974), obviSkin = (ObjectEffectInteger) Item.getEffect(972);
        if (obviXp == null || obviSkin == null) {
            Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
            return;
        }
        if (Message.skinId > ItemLivingObject.getLevelByObviXp(obviXp.value)) {
            Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
            return;
        }
        if (Item.getSlot() != CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED && Item.getApparrance() != 0) {
            Client.character.inventoryCache.removeApparence(Item.getApparrance());
        }
        Item.removeEffect(972);
        Item.getEffects$Notify().add(((ObjectEffectInteger) obviSkin.Clone()).SetValue(Message.skinId));
        Client.send(new ObjectModifiedMessage(Item.getObjectItem()));
        if (Item.getSlot() != CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED && Item.getApparrance() != 0) {
            Client.character.inventoryCache.addApparence(Item.getApparrance());
            Client.character.refreshEntitie();
        }
        Client.character.send(new InventoryWeightMessage(Client.character.inventoryCache.getWeight(), Client.character.inventoryCache.getTotalWeight()));
        Client.send(new BasicNoOperationMessage());
    }

    @HandlerAttribute(ID = ObjectFeedMessage.MESSAGE_ID)
    public static void HandleObjectFeedMessage(WorldClient Client, ObjectFeedMessage Message) {
        if (Client.isGameAction(GameActionTypeEnum.FIGHT)) {
            Client.send(new BasicNoOperationMessage());
            return;
        }
        InventoryItem Item = Client.character.inventoryCache.find(Message.objectUID), Food = Client.character.inventoryCache.find(Message.foodUID);
        if (Item == null || Food == null || Food.getSlot() != CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED) {
            Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
            return;
        }
        if (Item instanceof PetsInventoryItem) {
            if (!((PetsInventoryItem) Item).eat(Client.character, Food)) {
                Client.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 53, new String[0]));
            } else {
                int newQua = Food.getQuantity() - 1;
                if (newQua <= 0) {
                    Client.character.inventoryCache.removeItem(Food);
                } else {
                    Client.character.inventoryCache.updateObjectquantity(Food, newQua);
                }
                Client.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 32, new String[0]));
            }

        } else if (Item.isLivingObject()) {
            ObjectEffectInteger obviXp = (ObjectEffectInteger) Item.getEffect(974), obviType = (ObjectEffectInteger) Item.getEffect(973), obviState = (ObjectEffectInteger) Item.getEffect(971), obviSkin = (ObjectEffectInteger) Item.getEffect(972), obviItem = (ObjectEffectInteger) Item.getEffect(970);
            ObjectEffectDate obviTime = (ObjectEffectDate) Item.getEffect(808);
            if (obviItem == null || obviType == null || obviType.value != Food.getTemplate().getTypeId() || obviTime == null || obviXp == null || obviState == null || obviSkin == null) {
                Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
                return;
            }

            int newqua = Food.getQuantity() - Message.foodQuantity;
            if (newqua < 0) {
                Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
                return;
            }
            int xp = Food.getTemplate().getLevel() / 2,
                    oldxp = obviXp.value,
                    state = obviState.value;
            if (newqua == 0) {
                Client.character.inventoryCache.removeItem(Food);
            } else {
                Client.character.inventoryCache.updateObjectquantity(Food, newqua);
            }
            Item.removeEffect(974);
            Item.getEffects$Notify().add(((ObjectEffectInteger) obviXp.Clone()).SetValue(oldxp + xp));
            if (state < 2) {
                Item.removeEffect(971);
                Item.getEffects$Notify().add(((ObjectEffectInteger) obviState.Clone()).SetValue(state + 1));
            }
            Item.removeEffect(808);
            Calendar now = Calendar.getInstance();
            Item.getEffects$Notify().add(((ObjectEffectDate) new ObjectEffectDate(obviTime.actionId, now.get(Calendar.YEAR), (byte) now.get(Calendar.MONTH), (byte) now.get(Calendar.DAY_OF_MONTH), (byte) now.get(Calendar.HOUR), (byte) now.get(Calendar.MINUTE))));

            Client.send(new ObjectModifiedMessage(Item.getObjectItem()));
            Client.character.send(new InventoryWeightMessage(Client.character.inventoryCache.getWeight(), Client.character.inventoryCache.getTotalWeight()));
        } else {
            Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
        }
        Client.send(new BasicNoOperationMessage());
    }

    @HandlerAttribute(ID = LivingObjectDissociateMessage.MESSAGE_ID)
    public static void HandleLivingObjectDissociateMessage(WorldClient Client, LivingObjectDissociateMessage Message) {
        if (Client.isGameAction(GameActionTypeEnum.FIGHT)) {
            Client.send(new BasicNoOperationMessage());
            return;
        }
        InventoryItem Item = Client.character.inventoryCache.find(Message.livingUID);
        if (Item == null) {
            Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
            return;
        }
        ObjectEffectInteger obviXp = (ObjectEffectInteger) Item.getEffect(974), obviType = (ObjectEffectInteger) Item.getEffect(973), obviState = (ObjectEffectInteger) Item.getEffect(971), obviSkin = (ObjectEffectInteger) Item.getEffect(972), obviTemplate = (ObjectEffectInteger) Item.getEffect(970);
        ObjectEffectDate obviTime = (ObjectEffectDate) Item.getEffect(808), exchangeTime = (ObjectEffectDate) Item.getEffect(983);
        if (obviTemplate == null || obviXp == null || obviType == null || obviState == null || obviSkin == null || obviTime == null) {
            Client.send(new ObjectErrorMessage(ObjectErrorEnum.LIVING_OBJECT_REFUSED_FOOD));
            return;
        }
        if (Item.getSlot() != CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED/* && item.getTemplate().appearanceId != 0*/) {
            Client.character.inventoryCache.removeApparence(Item.getApparrance());
        }
        Client.character.inventoryCache.tryCreateItem(obviTemplate.value, Client.character, 1, CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED.value(), new ArrayList<ObjectEffect>() {
            {
                add(obviTemplate.Clone());
                add(obviXp.Clone());
                add(obviTime.Clone());
                add(obviState.Clone());
                add(obviType.Clone());
                add(obviSkin.Clone());
                if (exchangeTime != null) {
                    add(exchangeTime.Clone());
                }
            }
        });

        Item.removeEffect(974);
        Item.removeEffect(973);
        Item.removeEffect(971);
        Item.removeEffect(972);
        Item.removeEffect(808);
        Item.removeEffect(983);
        Item.removeEffect(970);

        Client.send(new ObjectModifiedMessage(Item.getObjectItem()));
        if (Item.getSlot() != CharacterInventoryPositionEnum.INVENTORY_POSITION_NOT_EQUIPED && Item.getTemplate().getAppearanceId() != 0) {
            Client.character.inventoryCache.addApparence(Item.getApparrance());
            Client.character.refreshEntitie();
        }
        Client.character.send(new InventoryWeightMessage(Client.character.inventoryCache.getWeight(), Client.character.inventoryCache.getTotalWeight()));
    }

}
