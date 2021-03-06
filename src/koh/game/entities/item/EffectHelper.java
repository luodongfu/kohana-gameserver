package koh.game.entities.item;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import koh.d2o.entities.Effect;

import koh.game.dao.DAO;
import koh.game.entities.environments.Pathfunction;
import koh.game.entities.spells.*;
import koh.game.fights.Fighter;
import koh.protocol.client.enums.ActionIdEnum;
import koh.protocol.client.enums.EffectGenerationType;
import koh.protocol.client.enums.StatsEnum;
import koh.protocol.types.game.data.items.ObjectEffect;
import koh.protocol.types.game.data.items.effects.*;
import koh.utils.Couple;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;

/**
 *
 * @author Neo-Craft
 */
public class EffectHelper {

    private static final Logger logger = LogManager.getLogger(EffectHelper.class);

    public static final int NEUTRAL_ELEMENT = 0;
    public static final int EARTH_ELEMENT = 1;
    public static final int FIRE_ELEMENT = 2;
    public static final int WATER_ELEMENT = 3;
    public static final int AIR_ELEMENT = 4;
    public static final int NONE_ELEMENT = 5;
    public static final int[] DATE_EFFECT = new int[]{805, 808, 983, 998}; //971
    public static final int DAMAGE_EFFECT_CATEGORY = 2;
    public static final int[] MONSTER_EFFECT = new int[]{185, 621, 1011, 905};
    public static final int[] LADDER_EFFECTS = new int[]{717};
    public static final int[] LIVING_OBJECT_EFFECT = new int[]{973, 971, 972, 974};
    public static final int[] RELATED_OBJECTS_EFFECT = new int[]{981, 982/*, 983*/};
    public static final int SPELL_EFFECT_PER_FIGHT = 1175;
    public static final int[] SPELL_ITEMS_EFFECTS = new int[]{
        281, //Augmente la PO du sort #1 de #3
        282, //Rend la portée du sort #1 modifiable
        283, //+#3 Dommages sur le sort #1
        284, //+#3 Soins sur le sort #1
        285, //Réduit de #3 le coût en PA du sort #1
        286, //Réduit de #3 le délai de relance du sort #1
        287, //+#3 CC sur le sort #1
        288, //Désactive le lancer en ligne du sort #1
        289, //Désactive la ligne de vue du sort #1
        290, //Augmente de #3 le nombre de lancer maximal par tour du sort #1
        291, //Augmente de #3 le nombre de lancer maximal par cible du sort #1
        292, //Fixe à #3 le délai de relance du sort #1
        293, //Augmente les dégâts de base du sort #1 de #3
        294, //Diminue la portée du sort #1 de #3
    };
    public static final int[] DAMAGE_EFFECTS_IDS = {1012, 1013, 1014, 1015, 1016,672, 85, 86, 87, 88, 89,1067, 1068, 1069, 1070, 1071, 96,97,98,99,100,91,92,93,94, 95,101,108 };
    public static final int[] HP_BASED_DAMAGE_EFFECTS_IDS= {672, 85, 86, 87, 88, 89};
    public static final int[] TARGET_HP_BASED_DAMAGE_EFFECTS_IDS= {1067, 1068, 1069, 1070, 1071};
    public static final int[] UN_RANDOMABLES_EFFECTS = new int[]{
        96,//Effect_DamageWater
        97,//Effect_DamageEarth
        98,//Effect_DamageAir
        99,//Effect_DamageFire
        100,//Effect_DamageNeutral
        91,//Effect_StealHPWater
        92,//Effect_StealHPEarth
        93,//Effect_StealHPAir
        94,//Effect_StealHPFire
        95,//Effect_StealHPNeutral
        101,//Effect_RemoveAP
        108//HEAL
    };

    private static final StatsEnum[] UNVERIFIED_EFFECTS = new StatsEnum[]{
            StatsEnum.DAMAGE_RETURN,
            StatsEnum.CHATIMENT,
            StatsEnum.DAMAGE_REDUCTION,
            StatsEnum.DAMMAGES_OCASSIONED,
            StatsEnum.DAMAGE_ARMOR_REDUCTION,
            StatsEnum.DODGE,
            StatsEnum.SACRIFICE,
            StatsEnum.SHARE_DAMAGES,
            StatsEnum.REFOULLAGE

    };

