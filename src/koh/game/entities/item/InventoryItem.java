package koh.game.entities.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import koh.game.Main;
import koh.game.dao.DAO;
import koh.game.entities.actors.Player;
import koh.game.entities.actors.character.GenericStats;
import koh.game.entities.item.animal.MountInventoryItem;
import koh.game.entities.item.animal.PetsInventoryItem;
import koh.protocol.client.enums.CharacterInventoryPositionEnum;
import koh.protocol.client.enums.ItemSuperTypeEnum;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.data.items.ObjectEffect;
import koh.protocol.types.game.data.items.ObjectItem;
import koh.protocol.types.game.data.items.effects.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.mina.core.buffer.IoBuffer;

/**
 *
 * @author Neo-Craft
 */
public class InventoryItem {

    public int ID;
    public int templateId;
    private int Position;
    private int Owner;
    private int Quantity;
    public List<ObjectEffect> effects; //FIXME : Think if we should migrate to Array or not , trought newArray = ArraysUtils.add(T[] Array,T Element);
    public boolean needInsert, NeedRemove;
    public List<String> columsToUpdate = null;

    public InventoryItem() {

    }

    private GenericStats myStats;

    public static InventoryItem getInstance(int ID, int TemplateId, int Position, int Owner, int Quantity, List<ObjectEffect> Effects) {
        if (DAO.getItemTemplates().getTemplate(TemplateId).getSuperType() == ItemSuperTypeEnum.SUPERTYPE_PET) {
            return new PetsInventoryItem(ID, TemplateId, Position, Owner, Quantity, Effects, !Effects.stream().anyMatch(x -> x.actionId == 995));
        } else if (DAO.getItemTemplates().getTemplate(TemplateId).typeId == 97) {
            return new MountInventoryItem(ID, TemplateId, Position, Owner, Quantity, Effects, !Effects.stream().anyMatch(x -> x.actionId == 995));
        } else {
            return new InventoryItem(ID, TemplateId, Position, Owner, Quantity, Effects);
        }
    }

    public InventoryItem(int ID, int TemplateId, int Position, int Owner, int Quantity, List<ObjectEffect> Effects) {
        this.ID = ID;
        this.templateId = TemplateId;
        this.Position = Position;
        this.Owner = Owner;
        this.Quantity = Quantity;
        this.effects = Effects;
    }

    public ObjectItem getObjectItem(int WithQuantity) {
        return new ObjectItem(this.Position, this.templateId, effects.stream().filter(Effect -> this.getTemplate().isVisibleInTooltip(Effect.actionId)).toArray(ObjectEffect[]::new), this.ID, WithQuantity);
    }

    public ObjectItem getObjectItem() {
        return new ObjectItem(this.Position, this.templateId, effects.stream().filter(Effect -> this.getTemplate().isVisibleInTooltip(Effect.actionId)).toArray(ObjectEffect[]::new), this.ID, this.Quantity);
    }

    public ItemSuperTypeEnum getSuperType() {
        return ItemSuperTypeEnum.valueOf(DAO.getItemTemplates().getType(getTemplate().typeId).superType);
    }

    public boolean isEquiped() {
        return this.Position != 63;
    }

    public boolean isLinked() { //928 = Lié 983 = Non echangeable
        return this.hasEffect(982) || this.hasEffect(983) || this.getSuperType() == ItemSuperTypeEnum.SUPERTYPE_QUEST || this.isTokenItem();
    }

    public boolean isLivingObject() {
        return this.getTemplate().typeId == 113 || (this.getEffect(970) != null && this.getEffect(971) != null);
    }

    public short getApparrance() {
        ObjectEffectInteger effect = (ObjectEffectInteger) this.getEffect(972);
        if (effect == null) {
            return this.getTemplate().appearanceId;
        } else {
            ObjectEffectInteger type = (ObjectEffectInteger) this.getEffect(970);
            if (type == null) {
                return this.getTemplate().appearanceId;
            }
            return (short) ItemLivingObject.getObviAppearanceBySkinId(effect.value, type.value);
        }
    }

    public int getPosition() {
        return Position;
    }

    public void setPosition(int i) {
        this.Position = i;
        this.notifyColumn("position");
    }

    public int getQuantity() {
        return Quantity;
    }

    public void SetQuantity(int i) {
        this.Quantity = i;
        this.notifyColumn("stack");
    }

    public int getOwner() {
        return Owner;
    }

    public void setOwner(int i) {
        this.Owner = i;
        this.notifyColumn("owner");
    }

    public void notifyColumn(String C) {
        if (this.columsToUpdate == null) {
            this.columsToUpdate = new ArrayList<>();
        }
        if (!this.columsToUpdate.contains(C)) {
            this.columsToUpdate.add(C);
        }
    }

