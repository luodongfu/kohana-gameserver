package koh.game.exchange;

import koh.game.actions.GameActionTypeEnum;
import koh.game.dao.mysql.ItemTemplateDAOImpl;
import koh.game.entities.item.InventoryItem;
import koh.game.network.WorldClient;
import koh.protocol.client.Message;
import koh.protocol.client.enums.DialogTypeEnum;
import koh.protocol.client.enums.ExchangeTypeEnum;
import koh.protocol.messages.game.dialog.LeaveDialogMessage;
import koh.protocol.messages.game.inventory.exchanges.ExchangeStartedWithStorageMessage;
import koh.protocol.messages.game.inventory.storage.StorageInventoryContentMessage;
import koh.protocol.messages.game.inventory.storage.StorageKamasUpdateMessage;

/**
 *
 * @author Neo-Craft
 */
public class StorageExchange extends Exchange {

    private final WorldClient myClient;

    public StorageExchange(WorldClient Client) {
        this.myClient = Client;
        this.send(new ExchangeStartedWithStorageMessage(ExchangeTypeEnum.STORAGE, 2147483647));
        this.send(new StorageInventoryContentMessage(Client.getAccount().accountData.toObjectsItem(), Client.getAccount().accountData.kamas));
    }

    @Override
    public boolean moveItems(WorldClient Client, InventoryItem[] items, boolean add) {
        InventoryItem NewItem = null;
        if (add) {
            for (InventoryItem Item : items) {
                NewItem = InventoryItem.getInstance(ItemTemplateDAOImpl.nextStorageId++, Item.templateId, 63, Client.getAccount().id, Item.getQuantity(), Item.effects);
                if (Client.getAccount().accountData.add(Client.character, NewItem, true)) {
                    NewItem.needInsert = true;
                }
                Client.character.inventoryCache.updateObjectquantity(Item, 0);
            }
        } else {
            for (InventoryItem Item : items) {
                NewItem = InventoryItem.getInstance(ItemTemplateDAOImpl.nextId++, Item.templateId, 63, Client.character.ID, Item.getQuantity(), Item.effects);
                if (Client.character.inventoryCache.add(NewItem, true)) {
                    NewItem.needInsert = true;
                }
                Client.getAccount().accountData.updateObjectquantity(Client.character, Item, 0);
            }
        }
        return true;
    }

    @Override
    public boolean moveItem(WorldClient Client, int itemID, int quantity) {
        if (quantity == 0) {
            return false;
        } else if (quantity <= 0) { //Remove from Bank
            InventoryItem BankItem = Client.getAccount().accountData.itemscache.get(itemID);
            if (BankItem == null) {
                return false;
            }
            Client.getAccount().accountData.updateObjectquantity(Client.character, BankItem, BankItem.getQuantity() + quantity);
            InventoryItem Item = InventoryItem.getInstance(ItemTemplateDAOImpl.nextId++, BankItem.templateId, 63, Client.character.ID, -quantity, BankItem.effects);
            if (Client.character.inventoryCache.add(Item, true)) {
                Item.needInsert = true;
            }
        } else { //add In bank
            InventoryItem Item = Client.character.inventoryCache.itemsCache.get(itemID);
            if (Item == null) {
                return false;
            }
            Client.character.inventoryCache.updateObjectquantity(Item, Item.getQuantity() - quantity);
            InventoryItem NewItem = InventoryItem.getInstance(ItemTemplateDAOImpl.nextStorageId++, Item.templateId, 63, Client.getAccount().id, quantity, Item.effects);
            if (Client.getAccount().accountData.add(Client.character, NewItem, true)) {
                NewItem.needInsert = true;
            }
        }
        return true;
    }

    @Override
    public boolean moveKamas(WorldClient Client, int quantity) {
        if (quantity == 0) {
            return false;
        } else if (quantity < 0) {
            if (Client.getAccount().accountData.kamas + quantity < 0) {
                return false;
            }
            Client.getAccount().accountData.setBankKamas(Client.getAccount().accountData.kamas + quantity);
            Client.send(new StorageKamasUpdateMessage(Client.getAccount().accountData.kamas));
            Client.character.inventoryCache.substractKamas(quantity, false);
        } else {
            if (Client.character.kamas - quantity < 0) {
                return false;
            }
            Client.getAccount().accountData.setBankKamas(Client.getAccount().accountData.kamas + quantity);
            Client.send(new StorageKamasUpdateMessage(Client.getAccount().accountData.kamas));
            Client.character.inventoryCache.substractKamas(quantity, false);
        }
        return true;
    }

    @Override
    public boolean buyItem(WorldClient Client, int templateId, int quantity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean sellItem(WorldClient Client, InventoryItem item, int quantity) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean validate(WorldClient Client) {
        return false;
    }

    @Override
    public boolean finish() {
        this.myEnd = true;

        return true;
    }

    @Override
    public boolean closeExchange(boolean Success) {
        this.finish();
        this.myClient.myExchange = null;
        this.myClient.send(new LeaveDialogMessage(DialogTypeEnum.DIALOG_EXCHANGE));
        this.myClient.endGameAction(GameActionTypeEnum.EXCHANGE);

        return true;
    }

    @Override
    public void send(Message Packet) {
        this.myClient.send(Packet);
    }

    @Override
    public boolean transfertAllToInv(WorldClient Client, InventoryItem[] items) {
        return Client.myExchange.moveItems(Client, Client.getAccount().accountData.itemscache.values().toArray(new InventoryItem[Client.getAccount().accountData.itemscache.size()]), false);
    }

}