    public static IoBuffer serializeEffectInstanceDice(EffectInstance[] effects) {
        final IoBuffer buff = IoBuffer.allocate(effects.length * 25);
        buff.setAutoExpand(true);

        buff.putInt(effects.length);
        for (EffectInstance e : effects) {
            buff.put(e.serializationIdentifier());
            e.toBinary(buff);
        }

        buff.flip();
        return buff;
    }

    public static int randomValue(Couple<Integer, Integer> Couple) {
        return randomValue(Couple.first, Couple.second);
    }

    public static int randomValue(int i1, int i2) {
        Random rand = new Random();
        return rand.nextInt(i2 - i1 + 1) + i1;
    }

    public static Effect getEffect(int eff){
        return DAO.getD2oTemplates().getEffect(eff);
    }



    public static boolean verifyEffectTrigger(Fighter pCasterId, Fighter pTargetId, EffectInstance[] pSpellEffects, EffectInstance pEffect, boolean pWeaponEffect, String pTriggers, int pSpellImpactCell) {
        return verifyEffectTrigger(pCasterId, pTargetId, pSpellEffects, pEffect, pWeaponEffect, pTriggers, pSpellImpactCell,false);
    }

    public static boolean verifyEffectTrigger(Fighter pCasterId, Fighter pTargetId, EffectInstance[] pSpellEffects, EffectInstance pEffect, boolean pWeaponEffect, String pTriggers, int pSpellImpactCell,boolean direct) {
        if(direct && ArrayUtils.contains(UNVERIFIED_EFFECTS, pEffect.getEffectType())){
            return true;
        }

        boolean verify = true;
        final boolean isTargetAlly = pCasterId.isFriendlyWith(pTargetId);
        final int distance = Pathfunction.getDistance( pCasterId.getCellId(), pTargetId.getCellId());
        
        for (String trigger : pTriggers.split("\\|")) {
            switch (trigger) {
                case "I":
                    verify = true;
                    break;
                case "D":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (pEffect.category() == DAMAGE_EFFECT_CATEGORY);
                    break;
                case "DA":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (((pEffect.category() == DAMAGE_EFFECT_CATEGORY)) && ((getEffect(pEffect.effectId).elementId == AIR_ELEMENT)));
                    break;
                case "DBA":
                    verify = isTargetAlly;
                    break;
                case "DBE":
                    verify = !(isTargetAlly);
                    break;
                case "DC":
                    verify = pWeaponEffect;
                    break;
                case "DE":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (((pEffect.category() == DAMAGE_EFFECT_CATEGORY)) && ((getEffect(pEffect.effectId).elementId == EARTH_ELEMENT)));
                    break;
                case "DF":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (((pEffect.category() == DAMAGE_EFFECT_CATEGORY)) && ((getEffect(pEffect.effectId).elementId == FIRE_ELEMENT)));
                    break;
                case "DG":
                    break;
                case "DI":
                    break;
                case "DM":
                    verify = (distance <= 1);
                    break;
                case "DN":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (((pEffect.category() == DAMAGE_EFFECT_CATEGORY)) && ((getEffect(pEffect.effectId).elementId == NEUTRAL_ELEMENT)));
                    break;
                case "DP":
                    break;
                case "DR":
                    verify = (distance > 1);
                    break;
                case "Dr":
                    break;
                case "DS":
                    verify = !(pWeaponEffect);
                    break;
                case "DTB":
                    break;
                case "DTE":
                    break;
                case "DW":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (((pEffect.category() == DAMAGE_EFFECT_CATEGORY)) && ((getEffect(pEffect.effectId).elementId == WATER_ELEMENT)));
                    break;
                case "MD":
                    //verify = PushUtil.hasPushDamages(pCasterId, pTargetId, pSpellEffects, pEffect, pSpellImpactCell);
                    verify = true;
                    break;
                case "MDM":
                    break;
                case "MDP":
                    break;
                case "A":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (pEffect.effectId == 101);
                    break;
                case "m":
                    if(pEffect == null){
                        verify = false;
                        continue;
                    }
                    verify = (pEffect.effectId == 127);
                    break;
            };
            if (verify) {
                return (true);
            };
        };
        return (false);
    }

