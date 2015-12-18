package koh.game.fights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import koh.game.fights.effects.EffectBase;
import koh.game.fights.effects.EffectCast;
import koh.game.fights.effects.buff.BuffActiveType;
import koh.game.fights.effects.buff.BuffDecrementType;
import koh.game.fights.effects.buff.BuffEffect;
import koh.game.fights.effects.buff.BuffState;
import koh.protocol.messages.game.actions.fight.GameActionFightDispellableEffectMessage;
import koh.utils.Couple;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neo-Craft
 */
public class FighterBuff {

    public List<Couple<EffectCast, Integer>> delayedEffects = new CopyOnWriteArrayList<>();
    private static final Logger logger = LogManager.getLogger(FighterBuff.class);

    private HashMap<BuffActiveType, ArrayList<BuffEffect>> buffsAct = new HashMap<BuffActiveType, ArrayList<BuffEffect>>() {
        {
            this.put(BuffActiveType.ACTIVE_ATTACKED_AFTER_JET, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_ATTACKED_POST_JET, new ArrayList<>());
             this.put(BuffActiveType.ACTIVE_ATTACKED_POST_JET_TRAP, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_ATTACK_AFTER_JET, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_ATTACK_POST_JET, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_HEAL_AFTER_JET, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_BEGINTURN, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_ENDTURN, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_ENDMOVE, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_STATS, new ArrayList<>());
            this.put(BuffActiveType.ACTIVE_ON_DIE, new ArrayList<>());
        }
    };

    private HashMap<BuffDecrementType, ArrayList<BuffEffect>> buffsDec = new HashMap<BuffDecrementType, ArrayList<BuffEffect>>() {
        {
            this.put(BuffDecrementType.TYPE_BEGINTURN, new ArrayList<>());
            this.put(BuffDecrementType.TYPE_ENDTURN, new ArrayList<>());
            this.put(BuffDecrementType.TYPE_ENDMOVE, new ArrayList<>());

        }
    };