    public boolean hasEffect(int id) {
        return this.effects.stream().anyMatch(x -> x.actionId == id);
    }

    public ObjectEffect getEffect(int id) {
        return this.effects.stream().filter(x -> x.actionId == id).findFirst().orElse(null);
    }

    public ItemTemplate getTemplate() {
        return DAO.getItemTemplates().getTemplate(templateId);
    }

    public ItemType getItemType() {
        return DAO.getItemTemplates().getType(getTemplate().typeId);
    }

    public Weapon getWeaponTemplate() {
        return (Weapon) DAO.getItemTemplates().getTemplate(templateId);
    }

    public CharacterInventoryPositionEnum getSlot() {
        return CharacterInventoryPositionEnum.valueOf((byte) this.Position);
    }

    public void removeEffect(int... id) {
        for (int i : id) {
            this.removeEffect(i);
        }
    }

    private void removeEffect(int id) {
        if (this.effects.removeIf(x -> x.actionId == id)) {
            this.notifyColumn("effects");
        }
    }

    public List<ObjectEffect> getEffects() {
        this.notifyColumn("effects");
        return effects;
    }

    public List<ObjectEffect> getEffectsCopy() {
        List<ObjectEffect> effects = new ArrayList<>();
        this.effects.stream().forEach((e) -> { //TODO: Parralel Stream
            effects.add(e.Clone());
        });
        return effects;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int getWeight() {
        return this.getTemplate().realWeight * this.Quantity;
    }

    public void setPosition(CharacterInventoryPositionEnum Slot) {
        this.Position = Slot.value();
        this.notifyColumn("position");
    }

    public boolean Equals(Collection<ObjectEffect> Effects) {
        ObjectEffect Get = null;
        if (Effects.size() != this.effects.size()) {
            return false;
        }
        for (ObjectEffect e : Effects) {
            Get = this.getEffect(e.actionId);
            if (Get == null || !Get.equals(e)) {
                return false;
            }
        }
        return true;
    }

    public static List<ObjectEffect> deserializeEffects(byte[] binary) {
        IoBuffer buf = IoBuffer.wrap(binary);
        int len = buf.getInt();
        List<ObjectEffect> Effects = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            Effects.add(ObjectEffect.Instance(buf.getInt()));
            Effects.get(i).deserialize(buf);
        }
        buf.clear();
        return Effects;
    }

    public IoBuffer serializeEffectInstanceDice() {
        IoBuffer buff = IoBuffer.allocate(2535);
        buff.setAutoExpand(true);

        buff.putInt(effects.size());
        for (ObjectEffect e : effects) {
            buff.putInt(e.getTypeId());
            e.serialize(buff);
        }

        buff.flip();
        return buff;
    }

    public boolean areConditionFilled(Player character) {
        try {
            if (this.getTemplate().getCriteriaExpression() == null) {
                return true;
            } else {
                return this.getTemplate().getCriteriaExpression().Eval(character);
            }
        } catch (Exception e) {
            Main.Logs().writeError(String.format("Bugged item %s Condition %s", this.getTemplate().id, this.getTemplate().criteria));
            e.printStackTrace();
            return false;
        }
    }

    public boolean isWeapon() {
        return DAO.getItemTemplates().getTemplate(templateId) instanceof Weapon;
    }

    private void parseStats() {
        this.myStats = new GenericStats();

        StatsEnum Stat;
        for (ObjectEffect e : this.effects) {
            if (e instanceof ObjectEffectInteger) {
                Stat = StatsEnum.valueOf(e.actionId);
                if (Stat == null) {
                    Main.Logs().writeError("Undefinied Stat id " + e.actionId);
                    continue;
                }
                this.myStats.addItem(Stat, ((ObjectEffectInteger) e).value);
            }
        }
        Stat = null;
    }

    public GenericStats getStats() {
        if (this.myStats == null) {
            this.parseStats();
            if (this.getTemplate() instanceof Weapon) {
                this.getWeaponTemplate().initialize();
            }
        }
        return myStats;
    }

    public void totalClear() {
        try {
            ID = 0;
            templateId = 0;
            Position = 0;
            Owner = 0;
            Quantity = 0;
            /*for (ObjectEffect e : effects) {
             e.totalClear();
             }*/
            effects.clear();
            effects = null;
            needInsert = false;
            NeedRemove = false;
            columsToUpdate.clear();
            columsToUpdate = null;
            if (this.myStats == null) {
                myStats.totalClear();
                myStats = null;
            }
            this.finalize();
        } catch (Throwable tr) {
        }
    }

    private boolean isTokenItem() {
        return false;
    }

}