    public static ObjectEffect[] toObjectEffects(EffectInstance[] effects) {
        ObjectEffect[] array = new ObjectEffect[effects.length];
        for (int i = 0; i < array.length; ++i) {
            //EffectInstanceCreate 
            if (effects[i] instanceof EffectInstanceLadder) {
                array[i] = new ObjectEffectLadder((effects[i]).effectId, ((EffectInstanceLadder) effects[i]).monsterCount, ((EffectInstanceLadder) effects[i]).monsterFamilyId);
                continue;
            }
            if (effects[i] instanceof EffectInstanceCreature) {
                array[i] = new ObjectEffectCreature((effects[i]).effectId, ((EffectInstanceCreature) effects[i]).monsterFamilyId);
                continue;
            }
            if (effects[i] instanceof EffectInstanceMount) {
                array[i] = new ObjectEffectMount((effects[i]).effectId, ((EffectInstanceMount) effects[i]).date, ((EffectInstanceMount) effects[i]).modelId, ((EffectInstanceMount) effects[i]).mountId);
                continue;
            }
            if (effects[i] instanceof EffectInstanceString) {
                array[i] = new ObjectEffectString((effects[i]).effectId, ((EffectInstanceString) effects[i]).text);
                continue;
            }
            if (effects[i] instanceof EffectInstanceCreature) {
                array[i] = new ObjectEffectCreature((effects[i]).effectId, ((EffectInstanceCreature) effects[i]).monsterFamilyId);
                continue;
            }
            if (effects[i] instanceof EffectInstanceMinMax) {
                array[i] = new ObjectEffectMinMax((effects[i]).effectId, ((EffectInstanceMinMax) effects[i]).MinValue, ((EffectInstanceMinMax) effects[i]).MaxValue);
                continue;
            }
            if (effects[i] instanceof EffectInstanceDate) {
                array[i] = new ObjectEffectDate((effects[i]).effectId, ((EffectInstanceDate) effects[i]).Year, ((EffectInstanceDate) effects[i]).Mounth, ((EffectInstanceDate) effects[i]).Day, ((EffectInstanceDate) effects[i]).Hour, ((EffectInstanceDate) effects[i]).Minute);
                continue;
            }
            if (effects[i] instanceof EffectInstanceDice) {
                array[i] = new ObjectEffectDice((effects[i]).effectId, ((EffectInstanceDice) effects[i]).diceNum, ((EffectInstanceDice) effects[i]).diceSide, ((EffectInstanceInteger) effects[i]).value);
                continue;
            }
            if (effects[i] instanceof EffectInstanceInteger) {
                array[i] = new ObjectEffectInteger((effects[i]).effectId, ((EffectInstanceInteger) effects[i]).value);
            }

        }
        return array;
    }