    public Stream<BuffEffect> getAllBuffs() {
        return Stream.concat(Stream.concat(this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN).stream(), this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN).stream()), this.buffsDec.get(BuffDecrementType.TYPE_ENDMOVE).stream());
    }

    public boolean buffMaxStackReached(BuffEffect Buff) { //CLEARCODE : Distinct state ?
        return Buff.CastInfos.SpellLevel != null && Buff.CastInfos.SpellLevel.getMaxStack() > 0
                && Buff.CastInfos.SpellLevel.getMaxStack()
                <= (Buff instanceof BuffState
                        ? this.getAllBuffs().filter(x -> x.CastInfos.SpellId == Buff.CastInfos.SpellId && x instanceof BuffState && ((BuffState) x).CastInfos.Effect.value == Buff.CastInfos.Effect.value).count()
                        : this.getAllBuffs().filter(x -> x.CastInfos.SpellId == Buff.CastInfos.SpellId && x.CastInfos.EffectType == Buff.CastInfos.EffectType).count());
    }

    public void addBuff(BuffEffect buff) {
        /*if (Buff.Delay > 0) {
         this.buffsDec.get(BuffDecrementType.TYPE_ENDDELAY).add(Buff);
         return;
         }*/
        if (buffMaxStackReached(buff)) {  //Vue que ces effets s'activent auto à leur lancement
            logger.debug("Buff {} canceled due to stack",buff.getClass().getName());
            return;
        }
        this.buffsAct.get(buff.ActiveType).add(buff);
        this.buffsDec.get(buff.DecrementType).add(buff);
        buff.Target.fight.sendToField(new GameActionFightDispellableEffectMessage(/*Buff.CastInfos.Effect.effectId*/buff.CastInfos.EffectType.value(), buff.caster.getID(), buff.getAbstractFightDispellableEffect()));
        logger.debug("Buff {} added",buff,getClass().getName());
    }

    //Le -1 definie l'infini
    public int beginTurn() {
        MutableInt Damage = new MutableInt(0);
        for (Couple<EffectCast, Integer> EffectCast : this.delayedEffects) {
            EffectCast.second--;
            if (EffectCast.second <= 0) {
                this.delayedEffects.remove(EffectCast);
                EffectCast.first.Targets.removeIf(Fighter -> !Fighter.isAlive());
                if (EffectBase.TryApplyEffect(EffectCast.first) == -3) {
                    return -3;
                }
            }
        }

        /*for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_ENDDELAY)) {
         Buff.Delay--;
         if (Buff.Delay <= 0) {
         this.buffsDec.get(BuffDecrementType.TYPE_ENDDELAY).remove(Buff);
         if (buffMaxStackReached(Buff)) {
         continue;
         }
         this.buffsAct.get(Buff.ActiveType).add(Buff);
         this.buffsDec.get(Buff.DecrementType).add(Buff);
         Buff.Target.fight.sendToField(new GameActionFightDispellableEffectMessage(Buff.CastInfos.EffectType.value(), Buff.caster.id, Buff.getAbstractFightDispellableEffect()));

         }
         }*/
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_BEGINTURN)) {
            if (Buff.applyEffect(Damage, null) == -3) {
                return -3;
            }
        }

        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN)) {
            if (Buff.Duration != -1 && Buff.decrementDuration() <= 0) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN).removeIf(x -> x.Duration <= 0 && x.Duration != -1);

        this.buffsAct.values().stream().forEach((BuffList) -> {
            BuffList.removeIf(Buff -> Buff.DecrementType == BuffDecrementType.TYPE_BEGINTURN && Buff.Duration <= 0);
        });

        return -1;
    }

    public int endTurn() {
        MutableInt Damage = new MutableInt(0);
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_ENDTURN)) {
            if (Buff.applyEffect(Damage, null) == -3) {
                return -3;
            }
        }

        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN)) {
            if (Buff.Duration != -1 && Buff.decrementDuration() <= 0) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN).removeIf(x -> x.Duration <= 0 && x.Duration != -1);

        for (ArrayList<BuffEffect> BuffList : this.buffsAct.values()) {
            BuffList.removeIf(Buff -> Buff.DecrementType == BuffDecrementType.TYPE_ENDTURN && Buff.Duration <= 0);
        }

        return -1;
    }

    public int endMove() {
        MutableInt Damage = new MutableInt(0);
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_ENDMOVE)) {
            if (Buff.applyEffect(Damage, null) == -3) {
                return -3;
            }
        }

        this.buffsAct.get(BuffActiveType.ACTIVE_ENDMOVE).removeIf(x -> x.DecrementType == BuffDecrementType.TYPE_ENDMOVE && x.Duration == 0);

        return -1;
    }

    /// <summary>
    /// Lance un soin, activation des buffs d'attaque avant le calcul du jet avec les statistiques
    /// </summary>
    /// <param name="CastInfos"></param>
    /// <param name="DamageValue"></param>
    public int onHealPostJet(EffectCast CastInfos, MutableInt DamageValue) {
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_HEAL_AFTER_JET)) {
            if (Buff.applyEffect(DamageValue, CastInfos) == -3) {
                return -3;
            }
        }

        return -1;
    }

    /// <summary>
    /// Lance une attaque, activation des buffs d'attaque avant le calcul du jet avec les statistiques
    /// </summary>
    /// <param name="CastInfos"></param>
    /// <param name="DamageValue"></param>
    public int onAttackPostJet(EffectCast CastInfos, MutableInt DamageValue) {
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_ATTACK_POST_JET)) {
            if (Buff.applyEffect(DamageValue, CastInfos) == -3) {
                return -3;
            }
        }

        return -1;
    }

    /// <summary>
    /// Lance une attaque, activation des buffs d'attaque apres le calcul du jet avec les statistiques
    /// </summary>
    /// <param name="CastInfos"></param>
    /// <param name="DamageValue"></param>
    public int onAttackAfterJet(EffectCast CastInfos, MutableInt DamageValue) {
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_ATTACK_AFTER_JET)) {
            if (Buff.applyEffect(DamageValue, CastInfos) == -3) {
                return -3;
            }
        }
        return -1;
    }

    /// Subit des dommages, activation des buffs de reduction, renvois, anihilation des dommages avant le calcul du jet
    /// </summary>
    /// <param name="CastInfos"></param>
    /// <param name="DamageValue"></param>
    public int onAttackedPostJet(EffectCast CastInfos, MutableInt DamageValue) {
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_ATTACKED_POST_JET)) {
            if (Buff.applyEffect(DamageValue, CastInfos) == -3) {
                return -3;
            }
        }
        return -1;
    }
    
     /// Subit des dommages, activation des buffs de reduction, renvois, anihilation des dommages avant le calcul du jet
    /// </summary>
    /// <param name="CastInfos"></param>
    /// <param name="DamageValue"></param>
    public int onAttackedPostJetTrap(EffectCast CastInfos, MutableInt DamageValue) {
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_ATTACKED_POST_JET_TRAP)) {
            if (Buff.applyEffect(DamageValue, CastInfos) == -3) {
                return -3;
            }
        }
        return -1;
    }

    /// Subit des dommages, activation des buffs de reduction, renvois, anihilation des dommages apres le calcul du jet
    /// </summary>
    /// <param name="CastInfos"></param>
    /// <param name="DamageValue"></param>
    public int onattackedafterjet(EffectCast CastInfos, MutableInt DamageValue) {
        for (BuffEffect Buff : buffsAct.get(BuffActiveType.ACTIVE_ATTACKED_AFTER_JET)) {
            if (Buff.applyEffect(DamageValue, CastInfos) == -3) {
                return -3;
            }
        }
        return -1;
    }

    public int decrementEffectDuration(int duration) {
        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN)) {
            if (Buff.isDebuffable() && Buff.decrementDuration(duration) <= 0) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN)) {
            if (Buff.isDebuffable() && Buff.decrementDuration(duration) <= 0) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN).removeIf(x -> x.isDebuffable() && x.Duration <= 0);
        this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN).removeIf(x -> x.isDebuffable() && x.Duration <= 0);

        this.buffsAct.values().stream().forEach((BuffList) -> {
            BuffList.removeIf(x -> x.isDebuffable() && x.Duration <= 0);
        });

        return -1;
    }

    public int dispell(int spell) {
        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN)) {
            if (Buff.CastInfos != null && Buff.CastInfos.SpellId == spell) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN)) {
            if (Buff.CastInfos != null && Buff.CastInfos.SpellId == spell) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN).removeIf(x -> x.CastInfos != null && x.CastInfos.SpellId == spell);
        this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN).removeIf(x -> x.CastInfos != null && x.CastInfos.SpellId == spell);

        this.buffsAct.values().stream().forEach((BuffList) -> {
            BuffList.removeIf(x -> x.CastInfos != null && x.CastInfos.SpellId == spell);
        });

        return -1;
    }

    public int debuff() {
        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN)) {
            if (Buff.isDebuffable()) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        for (BuffEffect Buff : this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN)) {
            if (Buff.isDebuffable()) {
                if (Buff.removeEffect() == -3) {
                    return -3;
                }
            }
        }

        this.buffsDec.get(BuffDecrementType.TYPE_BEGINTURN).removeIf(x -> x.isDebuffable());
        this.buffsDec.get(BuffDecrementType.TYPE_ENDTURN).removeIf(x -> x.isDebuffable());

        this.buffsAct.values().stream().forEach((BuffList) -> {
            BuffList.removeIf(x -> x.isDebuffable());
        });

        return -1;
    }

}
