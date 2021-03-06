package koh.game.entities.item.animal;

import java.time.Instant;
import java.util.List;

import koh.game.dao.DAO;
import koh.game.entities.item.InventoryItem;
import koh.protocol.types.game.data.items.ObjectEffect;
import koh.protocol.types.game.data.items.effects.*;
import koh.protocol.types.game.mount.MountClientData;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.mina.core.buffer.IoBuffer;

/**
 *
 * @author Neo-Craft
 */
public class MountInventoryItem extends InventoryItem {

    protected boolean myInitialized = false;
    @Getter
    private MountInventoryItemEntity entity;
    @Getter
    private MountClientData mount;

    public MountInventoryItem(int ID, int templateId, int position, int owner, int quantity, List<ObjectEffect> effects, boolean create) {
        super(ID, templateId, position, owner, quantity, effects);
        if (!create) {
            this.entity = DAO.getMountInventories().get(((ObjectEffectMount) this.getEffect(995)).mountId);
            if (this.entity != null) {
                this.initialize();
            }
        }
        if (create) {
            this.removeEffect(995,998);
            this.effects.add(new ObjectEffectMount(995, (double) Instant.now().toEpochMilli(), 10, 0));
            this.effects.add(new ObjectEffectDuration(998, 37, (byte) 0, (byte) 0));
        }
        if (this.entity == null) {
            this.entity = new MountInventoryItemEntity();
            this.entity.animalID = DAO.getMountInventories().nextId();
            this.entity.lastEat = (System.currentTimeMillis() - (24 * 3600 * 1000)) + "";
            this.mount = new MountClientData();
            this.mount.ownerId = owner;
            this.mount.energy = this.mount.energyMax = this.mount.loveMax = this.mount.reproductionCountMax = this.mount.reproductionCountMax = this.mount.serenityMax = 10000;
            this.mount.maxPods = 3000; //TODO: mount inventory
            this.mount.experience = 0;
            this.mount.experienceForLevel = DAO.getExps().getLevel(1).getMount();
            this.mount.experienceForNextLevel = (double) DAO.getExps().getLevel(2).getMount();
            this.mount.id = (double) this.entity.animalID;
            this.mount.isRideable = true;
            this.mount.level = DAO.getSettings().getIntElement("World.ID") == 1 ? (byte) 100 : 1;
            this.mount.model = DAO.getMounts().findTemplate(this.templateId).getId();
            this.mount.effectList = DAO.getMounts().getMountByEffect(this.mount.model, this.mount.level);
            this.serializeInformations();

            this.removeEffect(995);
            this.removeEffect(998);

            this.effects.add(new ObjectEffectMount(995, (double) Instant.now().toEpochMilli(),DAO.getMounts().findTemplate(this.templateId).getId(), this.entity.animalID));
            this.effects.add(new ObjectEffectDuration(998, 37, (byte) 0, (byte) 0));
            this.notifyColumn("effects");

            DAO.getMountInventories().insert(this.entity);
        }
    }

    public void addExperience(long amount) {
        this.mount.experience += amount;

        while (this.mount.experience >= DAO.getExps().getLevel(this.mount.level + 1).getMount() && this.mount.level < 100) {
            levelUp();
        }
        this.save();
    }

    public void levelUp() {
        this.mount.level++;
        this.mount.effectList = ArrayUtils.removeAll(this.mount.effectList);
        this.mount.effectList = DAO.getMounts().getMountByEffect(this.mount.model, this.mount.level);
    }

    public void save() {
        this.serializeInformations();
        DAO.getMountInventories().update(entity);
    }

    public void serializeInformations() {
        IoBuffer buf = IoBuffer.allocate(0xFFF)
                .setAutoExpand(true);
        this.mount.serialize(buf);
        this.entity.informations = buf.flip().array();
    }

    public synchronized void initialize() {
        if (myInitialized) {
            return;
        }
        IoBuffer buf = IoBuffer.wrap(this.entity.informations);
        this.mount = new MountClientData();
        this.mount.deserialize(buf);

        this.myInitialized = true;
    }

    @Override
    public void totalClear() {
        //this.mount.clear();
        this.mount = null;
        this.myInitialized = false;
        this.entity.totalClear();
        this.entity = null;
        super.totalClear();
    }

}