    public static EffectInstance[] generateIntegerEffectArray(EffectInstance[] possibleEffects, EffectGenerationType genType, boolean isWeapon) {
        EffectInstance[] effects = new EffectInstance[possibleEffects.length];
        int i = 0;
        for (EffectInstance e : possibleEffects) {
            if (e instanceof EffectInstanceDice) {
                if (e.effectId == SPELL_EFFECT_PER_FIGHT || ArrayUtils.contains(RELATED_OBJECTS_EFFECT, e.effectId) || ArrayUtils.contains(SPELL_ITEMS_EFFECTS, e.effectId) || (isWeapon && ArrayUtils.contains(UN_RANDOMABLES_EFFECTS, e.effectId))) {
                    effects[i] = e;
                    continue;
                }

                if (ArrayUtils.contains(DATE_EFFECT, e.effectId)) {
                    final Calendar now = Calendar.getInstance();
                    now.add(Calendar.MONTH,1);
                    effects[i] = (new EffectInstanceDate(e, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR), now.get(Calendar.MINUTE)));
                    continue;
                }
                if (ArrayUtils.contains(MONSTER_EFFECT, e.effectId)) {
                    effects[i] = new EffectInstanceCreature(e, ((EffectInstanceDice) e).diceNum);
                    continue;
                }
                if (ArrayUtils.contains(LADDER_EFFECTS, e.effectId)) {
                    effects[i] = new EffectInstanceLadder(e, ((EffectInstanceDice) e).diceNum, 0);
                    continue;
                }

                short num1 = (short) (((EffectInstanceDice) e).diceNum >= ((EffectInstanceDice) e).diceSide ? ((EffectInstanceDice) e).diceNum : ((EffectInstanceDice) e).diceSide);
                short num2 = (short) ( ((EffectInstanceDice) e).diceNum <=  ((EffectInstanceDice) e).diceSide ? ((EffectInstanceDice) e).diceNum : ((EffectInstanceDice) e).diceSide);
                if (genType == EffectGenerationType.MAX_EFFECTS) {
                    effects[i] = (new EffectInstanceInteger(e, !e.getTemplate().operator.equalsIgnoreCase("-") ? num1 : num2));
                    continue;
                }
                if (genType == EffectGenerationType.MIN_EFFECTS) {
                    effects[i] = (new EffectInstanceInteger(e, !e.getTemplate().operator.equalsIgnoreCase("-") ? num2 : num1));
                    continue;
                }
                if ((int) num2 == 0) {
                    effects[i] = (new EffectInstanceInteger(e, num1));
                } else {
                    effects[i] = (new EffectInstanceInteger(e, (short) randomValue((int) num2, (int) num1 /*+ 1*/)));
                }
            } else if (e != null) {
                throw new Error("effect not suport" + e.serializationIdentifier());
            }
            i++;
        }
        return effects;
    }

    public static List<ObjectEffect> generateIntegerEffect(EffectInstance[] possibleEffects, EffectGenerationType genType, boolean isWeapon) {
        final List<ObjectEffect> effects = new ArrayList<>();
        for (EffectInstance e : possibleEffects) {
            if (e instanceof EffectInstanceDice) {
                logger.debug(e.toString());
                if (e.effectId == 984 || e.effectId == 800) //Truc familiers pas sur
                {
                    continue;
                }
                if (e.effectId == SPELL_EFFECT_PER_FIGHT || ArrayUtils.contains(RELATED_OBJECTS_EFFECT, e.effectId) || ArrayUtils.contains(SPELL_ITEMS_EFFECTS, e.effectId) || (isWeapon && ArrayUtils.contains(UN_RANDOMABLES_EFFECTS, e.effectId))) {
                    effects.add(new ObjectEffectDice(e.effectId, ((EffectInstanceDice) e).diceNum, ((EffectInstanceDice) e).diceSide, ((EffectInstanceDice) e).value));
                    continue;
                }
                if (ArrayUtils.contains(LIVING_OBJECT_EFFECT, e.effectId)) {
                    effects.add(new ObjectEffectInteger(e.effectId, ((EffectInstanceDice) e).value));
                    continue;
                }
                if (ArrayUtils.contains(DATE_EFFECT, e.effectId)) {
                    final Calendar now = Calendar.getInstance();
                    effects.add(new ObjectEffectDate(e.effectId, now.get(Calendar.YEAR), (byte) now.get(Calendar.MONTH), (byte) now.get(Calendar.DAY_OF_MONTH), (byte) now.get(Calendar.HOUR_OF_DAY), (byte) now.get(Calendar.MINUTE)));
                    continue;
                }
                if (ArrayUtils.contains(MONSTER_EFFECT, e.effectId)) {
                    effects.add(new ObjectEffectCreature(e.effectId, ((EffectInstanceDice) e).diceNum));
                    continue;
                }
                if (ArrayUtils.contains(LADDER_EFFECTS, e.effectId)) {
                    effects.add(new ObjectEffectLadder(e.effectId, ((EffectInstanceDice) e).diceNum, 0));
                    continue;
                }
                if(e.effectId == ActionIdEnum.ACTION_CHARACTER_LEARN_SPELL){
                    effects.add(new ObjectEffectInteger(e.effectId, ((EffectInstanceDice) e).value));
                    continue;
                }

                short num1 = (short) (((EffectInstanceDice) e).diceNum >= ((EffectInstanceDice) e).diceSide ? ((EffectInstanceDice) e).diceNum : ((EffectInstanceDice) e).diceSide);
                short num2 = (short) (((EffectInstanceDice) e).diceNum <= ((EffectInstanceDice) e).diceSide ? ((EffectInstanceDice) e).diceNum : ((EffectInstanceDice) e).diceSide);
                if (genType == EffectGenerationType.MAX_EFFECTS && !e.getTemplate().operator.equalsIgnoreCase("-")) {
                    effects.add(new ObjectEffectInteger(e.effectId, !e.getTemplate().operator.equalsIgnoreCase("-") ? num1 : num2));
                    continue;
                }
                if (genType == EffectGenerationType.MIN_EFFECTS) {
                    effects.add(new ObjectEffectInteger(e.effectId, !e.getTemplate().operator.equalsIgnoreCase("-") ? num2 : num1));
                    continue;
                }
                if ((int) num2 == 0) {
                    effects.add(new ObjectEffectInteger(e.effectId, num1));
                } else {
                    effects.add(new ObjectEffectInteger(e.effectId, randomValue( num2, num1)));
                }
            } else {
                throw new Error("effect not suport" + e.toString());
            }
        }
        return effects;
    }

    public static ObjectEffect[] objectEffects(List<EffectInstance> effects) {
        ObjectEffect[] array = new ObjectEffect[effects.size()];
        for (int i = 0; i < array.length; ++i) {
            //EffectInstanceCreate 
            if (effects.get(i) instanceof EffectInstanceDuration) {
                array[i] = new ObjectEffectDuration((effects.get(i)).effectId, ((EffectInstanceDuration) effects.get(i)).days, ((EffectInstanceDuration) effects.get(i)).hours, ((EffectInstanceDuration) effects.get(i)).minutes);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceLadder) {
                array[i] = new ObjectEffectLadder((effects.get(i)).effectId, ((EffectInstanceLadder) effects.get(i)).monsterFamilyId, ((EffectInstanceLadder) effects.get(i)).monsterCount);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceCreature) {
                array[i] = new ObjectEffectCreature((effects.get(i)).effectId, ((EffectInstanceCreature) effects.get(i)).monsterFamilyId);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceMount) {
                array[i] = new ObjectEffectMount((effects.get(i)).effectId, ((EffectInstanceMount) effects.get(i)).date, ((EffectInstanceMount) effects.get(i)).modelId, ((EffectInstanceMount) effects.get(i)).mountId);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceString) {
                array[i] = new ObjectEffectString((effects.get(i)).effectId, ((EffectInstanceString) effects.get(i)).text);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceMinMax) {
                array[i] = new ObjectEffectMinMax((effects.get(i)).effectId, ((EffectInstanceMinMax) effects.get(i)).MinValue, ((EffectInstanceMinMax) effects.get(i)).MaxValue);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceDate) {
                array[i] = new ObjectEffectDate((effects.get(i)).effectId, ((EffectInstanceDate) effects.get(i)).Year, ((EffectInstanceDate) effects.get(i)).Mounth, ((EffectInstanceDate) effects.get(i)).Day, ((EffectInstanceDate) effects.get(i)).Hour, ((EffectInstanceDate) effects.get(i)).Minute);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceDice) {
                array[i] = new ObjectEffectDice((effects.get(i)).effectId, ((EffectInstanceDice) effects.get(i)).diceNum, ((EffectInstanceDice) effects.get(i)).diceSide, ((EffectInstanceInteger) effects.get(i)).value);
                continue;
            }
            if (effects.get(i) instanceof EffectInstanceInteger) {
                array[i] = new ObjectEffectInteger((effects.get(i)).effectId, ((EffectInstanceInteger) effects.get(i)).value);
            }

        }
        return array;
    }

}
