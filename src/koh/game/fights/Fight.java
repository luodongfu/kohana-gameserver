package koh.game.fights;

import koh.concurrency.CancellableScheduledRunnable;
import koh.game.actions.GameAction;
import koh.game.actions.GameMapMovement;
import koh.game.dao.DAO;
import koh.game.entities.actors.IGameActor;
import koh.game.entities.actors.Player;
import koh.game.entities.actors.character.FieldNotification;
import koh.game.entities.environments.*;
import koh.game.entities.environments.MovementPath;
import koh.game.entities.environments.cells.Zone;
import koh.game.entities.item.EffectHelper;
import koh.game.entities.item.InventoryItem;
import koh.game.entities.maps.pathfinding.*;
import koh.game.entities.spells.EffectInstanceDice;
import koh.game.entities.spells.SpellLevel;
import koh.game.fights.IFightObject.FightObjectType;
import koh.game.fights.effects.EffectBase;
import koh.game.fights.effects.EffectCast;
import koh.game.fights.effects.buff.BuffAddSpellRange;
import koh.game.fights.effects.buff.BuffEffect;
import koh.game.fights.effects.buff.BuffEndTurn;
import koh.game.fights.effects.buff.BuffMinimizeEffects;
import koh.game.fights.fighters.BombFighter;
import koh.game.fights.fighters.CharacterFighter;
import koh.game.fights.fighters.StaticFighter;
import koh.game.fights.fighters.VirtualFighter;
import koh.game.fights.layers.FightActivableObject;
import koh.game.fights.layers.FightPortal;
import koh.game.fights.utils.Algo;
import koh.game.network.WorldClient;
import koh.game.network.handlers.game.approach.CharacterHandler;
import koh.game.paths.Node;
import koh.game.utils.Three;
import koh.protocol.client.Message;
import koh.protocol.client.enums.*;
import koh.protocol.messages.game.actions.SequenceEndMessage;
import koh.protocol.messages.game.actions.SequenceStartMessage;
import koh.protocol.messages.game.actions.fight.*;
import koh.protocol.messages.game.basic.TextInformationMessage;
import koh.protocol.messages.game.context.*;
import koh.protocol.messages.game.context.fight.*;
import koh.protocol.messages.game.context.fight.character.GameFightShowFighterMessage;
import koh.protocol.messages.game.context.roleplay.figh.GameRolePlayRemoveChallengeMessage;
import koh.protocol.messages.game.context.roleplay.figh.GameRolePlayShowChallengeMessage;
import koh.protocol.types.game.action.fight.FightDispellableEffectExtendedInformations;
import koh.protocol.types.game.actions.fight.FightTriggeredEffect;
import koh.protocol.types.game.actions.fight.GameActionMark;
import koh.protocol.types.game.context.IdentifiedEntityDispositionInformations;
import koh.protocol.types.game.context.fight.*;
import koh.protocol.types.game.context.roleplay.party.NamedPartyTeam;
import koh.protocol.types.game.context.roleplay.party.NamedPartyTeamWithOutcome;
import koh.protocol.types.game.data.items.effects.ObjectEffectDice;
import koh.protocol.types.game.idol.Idol;
import koh.utils.Couple;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static koh.protocol.client.enums.StatsEnum.ADD_BASE_DAMAGE_SPELL;
import static koh.protocol.client.enums.StatsEnum.CAST_SPELL_ON_CRITICAL_HIT;

/**
 * @author Neo-Craft
 */
public abstract class Fight extends IWorldEventObserver implements IWorldField {

    //public static final ImprovedCachedThreadPool BackGroundWorker2 = new ImprovedCachedThreadPool(5, 50, 2);
    public static final ScheduledExecutorService BACK_GROUND_WORKER = Executors.newScheduledThreadPool(50);
    public static final Random RANDOM = new Random();
    public static final StatsEnum[] EFFECT_NOT_SILENCED = new StatsEnum[]{
            StatsEnum.DAMAGE_NEUTRAL, StatsEnum.DAMAGE_EARTH, StatsEnum.DAMAGE_AIR, StatsEnum.DAMAGE_FIRE, StatsEnum.DAMAGE_WATER,
            StatsEnum.STEAL_NEUTRAL, StatsEnum.STEAL_EARTH, StatsEnum.STEAL_AIR, StatsEnum.STEAL_FIRE, StatsEnum.STEAL_WATER, StatsEnum.STEAL_PV_FIX,
            StatsEnum.DAMAGE_LIFE_NEUTRE, StatsEnum.DAMAGE_LIFE_WATER, StatsEnum.DAMAGE_LIFE_TERRE, StatsEnum.DAMAGE_LIFE_AIR, StatsEnum.DAMAGE_LIFE_FEU, StatsEnum.DAMAGE_DROP_LIFE
    };
    @Getter
    protected static final Logger logger = LogManager.getLogger(Fight.class);
    private static final HashMap<Integer, HashMap<Integer, Short[]>> MAP_FIGHTCELLS = new HashMap<>();
    private final Object $mutex_lock = new Object();
    public boolean hasFinished = false;
    public boolean IsSequencing;
    public boolean waitAcknowledgment;
    public SequenceTypeEnum sequence;
    protected FightTeam myTeam1 = new FightTeam((byte) 0, this);
    protected FightTeam myTeam2 = new FightTeam((byte) 1, this);
    protected long myLoopTimeOut = -1;
    protected long myLoopActionTimeOut;
    protected int myNextID = -1000;
    protected ArrayList<GameAction> myActions = new ArrayList<>();
    protected volatile GameFightEndMessage myResult;
    @Getter
    @Setter
    protected short fightId;
    @Getter
    protected FightState fightState;
    @Getter
    @Setter
    protected FightLoopState fightLoopState;
    @Getter
    protected DofusMap map;
    @Getter
    protected Fighter currentFighter;
    @Getter
    protected long fightTime, creationTime;
    @Getter
    protected FightTypeEnum fightType;
    @Getter
    protected Map<Short, FightCell> fightCells = new HashMap<>();
    protected Map<FightTeam, Map<Short, FightCell>> myFightCells = new HashMap<>();
    protected short ageBonus = -1, lootShareLimitMalus = -1;
    protected Map<String, CancellableScheduledRunnable> myTimers = new HashMap<>();
    @Getter
    protected Map<Fighter, CopyOnWriteArrayList<FightActivableObject>> activableObjects = Collections.synchronizedMap(new HashMap<>());
    @Getter
    protected FightWorker fightWorker = new FightWorker(this);
    @Getter
    protected AtomicInteger nextTriggerUid = new AtomicInteger();
    private SequenceTypeEnum m_lastSequenceAction;
    private int m_sequenceLevel;
    private Stack<SequenceTypeEnum> m_sequences = new Stack<>();
    private AtomicInteger contextualIdProvider = new AtomicInteger(-2);

    public Fight(FightTypeEnum type, DofusMap map) {
        this.fightState = fightState.STATE_PLACE;
        this.fightTime = -1;
        this.creationTime = Instant.now().getEpochSecond();
        this.fightType = type;
        this.map = map;
        this.fightId = this.map.nextFightId();
        this.initCells();
    }

    public synchronized int nextID() {
        return this.myNextID--;
    }

    public FighterRefusedReasonEnum canJoin(FightTeam Team, Player Character) {
        if (Team.canJoin(Character) != FighterRefusedReasonEnum.FIGHTER_ACCEPTED) {
            return Team.canJoin(Character);
        } else if (this.getFreeSpawnCell(Team) == null) {
            return FighterRefusedReasonEnum.TEAM_FULL;
        } else {
            return FighterRefusedReasonEnum.FIGHTER_ACCEPTED;
        }
    }

    public boolean canJoinSpectator() {
        return this.fightState == fightState.STATE_ACTIVE && !this.myTeam1.isToggled(FightOptionsEnum.FIGHT_OPTION_SET_SECRET) && !this.myTeam2.isToggled(FightOptionsEnum.FIGHT_OPTION_SET_SECRET);
    }

    public FightTeam getEnnemyTeam(FightTeam team) {
        return (team == this.myTeam1 ? this.myTeam2 : this.myTeam1);
    }

    public FightTeam getAllyTeam(FightTeam team)
    {
        return (team == this.myTeam1 ? this.myTeam1 : this.myTeam2);
    }

    public FightTeam getTeam(int LeaderId) {
        return (this.myTeam1.LeaderId == LeaderId ? this.myTeam1 : this.myTeam2);
    }

    public abstract void leaveFight(Fighter Fighter);
    //TODO ActionIdConverter.ACTION_FIGHT_DISABLE_PORTAL

    public abstract void endFight(FightTeam Winners, FightTeam Loosers);

    public abstract int getStartTimer();

    public abstract int getTurnTime();

    private void initCells() {
        // Ajout des cells
        for (DofusCell cell : this.map.getCells()) {
            this.fightCells.put(cell.getId(), new FightCell(cell.getId(), cell.walakableInFight(), cell.los()));
        }
        this.myFightCells.put(myTeam1, new HashMap<>());
        this.myFightCells.put(myTeam2, new HashMap<>());

        if (Fight.MAP_FIGHTCELLS.containsKey(this.map.getId())) {
            // Ajout
            synchronized (Fight.MAP_FIGHTCELLS) {
                for (Short Cell : Fight.MAP_FIGHTCELLS.get(this.map.getId()).get(0)) {
                    this.myFightCells.get(this.myTeam1).put(Cell, this.fightCells.get(Cell));
                }
                for (Short Cell : Fight.MAP_FIGHTCELLS.get(this.map.getId()).get(1)) {
                    this.myFightCells.get(this.myTeam2).put(Cell, this.fightCells.get(Cell));
                }
            }
            return;
        }

        for (Short CellValue : this.map.getRedCells()) {
            FightCell Cell = this.fightCells.get(CellValue);
            if (Cell == null || !Cell.canWalk()) {
                continue;
            }
            this.myFightCells.get(this.myTeam1).put(CellValue, Cell);
        }

        for (Short CellValue : this.map.getBlueCells()) {
            FightCell Cell = this.fightCells.get(CellValue);
            if (Cell == null || !Cell.canWalk()) {
                continue;
            }
            this.myFightCells.get(this.myTeam2).put(CellValue, Cell);
        }

        if (this.map.getBlueCells().length == 0 || this.map.getRedCells().length == 0) {
            this.myFightCells.get(this.myTeam1).clear();
            this.myFightCells.get(this.myTeam2).clear();
            Couple<ArrayList<FightCell>, ArrayList<FightCell>> startCells = Algo.genRandomFightPlaces(this);
            for (FightCell Cell : startCells.first) {
                this.myFightCells.get(this.myTeam1).put(Cell.Id, Cell);
            }
            for (FightCell Cell : startCells.second) {
                this.myFightCells.get(this.myTeam2).put(Cell.Id, Cell);
            }
            synchronized (Fight.MAP_FIGHTCELLS) {
                Fight.MAP_FIGHTCELLS.put(this.map.getId(), new HashMap<>());
                Fight.MAP_FIGHTCELLS.get(this.map.getId()).put(0, this.myFightCells.get(this.myTeam1).keySet().toArray(new Short[this.myFightCells.get(this.myTeam1).size()]));
                Fight.MAP_FIGHTCELLS.get(this.map.getId()).put(1, this.myFightCells.get(this.myTeam2).keySet().toArray(new Short[this.myFightCells.get(this.myTeam2).size()]));
            }
        }

    }

    public void disconnect(CharacterFighter fighter) {
        this.sendToField(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 182, new String[]{fighter.getCharacter().getNickName(), Integer.toString(fighter.getTurnRunning())}));
        if (this.currentFighter.getID() == fighter.getID()) {
            this.fightLoopState = fightLoopState.STATE_END_TURN;
        }
        fighter.setTurnReady(true);
    }

    public void launchSpell(Fighter fighter, SpellLevel spelllevel, short cellId, boolean friend) {
        launchSpell(fighter, spelllevel, cellId, friend, false, true);
    }


    public boolean pointLos(int x, int y, boolean bAllowTroughEntity)
    {
        final FightCell cell = this.getCell(MapPoint.fromCoords(x, y).get_cellId());
        boolean los = cell.isLineOfSight();
        if (!(bAllowTroughEntity))
        {
            if(!cell.canGoTrough())
                return false;
        };
        return (los);
    }

    private static final int TOLERANCE_ELEVATION = 11;

    public boolean pointMov(int x, int y, boolean bAllowTroughEntity, int previousCellId, int endCellId)
    {
        boolean useNewSystem;
        short cellId;
        DofusCell cellData,previousCellData;
        boolean mov;
        int  dif;
        if (MapPoint.isInMap(x, y))
        {
            useNewSystem =  map.isUsingNewMovementSystem();
            cellId = MapPoint.fromCoords(x, y).get_cellId();
            cellData = map.getCell(cellId);
            mov = ((cellData.mov()) && (cellData.nonWalkableDuringFight()));
            if (((((((mov) && (useNewSystem))) && (!((previousCellId == -1))))) && (!((previousCellId == cellId)))))
            {
                previousCellData = map.getCell((short)previousCellId);
                dif = Math.abs((Math.abs(cellData.getFloor()) - Math.abs(previousCellData.getFloor())));
                if (((((!((previousCellData.getMoveZone() == cellData.getMoveZone()))) && ((dif > 0)))) || ((((((previousCellData.getMoveZone() == cellData.getMoveZone())) && ((cellData.getMoveZone() == 0)))) && ((dif > TOLERANCE_ELEVATION))))))
                {
                    mov = false;
                }
            }
            if (!(bAllowTroughEntity))
            {
                for(IFightObject o : this.getCell(cellId).getObjects()){
                    if ((((endCellId == cellId)) && (o.canWalk())))
                    {
                    }
                    else
                    {
                        if (!(o.canGoThrough()))
                        {
                            return (false);
                        }
                    }
                }
            }
        }
        else
        {
            mov = false;
        }
        return (mov);
    }

    public boolean hasEntity(int x, int y){
        final short cell = (short) Math.abs(MapPoint.coordToCellId(x, y));
        try {
            for(IFightObject o : this.getCell(cell).getObjects()){
                if(!o.canGoThrough()){
                    return true;
                }
            }
            return !this.map.getCell(cell).los() && !this.map.getCell(cell).farmCell();
        }
        catch(ArrayIndexOutOfBoundsException | NullPointerException e){
        }
        return false;
    }

    private Three<Integer, int[], Integer> getTargetThroughPortal(Fighter Fighter, int param1) {
        return getTargetThroughPortal(Fighter, param1, false);
    }

    private Three<Integer, int[], Integer> getTargetThroughPortal(Fighter Fighter, int param1, boolean param2) {
        MapPoint _loc3_ = null;
        int damagetoReturn = 0;
        MapPoint _loc16_;
        FightPortal[] Portails = new FightPortal[0];
        for (CopyOnWriteArrayList<FightActivableObject> Objects : this.activableObjects.values()) {
            for (FightActivableObject Object : Objects) {
                if (Object instanceof FightPortal && ((FightPortal) Object).Enabled) {
                    Portails = ArrayUtils.add(Portails, (FightPortal) Object);
                    if (Object.getCellId() == param1) {
                        _loc3_ = Object.getMapPoint();
                    }
                }
            }
        }
        if (Portails.length < 2) {
            return new Three<>(param1, new int[0], 0);
        }
        if (_loc3_ == null) {
            return new Three<>(param1, new int[0], 0);
        }
        final int[] _loc10_ = LinkedCellsManager.getLinks(_loc3_, Arrays.stream(Portails)/*.filter(x -> x.caster.team == Fighter.team)*/.map(x -> x.getMapPoint()).toArray(MapPoint[]::new));
        MapPoint _loc11_ = MapPoint.fromCellId(_loc10_[/*_loc10_.length == 0 ? 0 :*/_loc10_.length - 1]);
        MapPoint _loc12_ = MapPoint.fromCellId(Fighter.getCellId());
        if (_loc12_ == null) {
            return new Three<>(param1, new int[0], 0);
        }
        int _loc13_ = _loc3_.get_x() - _loc12_.get_x() + _loc11_.get_x();
        int _loc14_ = _loc3_.get_y() - _loc12_.get_y() + _loc11_.get_y();
        if (!MapPoint.isInMap(_loc13_, _loc14_)) {
            return /*AtouinConstants.MAP_CELLS_COUNT + 1*/ new Three<>(561, new int[0], 0);
        }
        _loc16_ = MapPoint.fromCoords(_loc13_, _loc14_);
        /* if (param2) {
         _loc17_ = new int[]{_loc12_.get_cellId(), _loc3_.get_cellId()};
         //LinkedCellsManager.getInstance().drawLinks("spellEntryLink",_loc17_,10,TARGET_COLOR.color,1);
         if (_loc16_.get_cellId() < 560) {
         _loc18_ = new int[]{_loc11_.get_cellId(), _loc16_.get_cellId()};
         //LinkedCellsManager.getInstance().drawLinks("spellExitLink",_loc18_,6,TARGET_COLOR.color,1);
         }
         }
         for (int i : _loc10_) {
         damagetoReturn += Arrays.stream(Portails).filter(y -> y.getCellId() == i).findFirst().get().damageValue;
         }*/
        int[] portailIds = new int[_loc10_.length];
        FightPortal Portal;
        for (int i = 0; i < _loc10_.length; i++) {
            final int ID = _loc10_[i];
            Portal = Arrays.stream(Portails).filter(y -> y.getCellId() == ID).findFirst().get();
            damagetoReturn += Portal.damageValue;
            portailIds[i] = Portal.ID;
        }
        return new Three<>((int) _loc16_.get_cellId(), portailIds, damagetoReturn);
    }

    private static final int[] BLACKLISTED_EFFECTS = DAO.getSettings().getIntArray("Effect.BlacklistedByTriggers");

    public void launchSpell(Fighter fighter, SpellLevel spellLevel, short cellId, boolean friend, boolean fakeLaunch, boolean imTargeted) {
        if (this.fightState != fightState.STATE_ACTIVE) {
            return;
        }
        short oldCell = cellId;
        if (spellLevel.getSpellId() == 0 && fighter.isPlayer() && fighter.getPlayer().getInventoryCache().getItemInSlot(CharacterInventoryPositionEnum.ACCESSORY_POSITION_WEAPON) != null) {
            this.launchWeapon(fighter.asPlayer(), cellId);
            return;
        }

        // La cible si elle existe
        Fighter TargetE = this.hasEnnemyInCell(cellId, fighter.getTeam());
        if (friend && TargetE == null) { //FIXME: relook this line
            TargetE = this.hasFriendInCell(cellId, fighter.getTeam());
        }

        int targetId = TargetE == null ? -1 : TargetE.getID();
        // Peut lancer le sort ?
        if (!fakeLaunch && !this.canLaunchSpell(fighter, spellLevel, fighter.getCellId(), cellId, targetId)) {
            fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 175));
            this.endSequence(SequenceTypeEnum.SEQUENCE_SPELL, false);
            return;
        }
        if (!fakeLaunch) {
            this.startSequence(SequenceTypeEnum.SEQUENCE_SPELL);

            fighter.setUsedAP(fighter.getUsedAP() + spellLevel.getApCost());
            fighter.getSpellsController().actualize(spellLevel, targetId);
        }

        boolean IsCc = false;
        if (spellLevel.getCriticalHitProbability() != 0 && spellLevel.getCriticalEffect().length > 0) {
            int TauxCC = spellLevel.getCriticalHitProbability() - fighter.getStats().getTotal(StatsEnum.ADD_CRITICAL_HIT);
            if (TauxCC < 2) {
                TauxCC = 2;
            }
            if (Fight.RANDOM.nextInt(TauxCC) == 0) {
                IsCc = true;
            }
            logger.debug("CC: " + IsCc + " TauxCC " + TauxCC + " getSpellLevel.criticalHitProbability " + spellLevel.getCriticalHitProbability());
        }
        IsCc &= !fighter.getBuff().getAllBuffs().anyMatch(x -> x instanceof BuffMinimizeEffects);
        if (IsCc && !fakeLaunch && fighter.getStats().getTotal(CAST_SPELL_ON_CRITICAL_HIT) > 0) { //Turquoise
            fighter.getPlayer().getInventoryCache().getEffects(CAST_SPELL_ON_CRITICAL_HIT.value()).forEach(list -> {
                list.forEach(effect -> {
                    launchSpell(fighter, DAO.getSpells().findSpell(effect.diceNum).getSpellLevel(effect.diceSide), fighter.getCellId(), true,true,true);
                });
            });
        }

        EffectInstanceDice[] spellEffects = IsCc ? spellLevel.getCriticalEffect() : spellLevel.getEffects();
        if (spellEffects == null) {
            spellEffects = spellLevel.getCriticalEffect();
        }
        final int maxGroup = Arrays.stream(spellEffects).mapToInt(ef -> ef.group).max().orElse(0);
        if (maxGroup > 0) {
            Arrays.stream(spellEffects).forEach(e -> e.random = 0); //TODO check 3-4 monsters group spells
            final int randGroup = RANDOM.nextInt(maxGroup);
            spellEffects = Arrays.stream(spellEffects).filter(ef -> ef.group == randGroup).toArray(EffectInstanceDice[]::new);
            Arrays.stream(spellEffects).forEach(x -> x.targetMask = "a,A");
            //TODO: Ecaflip rekop make all c targetMask in db , ankama mistake ...
        }

        final boolean silentCast = Arrays.stream(spellEffects).allMatch(x -> !ArrayUtils.contains(EFFECT_NOT_SILENCED, x.getEffectType()));

        if (!fakeLaunch) {
            Three<Integer, int[], Integer> informations = null;
            if (this.getCell(cellId).hasGameObject(FightObjectType.OBJECT_PORTAL)
                    && !Arrays.stream(spellEffects).anyMatch(Effect -> Effect.getEffectType().equals(StatsEnum.DISABLE_PORTAL))) {
                informations = this.getTargetThroughPortal(fighter, cellId, true);
                cellId = informations.first.shortValue();
                //this.sendToField(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 0, new String[]{"DamagePercentBoosted Suite au portails = " + DamagePercentBoosted}));

            }
            for (Player player : this.Observable$stream()) {
                player.send(new GameActionFightSpellCastMessage(ActionIdEnum.ACTION_FIGHT_CAST_SPELL, fighter.getID(), targetId, cellId, (byte) (IsCc ? 2 : 1), spellLevel.getSpellId() == 2763 ? true : (!fighter.isVisibleFor(player) || silentCast), spellLevel.getSpellId(), spellLevel.getGrade(), informations == null ? new int[0] : informations.second));
            }
            if (informations != null) {
                informations.Clear();
            }
        }

        HashMap<EffectInstanceDice, ArrayList<Fighter>> targets = new HashMap<>();
        for (EffectInstanceDice effect : spellEffects) {
            System.out.println(effect.toString());
            targets.put(effect, new ArrayList<>());
            //.
            final Fighter[] targetsOnZone = Arrays.stream((new Zone(effect.getZoneShape(), effect.zoneSize(), MapPoint.fromCellId(fighter.getCellId()).advancedOrientationTo(MapPoint.fromCellId(cellId), true), this.map))
                    .getCells(cellId))
                    .map(cell -> this.getCell(cell))
                    .filter(cell -> cell != null && cell.hasGameObject(FightObjectType.OBJECT_FIGHTER, FightObjectType.OBJECT_STATIC))
                    .map(fightCell -> fightCell.getObjectsAsFighter()[0])
                    .toArray(Fighter[]::new);

            /* Explanation about the bottom code
               When a fighter cast spell in a cell where he is not on it .
               Like Mot drainant , Corruption.. Some spell effect need to be applied on himself
             */
            if (effect.targetMask.equals("C")
                    && effect.zoneShape() == 80
                    && effect.zoneSize() == 1
                    && Arrays.stream(targetsOnZone).noneMatch(fr -> fr.getID() == fighter.getID())) {
                if (effect.isValidTarget(fighter, fighter) && (EffectInstanceDice.verifySpellEffectMask(fighter, fighter, effect))) {
                    targets.get(effect).add(fighter);
                }
            }
            for (Fighter target : targetsOnZone) {
                logger.debug("EffectId {} target {} Triger {} validTarget {} spellMask {}",effect.effectId,target.getID(),EffectHelper.verifyEffectTrigger(fighter, target, spellEffects, effect, false, effect.triggers, cellId),effect.isValidTarget(fighter, target),EffectInstanceDice.verifySpellEffectMask(fighter, target, effect));
                if ((ArrayUtils.contains(BLACKLISTED_EFFECTS,effect.effectUid)
                        || EffectHelper.verifyEffectTrigger(fighter, target, spellEffects, effect, false, effect.triggers, cellId))
                        && effect.isValidTarget(fighter, target)
                        && EffectInstanceDice.verifySpellEffectMask(fighter, target, effect)) {
                    if ((effect.targetMask.equals("C") && fighter.getCarriedActor() == target.getID())
                            || (effect.targetMask.equals("a,A") && fighter.getCarriedActor() != 0 & fighter.getID() == target.getID())
                            || (!imTargeted && target.getID() == fighter.getID())) {
                        continue;
                    }
                                /*if (Fighter instanceof BombFighter && target.states.hasState(FightStateEnum.Kaboom)) {
                                 continue;
                                 }*/
                    logger.debug("Targeet Aded!");
                    targets.get(effect).add(target);
                }
            }
        }
        double num1 = Fight.RANDOM.nextDouble();
        double num2 = (double) Arrays.stream(spellEffects).mapToInt(x -> x.random).sum();
        boolean flag = false;
        for (Iterator<EffectInstanceDice> effectI = ((Arrays.stream(spellEffects).sorted((e2, e1) -> (e1.getEffectType() == StatsEnum.DISPELL_SPELL ? 0 : (e2.getEffectType() == StatsEnum.DISPELL_SPELL ? -1 : 1))).iterator())); effectI.hasNext(); ) {
            final EffectInstanceDice effect = effectI.next();
            if (effect.random > 0) {
                if (!flag) {
                    if (num1 > (double) effect.random / num2) {
                        num1 -= (double) effect.random / num2;
                        continue;
                    } else {
                        flag = true;
                    }
                } else {
                    continue;
                }
            }
            // Actualisation des morts
            targets.get(effect).removeIf(fr -> fr.isDead());
            if (effect.getEffectType() == ADD_BASE_DAMAGE_SPELL) {
                targets.get(effect).clear();
                targets.get(effect).add(fighter);
            }
            if (effect.delay > 0) {
                //TODO: Set ParentBoost UID
                fighter.getBuff().delayedEffects.add(new Couple<>(new EffectCast(effect.getEffectType(), spellLevel.getSpellId(), cellId, num1, effect, fighter, targets.get(effect), false, StatsEnum.NONE, 0, spellLevel), effect.delay));
                targets.get(effect).stream().forEach((Target) -> {
                    this.sendToField(new GameActionFightDispellableEffectMessage(effect.effectId, fighter.getID(), new FightTriggeredEffect(Target.getNextBuffUid().incrementAndGet(), Target.getID(), (short) effect.duration, FightDispellableEnum.DISPELLABLE, spellLevel.getSpellId(), effect.effectUid, 0, (short) effect.diceNum, (short) effect.diceSide, (short) effect.value, (short) effect.delay)));
                });
                continue;
            }
            EffectCast CastInfos = new EffectCast(effect.getEffectType(), spellLevel.getSpellId(), cellId, num1, effect, fighter, targets.get(effect), false, StatsEnum.NONE, 0, spellLevel);
            CastInfos.targetKnownCellId = cellId;
            CastInfos.oldCell = oldCell;
            if (EffectBase.tryApplyEffect(CastInfos) == -3) {
                break;
            }
        }

        if (!fakeLaunch) {
            this.sendToField(new GameActionFightPointsVariationMessage(ActionIdEnum.ACTION_CHARACTER_ACTION_POINTS_USE, fighter.getID(), fighter.getID(), (short) -spellLevel.getApCost()));
        }

        if (!fakeLaunch
                && fighter.getVisibleState() == GameActionFightInvisibilityStateEnum.INVISIBLE
                && silentCast
                && spellLevel.getSpellId() != 2763) {
            this.sendToField(new ShowCellMessage(fighter.getID(), fighter.getCellId()));
        }

        if (!fakeLaunch) {
            this.endSequence(SequenceTypeEnum.SEQUENCE_SPELL, false);
        }
    }

    //TODO : Log number of cac launched in case if he reconnect and useBug
    public void launchWeapon(CharacterFighter fighter, short cellId) {
        // Combat encore en cour ?
        if (this.fightState != fightState.STATE_ACTIVE) {
            return;
        }
        if (fighter != this.currentFighter) {
            return;
        }
        this.startSequence(SequenceTypeEnum.SEQUENCE_WEAPON);
        InventoryItem weapon = fighter.getCharacter().getInventoryCache().getItemInSlot(CharacterInventoryPositionEnum.ACCESSORY_POSITION_WEAPON);
        if (weapon.getTemplate().getTypeId() == 83) { //Pière d'Ame
            return;
        }
        // La cible si elle existe
        Fighter targetE = this.hasEnnemyInCell(cellId, fighter.getTeam());
        if (targetE == null) {
            targetE = this.hasFriendInCell(cellId, fighter.getTeam());
        }
        int targetId = targetE == null ? -1 : targetE.getID();
        if (!(Pathfunction.goalDistance(map, cellId, fighter.getCellId()) <= weapon.getWeaponTemplate().getRange()
                && Pathfunction.goalDistance(map, cellId, fighter.getCellId()) >= weapon.getWeaponTemplate().getMinRange()
                && fighter.getAP() >= weapon.getWeaponTemplate().getApCost())) {
            fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 175));
            this.endSequence(SequenceTypeEnum.SEQUENCE_WEAPON, false);
            return;
        }



        fighter.setUsedAP(fighter.getUsedAP() + weapon.getWeaponTemplate().getApCost());

        boolean isCc = false;

        int tauxCC = weapon.getWeaponTemplate().getCriticalHitProbability() - fighter.getStats().getTotal(StatsEnum.ADD_CRITICAL_HIT);
        if (tauxCC < 2) {
            tauxCC = 2;
        }
        if (Fight.RANDOM.nextInt(tauxCC) == 0) {
            isCc = true;
        }

        isCc &= !fighter.getBuff().getAllBuffs().anyMatch(x -> x instanceof BuffMinimizeEffects);

        ArrayList<Fighter> Targets = new ArrayList<>(5);

        for (short Cell : (new Zone(SpellShapeEnum.valueOf(weapon.getItemType().zoneShape()), weapon.getItemType().zoneSize(), MapPoint.fromCellId(fighter.getCellId()).advancedOrientationTo(MapPoint.fromCellId(cellId), true), this.map)).getCells(cellId)) {
            FightCell FightCell = this.getCell(Cell);
            if (FightCell != null) {
                if (FightCell.hasGameObject(FightObjectType.OBJECT_FIGHTER) | FightCell.hasGameObject(FightObjectType.OBJECT_STATIC)) {
                    Targets.addAll(FightCell.getObjectsAsFighterList());
                }
            }
        }

        Targets.removeIf(F -> F.isDead());
        Targets.remove(fighter);
        ObjectEffectDice[] Effects = weapon.getEffects$Notify()
                .stream()
                .filter(Effect -> Effect instanceof ObjectEffectDice && ArrayUtils.contains(EffectHelper.UN_RANDOMABLES_EFFECTS, Effect.actionId))
                .map(x -> (ObjectEffectDice) x)
                .toArray(ObjectEffectDice[]::new);

        double num1 = Fight.RANDOM.nextDouble();

        double num2 = Arrays.stream(Effects)
                .mapToInt(Effect -> weapon.getTemplate().getEffect(Effect.actionId).random)
                .sum();
        boolean flag = false;

        this.sendToField(new GameActionFightCloseCombatMessage(ActionIdEnum.ACTION_FIGHT_CAST_SPELL, fighter.getID(), targetId, cellId, (byte) (isCc ? 2 : 1), false, weapon.getTemplate().getId()));

        EffectInstanceDice effectParent;
        for (ObjectEffectDice effect : Effects) {
            effectParent = (EffectInstanceDice) weapon.getTemplate().getEffect(effect.actionId);
            System.out.println(effectParent.toString());
            if (effectParent.random > 0) {
                if (!flag) {
                    if (num1 > (double) effectParent.random / num2) {
                        num1 -= (double) effectParent.random / num2;
                        continue;
                    } else {
                        flag = true;
                    }
                } else {
                    continue;
                }
            }
            EffectCast castInfos = new EffectCast(StatsEnum.valueOf(effect.actionId), 0, cellId, num1, effectParent, fighter, Targets, true, StatsEnum.NONE, 0, null);
            castInfos.targetKnownCellId = cellId;
            if (EffectBase.tryApplyEffect(castInfos) == -3) {
                break;
            }
        }

        this.sendToField(new GameActionFightPointsVariationMessage(!isCc ? ActionIdEnum.ACTION_FIGHT_CLOSE_COMBAT : ActionIdEnum.ACTION_FIGHT_CLOSE_COMBAT_CRITICAL_MISS, fighter.getID(), fighter.getID(), (short) weapon.getWeaponTemplate().getApCost()));
        this.endSequence(SequenceTypeEnum.SEQUENCE_WEAPON, false);
    }

    public boolean canLaunchSpell(Fighter fighter, SpellLevel spell, short currentCell, short cellId, int targetId) {
        // Fake caster
        if (fighter != this.currentFighter) {
            return false;
        }

        // Fake cellId
        if (!this.fightCells.containsKey(cellId)) {
            return false;
        }

        // PA manquant ?
        if (fighter.getAP() < spell.getApCost()) {
            fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 170, String.valueOf(fighter.getAP()),String.valueOf(spell.getApCost())));
            return false;
        }
        else if (!this.map.getCell(cellId).walakable() || this.map.getCell(cellId).nonWalkableDuringFight()) {
            return false;
        }
        else if(!(!spell.isNeedFreeCell() || targetId == -1)){
            fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 172));
            return false;
        }
        else if(!(!spell.isNeedTakenCell() || targetId != -1)
                || (spell.isNeedFreeTrapCell() && this.fightCells.get(cellId).hasGameObject(FightObjectType.OBJECT_TRAP)) ){
            fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 193));
            return false;
        }
        else if(Arrays.stream(spell.getStatesForbidden()).anyMatch(x -> fighter.hasState(x))
                || Arrays.stream(spell.getStatesRequired()).anyMatch(x -> !fighter.hasState(x))){
            return false;
        }
        else if(!fighter.getSpellsController().canLaunchSpell(spell, targetId)){
            fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 175));
            return false;
        }

        int range = spell.getRange()
                + fighter.getBuff().getAllBuffs()
                .filter(buff -> buff instanceof BuffAddSpellRange && buff.castInfos.effect.diceNum == spell.getSpellId())
                .mapToInt(buff -> buff.castInfos.effect.value)
                .sum();

        if (spell.isRangeCanBeBoosted()) {
            int val1 = range + fighter.getStats().getTotal(StatsEnum.ADD_RANGE);
            if (val1 < spell.getMinRange()) {
                val1 = spell.getMinRange();
            }
            range = Math.min(val1, 280);
        }

        final Short[] zone = fighter.getCastZone(range,spell,currentCell);

        if((!ArrayUtils.contains(zone, cellId))){
            fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 171, String.valueOf(spell.getMinRange() != 0 ? spell.getMinRange() : 0), String.valueOf(range), String.valueOf(spell.getRange())));
            return false;
        }
        else if(spell.isCastTestLos() && !spell.isNeedFreeTrapCell()){
            final MapPoint target = MapPoint.fromCellId(cellId);
            final byte dir = fighter.getMapPoint().advancedOrientationTo(target);
            FightCell cell,lastCell = null;
            boolean result = true;

            for(Point p : Bresenham.findLine(fighter.getMapPoint().get_x(),fighter.getMapPoint().get_y(),target.get_x(),target.get_y())){
                if (!(MapPoint.isInMap(p.x, p.y))) {
                }else {
                    cell = this.getCell(MapTools.getCellNumFromXYCoordinates(p.x, p.y));fighter.send(new ShowCellMessage(0,cell.Id));
                    if(lastCell != null && lastCell.hasFighter()){
                        result = false;
                    }
                    if(!(cell.isLineOfSight())){
                        result = false;
                    }
                    lastCell = cell;
                }
            }
            if(!result){
                fighter.send(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 174));
                return false;
            }
        }
        return true;
    }

    public void toggleLock(Fighter fighter, FightOptionsEnum type) {
        boolean value = fighter.getTeam().isToggled(type) == false;
        fighter.getTeam().toggle(type, value);
        if (this.fightState == fightState.STATE_PLACE) {
            this.map.sendToField(new GameFightOptionStateUpdateMessage(this.fightId, fighter.getTeam().id, type.value, value));
        }

        Message Message = null;
        switch (type) {
            case FIGHT_OPTION_SET_CLOSED:
                if (value) {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 95);
                } else {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 96);
                }
                break;

            case FIGHT_OPTION_ASK_FOR_HELP:
                if (value) {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 103);
                } else {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 104);
                }
                break;

            case FIGHT_OPTION_SET_TO_PARTY_ONLY:
                if (value) {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 93);
                } else {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 94);
                }
                break;

            case FIGHT_OPTION_SET_SECRET:
                if (value) {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 40);

                    // on kick les spectateurs
                    this.kickSpectators();
                } else {
                    Message = new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_MESSAGE, 39);
                }
                break;
        }
        this.sendToField(Message);
    }

    public void kickSpectators() {

    }

    public synchronized void startFight() {
        // Si combat deja lancé
        if (this.fightState != fightState.STATE_PLACE) {
            return;
        }

        this.map.sendToField(new GameRolePlayRemoveChallengeMessage(this.fightId));

        // Preparation du lancement
        this.fightState = fightState.STATE_INIT;

        //TODO : CHALLENGE
        // Arret du timer
        this.stopTimer("startTimer");
        this.fightTime = System.currentTimeMillis();

        // initialize des tours
        this.fightWorker.initTurns();

        this.sendToField(new GameEntitiesDispositionMessage(this.fighters().map(x -> x.GetIdentifiedEntityDispositionInformations()).toArray(IdentifiedEntityDispositionInformations[]::new)));
        this.sendToField(new GameFightStartMessage(new Idol[0]));
        // Liste des tours
        //this.sendToField(new GameFightTurnListMessage(this.fightWorker.fighters().stream().filter(x -> x.isAlive()).mapToInt(x -> x.id).toArray(), this.fightWorker.fighters().stream().filter(x -> !x.isAlive()).mapToInt(x -> x.id).toArray()));
        this.sendToField(getFightTurnListMessage());
        this.sendToField(new GameFightSynchronizeMessage(this.fighters().map(x -> x.getGameContextActorInformations(null)).toArray(GameFightFighterInformations[]::new)));

        // reset du ready
        this.setAllUnReady();
        // En attente de lancement
        this.fightLoopState = fightLoopState.STATE_WAIT_START;

        // Lancement du gameLoop 10 ms d'interval.
        this.startTimer(new CancellableScheduledRunnable(BACK_GROUND_WORKER, 10, 10) {
            @Override
            public void run() {
                gameLoop();
            }
        }, "gameLoop");
    }

    private void gameLoop() {
        try {
            // Switch sur le status et verify fin de tour
            switch (this.fightLoopState) {
                case STATE_WAIT_START: // En attente de lancement
                    this.fightState = fightState.STATE_ACTIVE;
                    this.fightLoopState = fightLoopState.STATE_WAIT_READY;
                    this.beginTurn();
                    break;

                case STATE_WAIT_TURN: // Fin du tour par force a cause du timeout
                    if (this.myLoopTimeOut < System.currentTimeMillis()) {
                        if (this.isActionsFinish() || this.myLoopActionTimeOut < System.currentTimeMillis()) {
                            this.endTurn(); // Fin du tour
                        }
                    }
                    break;

                case STATE_END_TURN: // Fin du tour par le joueur
                    if (this.isActionsFinish() || this.myLoopActionTimeOut < System.currentTimeMillis()) {
                        this.endTurn(); // Fin du tour
                    }
                    break;

                case STATE_WAIT_READY: // En attente des joueurs x ...
                    if (this.isAllTurnReady()) {
                        this.middleTurn();
                        this.beginTurn();
                    } else if (this.myLoopTimeOut + 5000 < System.currentTimeMillis()) {
                        this.sendToField(new TextInformationMessage((byte) 1, 29, new String[]{StringUtils.join(this.getAliveFighters().filter(x -> !x.isTurnReady() && x instanceof CharacterFighter).map(y -> ((CharacterFighter) y).getCharacter().getNickName()).toArray(String[]::new), ", ")}));
                        this.middleTurn();
                        this.beginTurn();
                    }
                    break;

                case STATE_WAIT_AI: // Artificial intelligence
                     if (this.currentFighter instanceof VirtualFighter)
                     {
                     // Lancement de l'IA pour 30 secondes maximum
                         (this.currentFighter.asVirtual()).getMind().runAI();
                     }
                     else if (this.currentFighter.getObjectType() == FightObjectType.OBJECT_STATIC) {
                         this.myLoopActionTimeOut = System.currentTimeMillis() + 750;
                     }
                         // Fin de tour
                     if (this.fightLoopState != fightLoopState.STATE_WAIT_END) {
                         this.fightLoopState = fightLoopState.STATE_END_TURN;
                     }

                    break;

                case STATE_WAIT_END: // Fin du combat
                    if (!hasFinished || this.isActionsFinish() || this.myLoopActionTimeOut < System.currentTimeMillis()) {
                        this.endTurn(true);
                        //System.Threading.Thread.Sleep(500);
                        this.myTeam1.endFight();
                        this.myTeam2.endFight();
                        this.endFight(this.getWinners(), this.getEnnemyTeam(this.getWinners()));
                        hasFinished = true;
                    }
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /// <summary>
    /// Veifie si toute les actions son terminé
    /// </summary>
    /// <returns></returns>
    public boolean isActionsFinish() {
        return this.myActions.isEmpty();
    }

    public synchronized void beginTurn() {
        // Mise a jour du combattant
        this.currentFighter = this.fightWorker.getNextFighter();

        this.startSequence(SequenceTypeEnum.SEQUENCE_TURN_END);

        // Activation des buffs et fightObjects
        int BeginTurnIndice = this.currentFighter.beginTurn();

        this.endSequence(SequenceTypeEnum.SEQUENCE_TURN_END, false);

        // Mort du joueur ou fin de combat
        if (BeginTurnIndice == -3 || BeginTurnIndice == -2) {
            return;
        }

        // Envois debut du tour
        this.sendToField(new GameFightTurnStartMessage(this.currentFighter.getID(), this.getTurnTime() / 100));

        if (this.currentFighter instanceof CharacterFighter) {
            this.currentFighter.send(((CharacterFighter) this.currentFighter).FighterStatsListMessagePacket());
        }

        this.sendToField((o) -> {
            ((Player) o).send(new GameFightSynchronizeMessage(this.fighters().map(x -> x.getGameContextActorInformations((Player) o)).toArray(GameFightFighterInformations[]::new)));
        });

        this.currentFighter.send(new GameFightTurnStartPlayingMessage());

        // Timeout du tour
        this.myLoopTimeOut = System.currentTimeMillis() + this.getTurnTime();

        // status en attente de fin de tour
        if ((this.currentFighter instanceof CharacterFighter && ((CharacterFighter) currentFighter).getCharacter().getClient() == null && this.currentFighter.getTeam().getAliveFighters().count() > 1L) || this.currentFighter.getBuff().getAllBuffs().anyMatch(x -> x instanceof BuffEndTurn)) {
            this.fightLoopState = fightLoopState.STATE_END_TURN;
        } else {
            this.fightLoopState = fightLoopState.STATE_WAIT_TURN;
        }

        //Chalenge
            /*if (this instanceof MonsterFight && this.currentFighter instanceof CharacterFighter)
         {
         foreach (var Challenge in Challanges)
         {
         Challenge.beginTurn(this.currentFighter);
         }
         }*/
        // Monstre passe le tour
        if (this.currentFighter instanceof VirtualFighter || this.currentFighter instanceof StaticFighter) {
            this.fightLoopState = fightLoopState.STATE_WAIT_AI;
        }
    }

    public void endTurn() {
        endTurn(false);
    }

    public void endTurn(boolean finish) {
        this.startSequence(SequenceTypeEnum.SEQUENCE_TURN_END);
        // Fin du tour, activation des buffs, pieges etc
        if (this.currentFighter.endTurn() == -3) {
            return;
        }
        this.endSequence(SequenceTypeEnum.SEQUENCE_TURN_END, false);
        // Combat fini a la fin de son tour

        /* if (this instanceof MonsterFight && this.currentFighter  instanceof CharacterFighter)
         {
         this.Challanges.ForEach(x => x.endTurn(this.currentFighter));
         }*/
        // Tout le monde doit se synchro
        this.setAllUnReady();
        if (!finish) // En attente des joueurs
        {
            this.fightLoopState = fightLoopState.STATE_WAIT_READY;
        }

        if (this.IsSequencing) {
            this.endSequence(this.sequence, true);
        }
        if (this.waitAcknowledgment) {
            this.acknowledgeAction();
        }

        // Tour fini
        this.sendToField(new GameFightTurnEndMessage(this.currentFighter.getID()));
        if (!finish) {
            this.sendToField(new GameFightTurnReadyRequestMessage(this.currentFighter.getID()));
        }
    }

    public void middleTurn() {
        this.currentFighter.middleTurn();

    }

    protected void onTackled(Fighter fighter, int movementLength) {
        ArrayList<Fighter> tacklers = Pathfunction.getEnnemyNearToTakle(this, fighter.getTeam(), fighter.getCellId());

        int tackledMp = fighter.getTackledMP();
        int tackledAp = fighter.getTackledAP();
        if (fighter.getMP() - tackledMp < 0) {
            logger.error("Cannot apply tackle : mp tackled ({0}) > available mp ({1})", tackledMp, fighter.getMP());
        } else {
            this.sendToField(new GameActionFightTackledMessage(ActionIdEnum.ACTION_CHARACTER_ACTION_TACKLED, fighter.getID(), tacklers.stream().mapToInt(x -> x.getID()).toArray()));

            fighter.setUsedAP(fighter.getUsedAP() + tackledAp);
            fighter.setUsedMP(fighter.getUsedMP() + tackledMp);

            this.sendToField(new GameActionFightPointsVariationMessage(ActionIdEnum.ACTION_CHARACTER_ACTION_POINTS_USE, fighter.getID(), fighter.getID(), (short) -tackledAp));
            this.sendToField(new GameActionFightPointsVariationMessage(ActionIdEnum.ACTION_CHARACTER_MOVEMENT_POINTS_USE, fighter.getID(), fighter.getID(), (short) -tackledMp));

            if (movementLength <= fighter.getMP()) {
                return;
            }
        }
    }

    public void affectSpellTo(Fighter caster, Fighter target, int level, int... spells) {
        SpellLevel spell;
        for (int spellid : spells) {
            spell = DAO.getSpells().findSpell(spellid).getSpellLevel(level);
            double num1 = Fight.RANDOM.nextDouble();
            double num2 = (double) Arrays.stream(spell.getEffects()).mapToInt(x -> x.random).sum();
            boolean flag = false;
            for (EffectInstanceDice Effect : spell.getEffects()) {
                if (Effect.random > 0) {
                    if (!flag) {
                        if (num1 > (double) Effect.random / num2) {
                            num1 -= (double) Effect.random / num2;
                            continue;
                        } else {
                            flag = true;
                        }
                    } else {
                        continue;
                    }
                }
                ArrayList<Fighter> targets = new ArrayList<Fighter>() {
                    {
                        add(target);
                    }
                };
                if (Effect.delay > 0) {
                    target.getBuff().delayedEffects.add(new Couple<>(new EffectCast(Effect.getEffectType(), spellid, target.getCellId(), num1, Effect, caster, targets, false, StatsEnum.NONE, 0, spell), Effect.delay));
                    this.sendToField(new GameActionFightDispellableEffectMessage(Effect.effectId, caster.getID(), new FightTriggeredEffect(target.getNextBuffUid().incrementAndGet(), target.getID(), (short) Effect.duration, FightDispellableEnum.DISPELLABLE, spellid, Effect.effectUid, 0, (short) Effect.diceNum, (short) Effect.diceSide, (short) Effect.value, (short) Effect.delay)));
                    continue;
                }
                EffectCast CastInfos = new EffectCast(Effect.getEffectType(), spellid, target.getCellId(), num1, Effect, caster, targets, false, StatsEnum.NONE, 0, spell);
                CastInfos.targetKnownCellId = target.getCellId();
                if (EffectBase.tryApplyEffect(CastInfos) == -3) {
                    break;
                }
            }
        }
    }

    public synchronized GameMapMovement tryMove(Fighter fighter, koh.game.paths.Pathfinder path) {
        if (fighter != this.currentFighter) {
            return null;
        }

        // Pas assez de point de mouvement
        if (path.getPoints() > fighter.getMP()) {
            return null;
        }


        this.startSequence(SequenceTypeEnum.SEQUENCE_MOVE);

        if ((fighter.getTackledMP() > 0 || fighter.getTackledAP() > 0) && !this.currentFighter.getStates().hasState(FightStateEnum.ENRACINÉ)) {
            this.onTackled(fighter, path.getPoints());

        }

        for(int i = 0; i < path.getPath().size(); ++i){
            if(Pathfunction.isStopCell(this, fighter.getTeam(), path.getPath().get(i).getCell(), fighter)){
                if(path.getPath().last() != path.getPath().get(i)) {
                    path.cutPath(i +1);
                    break;
                }
            }
        }

        GameMapMovement gameMapMovement = new GameMapMovement(this, fighter, path.getPath().encode());

        this.sendToField(new FieldNotification(new GameMapMovementMessage(gameMapMovement.keyMovements, fighter.getID())) {
            @Override
            public boolean can(Player perso) {
                return fighter.isVisibleFor(perso);
            }
        });

        fighter.usedMP += path.getPoints();
        this.sendToField(new GameActionFightPointsVariationMessage(ActionIdEnum.ACTION_CHARACTER_MOVEMENT_POINTS_USE, fighter.getID(), fighter.getID(), (short) -path.getPoints()));

        fighter.setCell(this.getCell(path.getPath().last().getCell()));
        fighter.setDirection(path.getPath().last().getOrientation());
        this.endSequence(SequenceTypeEnum.SEQUENCE_MOVE, false);
        return gameMapMovement;
    }

    public synchronized GameMapMovement tryMove(Fighter fighter, MovementPath path) {
        // Pas a lui de jouer
        if (fighter != this.currentFighter) {
            return null;
        }

        // Pas assez de point de mouvement
        if (path.getMovementLength() > fighter.getMP() || path.getMovementLength() == -1) {
            return null;
        }

        this.startSequence(SequenceTypeEnum.SEQUENCE_MOVE);

        if ((fighter.getTackledMP() > 0 || fighter.getTackledAP() > 0) && !this.currentFighter.getStates().hasState(FightStateEnum.ENRACINÉ)) {
            this.onTackled(fighter, path.getMovementLength());
            if (path.transitCells.isEmpty() || path.getMovementLength() == 0) {
                this.endSequence(SequenceTypeEnum.SEQUENCE_MOVE, false);
                return null;
            }

        }
        GameMapMovement gameMapMovement = new GameMapMovement(this, fighter, path.serializePath());

        this.sendToField(new FieldNotification(new GameMapMovementMessage(gameMapMovement.keyMovements, fighter.getID())) {
            @Override
            public boolean can(Player perso) {
                return fighter.isVisibleFor(perso);
            }
        });

        fighter.usedMP += path.getMovementLength();
        this.sendToField(new GameActionFightPointsVariationMessage(ActionIdEnum.ACTION_CHARACTER_MOVEMENT_POINTS_USE, fighter.getID(), fighter.getID(), (short) -path.getMovementLength()));

        fighter.setCell(this.getCell(path.getEndCell()));
        fighter.setDirection(path.getEndDirection());
        this.endSequence(SequenceTypeEnum.SEQUENCE_MOVE, false);
        return gameMapMovement;

    }

    public byte findPlacementDirection(Fighter fighter) {
        if (this.fightState != fightState.STATE_PLACE) {
            throw new Error("State != Placement, cannot give placement direction");
        }
        FightTeam fightTeam = fighter.getTeam() == this.myTeam1 ? this.myTeam2 : this.myTeam1;
        Couple<Short, Integer> bestPos = null; //@Param1 = cellid,@Param2 = Distance
        for (Fighter fightActor : (Iterable<Fighter>) fightTeam.getFighters()::iterator) {
            MapPoint point = fightActor.getMapPoint();
            if (bestPos == null) {
                bestPos = new Couple<>(fightActor.getCellId(), fighter.getMapPoint().distanceToCell(point));
            } else if (fighter.getMapPoint().distanceToCell(point) < bestPos.second) {
                bestPos = new Couple<>(fightActor.getCellId(), fighter.getMapPoint().distanceToCell(point));
            }
        }
        if (bestPos == null) {
            return fighter.getDirection();
        } else {
            return fighter.getMapPoint().advancedOrientationTo(MapPoint.fromCellId(bestPos.first), false);
        }
    }

    public void swapPosition(Fighter fighter, Fighter fighterTarget) {
        FightCell cell = fighter.getMyCell();
        FightCell cell2 = fighterTarget.getMyCell();
        fighter.setCell(cell2);
        fighterTarget.setCell(cell);
        this.fighters().forEach(fr -> fr.setDirection(this.findPlacementDirection(fr)));
        this.sendToField(new GameFightPlacementSwapPositionsMessage(new IdentifiedEntityDispositionInformations[]{fighter.GetIdentifiedEntityDispositionInformations(), fighterTarget.GetIdentifiedEntityDispositionInformations()}));
    }

    public void SetFighterReady(Fighter fighter) {
        // Si combat deja commencé on arrete
        if (this.fightState != fightState.STATE_PLACE) {
            return;
        }

        fighter.setTurnReady(!fighter.isTurnReady());

        this.sendToField(new GameFightHumanReadyStateMessage(fighter.getID(), fighter.isTurnReady()));

        // Debut du combat si tout le monde ready
        if (this.isAllTurnReady() && this.fightType != FightTypeEnum.FIGHT_TYPE_PvT) {
            this.startFight();
        }
    }

    private boolean isAllTurnReady() {
        return this.getAliveFighters().allMatch(Fighter -> Fighter.isTurnReady());
    }

    private void setAllUnReady() {
        this.fighters()
                .filter(fr -> fr instanceof CharacterFighter && ((CharacterFighter) fr).getCharacter().getClient() != null)
                .forEach(fr -> fr.setTurnReady(false));
        /*foreach (var Fighter in this.fighters.Where(Fighter => Fighter is DoubleFighter))
         Fighter.turnReady = true;*/
    }

    public void setFighterPlace(Fighter fighter, short cellId) {
        // Deja pret ?
        if (fighter.isTurnReady()) {
            return;
        }

        FightCell Cell = this.myFightCells.get(fighter.getTeam()).get(cellId);

        // Existante ?
        if (Cell != null) {
            // Aucun persos dessus ?
            if (Cell.canWalk()) {
                // Affectation
                fighter.setCell(Cell);
                this.fighters().forEach(x -> x.setDirection(this.findPlacementDirection(x)));
                this.sendToField(new GameEntitiesDispositionMessage(this.fighters().map(x -> x.GetIdentifiedEntityDispositionInformations()).toArray(IdentifiedEntityDispositionInformations[]::new)));
            }
        }
    }

    protected void initFight(Fighter attacker, Fighter defender) {
        // Les leaders d'equipes
        this.myTeam1.setLeader(attacker);
        this.myTeam2.setLeader(defender);

        // On despawn avant la vue du flag de combat
        attacker.joinFight();
        defender.joinFight();

        // Flags de combat
        this.sendFightFlagInfos();

        // Rejoins les combats
        this.joinFightTeam(attacker, this.myTeam1, true, (short) -1, true);
        this.joinFightTeam(defender, this.myTeam2, true, (short) -1, true);

        // Si un timer pour le lancement du combat
        if (this.getStartTimer() != -1) {
            //FIXME: remove Thread.sleep
            this.startTimer(new CancellableScheduledRunnable(BACK_GROUND_WORKER, (getStartTimer() * 1000)) {
                @Override
                public void run() {
                    try {
                        Thread.sleep(getStartTimer() * 1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    startFight();
                }
            }, "startTimer");
        }
    }

    public void sendFightFlagInfos(WorldClient client) {
        if (this.fightState != fightState.STATE_PLACE) {
            return;
        }
        if (this.myTeam1.bladePosition == -1) {
            if (this.myTeam1.leader.getMapCell() != this.myTeam2.leader.getMapCell()) {
                this.myTeam1.bladePosition = this.myTeam1.leader.getMapCell();
                this.myTeam2.bladePosition = this.myTeam2.leader.getMapCell();
            } else {
                this.myTeam1.bladePosition = this.map.getRandomAdjacentFreeCell(this.myTeam2.leader.getMapCell()).getId();
                this.myTeam2.bladePosition = this.myTeam2.leader.getMapCell();
            }
        }
        if (client == null) {
            this.map.sendToField(new GameRolePlayShowChallengeMessage(getFightCommonInformations()));
        } else {
            client.send(new GameRolePlayShowChallengeMessage(getFightCommonInformations()));
        }
    }

    public void sendFightFlagInfos() {
        this.sendFightFlagInfos(null);
    }

    public void onTeamOptionsChanged(FightTeam team, FightOptionsEnum option) {
        this.sendToField(new GameFightOptionStateUpdateMessage(this.fightId, team.id, option.value, team.isToggled(option)));
        if (this.fightState == fightState.STATE_PLACE) {
            this.map.sendToField(new GameFightOptionStateUpdateMessage(this.fightId, team.id, option.value, team.isToggled(option)));
        }
    }

    //int fightId, byte fightType, FightTeamInformations[] fightTeams, int[] fightTeamsPositions, FightOptionsInformations[] fightTeamsOption
    public FightCommonInformations getFightCommonInformations() {
        return new FightCommonInformations(this.fightId, this.fightType.value, new FightTeamInformations[]{this.myTeam1.getFightTeamInformations(), this.myTeam2.getFightTeamInformations()}, new int[]{this.myTeam1.bladePosition, this.myTeam2.bladePosition}, new FightOptionsInformations[]{this.myTeam1.getFightOptionsInformations(), this.myTeam2.getFightOptionsInformations()});
    }

    public void joinFightTeam(Fighter fighter, FightTeam team, boolean leader, short cell, boolean sendInfos) {
        if (!leader) {
            fighter.joinFight();
        }

        // Ajout a la team
        team.fighterJoin(fighter);

        // On envois l'ajout du joueur a la team sur la map BLADE
        if (this.fightState == fightState.STATE_PLACE) {
            this.map.sendToField(new GameFightUpdateTeamMessage(this.fightId, team.getFightTeamInformations()));
        }

        // cell de combat
        if (cell == -1) {
            fighter.setCell(this.getFreeSpawnCell(team));
        } else {
            fighter.setCell(this.getCell(cell));
        }

        if (fighter instanceof CharacterFighter) {
            this.sendPlacementInformation((CharacterFighter) fighter, true);
        }

        if (sendInfos) {
            this.sendToField(new FieldNotification(new GameFightShowFighterMessage(fighter.getGameContextActorInformations(null))) {
                @Override
                public boolean can(Player perso) {
                    return perso.getID() != fighter.getID();
                }
            });
        }

        // this.sendToField(this.getFightTurnListMessage());
    }

    public void sendPlacementInformation(CharacterFighter fighter, boolean update) {
        if (update) {
            fighter.send(new GameContextDestroyMessage());
            fighter.send(new GameContextCreateMessage((byte) 2));
        }
        fighter.send(new GameFightStartingMessage(fightType.value, getTeam1().LeaderId, getTeam2().LeaderId));
        //TODO FriendUpdateMessage OnContexteChanged

        this.sendGameFightJoinMessage(fighter);
        fighter.send(new GameFightPlacementPossiblePositionsMessage(this.myFightCells.get(myTeam1).keySet().stream().mapToInt(x -> x.intValue()).toArray(), this.myFightCells.get(myTeam2).keySet().stream().mapToInt(x -> x.intValue()).toArray(), fighter.getTeam().id));

        if (!update) {
            CharacterHandler.sendCharacterStatsListMessage(fighter.getCharacter().getClient());
        }
        this.fighters().forEach((Actor) -> {
            fighter.send(new GameFightShowFighterMessage(Actor.getGameContextActorInformations(null)));
        });

        fighter.send(new GameEntitiesDispositionMessage(this.fighters().map(x -> x.GetIdentifiedEntityDispositionInformations()).toArray(IdentifiedEntityDispositionInformations[]::new)));
        fighter.send(new GameFightUpdateTeamMessage(this.fightId, this.getTeam1().getFightTeamInformations()));
        fighter.send(new GameFightUpdateTeamMessage(this.fightId, this.getTeam2().getFightTeamInformations()));
        if (update) {
            this.sendToField(new FieldNotification(new GameFightUpdateTeamMessage(this.fightId, fighter.getTeam().getFightTeamInformations())) {
                @Override
                public boolean can(Player Actor) {
                    return Actor.getID() != fighter.getID();
                }
            });
        }
        this.fighters()
                .filter(fr -> !(fr instanceof VirtualFighter))
                .forEach(fr -> fighter.send(new GameFightHumanReadyStateMessage(fr.getID(), fr.isTurnReady())));
    }

    public void onReconnect(CharacterFighter fighter) {
        this.sendToField(new TextInformationMessage(TextInformationTypeEnum.TEXT_INFORMATION_ERROR, 184, new String[]{fighter.getCharacter().getNickName()}));

        if (this.fightState == fightState.STATE_PLACE) {
            this.sendPlacementInformation(fighter, false);
        } else {
            fighter.send(new GameFightStartingMessage(fightType.value, getTeam1().LeaderId, getTeam2().LeaderId));
            this.sendGameFightJoinMessage(fighter);
            this.fighters().forEach((Actor) -> {
                fighter.send(new GameFightShowFighterMessage(Actor.getGameContextActorInformations(fighter.getCharacter())));
            });
            fighter.send(new GameEntitiesDispositionMessage(this.getAliveFighters().map(x -> x.GetIdentifiedEntityDispositionInformations()).toArray(IdentifiedEntityDispositionInformations[]::new)));
            fighter.send(new GameFightResumeMessage(getFightDispellableEffectExtendedInformations(), getAllGameActionMark(), this.fightWorker.fightTurn, (int) (System.currentTimeMillis() - this.fightTime), getIdols(),
                    fighter.getSpellsController().getInitialCooldown().entrySet()
                            .stream()
                            .map(x -> new GameFightSpellCooldown(x.getKey(), fighter.getSpellsController().minCastInterval(x.getKey()) == 0 ? x.getValue().initialCooldown : fighter.getSpellsController().minCastInterval(x.getKey())))
                            .toArray(GameFightSpellCooldown[]::new),
                    (byte) fighter.getTeam().getAliveFighters()
                            .filter(x -> x.getSummonerID() == fighter.getID() && !(x instanceof BombFighter))
                            .count(),
                    (byte) fighter.getTeam().getAliveFighters()
                            .filter(x -> x.getSummonerID() == fighter.getID() && (x instanceof BombFighter))
                            .count()));
            fighter.send(getFightTurnListMessage());
            fighter.send(new GameFightSynchronizeMessage(this.fighters().map(x -> x.getGameContextActorInformations(fighter.getCharacter())).toArray(GameFightFighterInformations[]::new)));

            /*/213.248.126.93 ChallengeInfoMessage Second8 paket
             /213.248.126.93 ChallengeResultMessage Second9 paket*/
            CharacterHandler.sendCharacterStatsListMessage(fighter.getCharacter().getClient());
            if (this.currentFighter.getID() == fighter.getID()) {
                fighter.send(this.currentFighter.asPlayer().FighterStatsListMessagePacket());
            }
            /*Fighter.send(new GameFightUpdateTeamMessage(this.fightId, this.getTeam1().getFightTeamInformations()));
             Fighter.send(new GameFightUpdateTeamMessage(this.fightId, this.getTeam2().getFightTeamInformations()));*/

            fighter.send(new GameFightNewRoundMessage(this.fightWorker.round));

            fighter.send(new GameFightTurnResumeMessage(this.currentFighter.getID(), this.getTurnTime() / 100, (int) (this.myLoopTimeOut - System.currentTimeMillis()) / 100));
        }
    }

    public GameActionMark[] getAllGameActionMark() {
        GameActionMark[] gameActionMarks = new GameActionMark[0];
        for (CopyOnWriteArrayList<FightActivableObject> objs : this.activableObjects.values()) {
            for (FightActivableObject Object : objs) {
                gameActionMarks = ArrayUtils.add(gameActionMarks, Object.getHiddenGameActionMark());
            }
        }
        return gameActionMarks;
    }

    public FightDispellableEffectExtendedInformations[] getFightDispellableEffectExtendedInformations() {
        FightDispellableEffectExtendedInformations[] FightDispellableEffectExtendedInformations = new FightDispellableEffectExtendedInformations[0];

        for (Stream<BuffEffect> Buffs : (Iterable<Stream<BuffEffect>>) this.getAliveFighters().map(x -> x.getBuff().getAllBuffs())::iterator) {
            for (BuffEffect Buff : (Iterable<BuffEffect>) Buffs::iterator) {
                FightDispellableEffectExtendedInformations = ArrayUtils.add(FightDispellableEffectExtendedInformations, new FightDispellableEffectExtendedInformations(Buff.castInfos.effectType.value(), Buff.caster.getID(), Buff.getAbstractFightDispellableEffect()));
            }
        }
        /*return Stream.of(this.getAliveFighters()
         .map(x -> x.buff.getAllBuffs()
         .map(Buff -> (new FightDispellableEffectExtendedInformations(Buff.castInfos.effectType.value(), Buff.caster.id, Buff.getAbstractFightDispellableEffect())))
         )).toArray(FightDispellableEffectExtendedInformations[]::new);*/
        return FightDispellableEffectExtendedInformations;
    }

    public byte summonCount() {
        return 0;
    }

    public byte bombCount() {
        return 0;
    }

    public Idol[] getIdols() {
        return new Idol[0];
    }

    protected abstract void sendGameFightJoinMessage(Fighter fighter);

    public boolean onlyOneTeam() {
        boolean Team1 = this.myTeam1.getFighters().anyMatch(Player -> !Player.isMarkedDead() && (!Player.isLeft()));
        boolean Team2 = this.myTeam2.getFighters().anyMatch(Player -> !Player.isMarkedDead() && (!Player.isLeft()));
        /*for (Fighter player : fighters()) {
         if ((player.team.id == 0) && (!player.dead) && (!player.left)) {
         Team1 = true;
         }
         if ((player.team.id == 1) && (!player.dead) && (!player.left)) {
         Team2 = true;
         }
         }*/
        return !(Team1 && Team2);
    }

    public synchronized boolean tryEndFight() {
        if (this.getWinners() != null) {
            this.fightLoopState = fightLoopState.STATE_WAIT_END;
            return true;
        }
        return false;
    }

    public boolean startSequence(SequenceTypeEnum sequenceType) {
        this.m_lastSequenceAction = sequenceType;
        ++this.m_sequenceLevel;
        if (this.IsSequencing) {
            return false;
        }
        this.IsSequencing = true;
        this.sequence = sequenceType;
        this.m_sequences.push(sequenceType);
        this.sendToField(new SequenceStartMessage(sequenceType.value, this.currentFighter.getID())); //TODO not Spectator?
        return true;
    }

    public int getNextContextualId() {
        int id = contextualIdProvider.decrementAndGet();
        while (anyFighterMatchId(id)) {
            id = contextualIdProvider.decrementAndGet();
        }
        return id;
    }

    public boolean anyFighterMatchId(final int id) {
        return this.fighters().anyMatch(Fighter -> Fighter.getID() == id);
    }

    public boolean isSequence(SequenceTypeEnum sequenceType) {
        return this.m_sequences.contains(sequenceType);
    }

    public boolean endSequence(SequenceTypeEnum sequenceType) {
        return endSequence(sequenceType, false);
    }

    public boolean endSequence(SequenceTypeEnum sequenceType, boolean force) {
        if (!this.IsSequencing) {
            return false;
        }
        --this.m_sequenceLevel;
        if (this.m_sequenceLevel > 0 && !force) {
            return false;
        }
        this.IsSequencing = false;
        this.waitAcknowledgment = true;
        SequenceTypeEnum sequenceTypeEnum = this.m_sequences.pop();
        if (sequenceTypeEnum != sequenceType) {
            logger.debug("Popped sequence different ({0} != {1})", sequenceTypeEnum.value, sequenceType.value);
        }
        this.sendToField(new SequenceEndMessage(this.m_lastSequenceAction.value, this.currentFighter.getID(), sequenceType.value));
        return true;
    }

    public void endAllSequences() {
        this.m_sequenceLevel = 0;
        this.IsSequencing = false;
        this.waitAcknowledgment = false;
        while (this.m_sequences.size() > 0) {
            this.sendToField(new SequenceEndMessage(this.m_lastSequenceAction.value, this.currentFighter.getID(), this.m_sequences.pop().value));
        }
    }

    public void acknowledgeAction() {
        this.waitAcknowledgment = false;
    }

    protected synchronized void endFight() {
        switch (this.fightState) {
            case STATE_PLACE:
                this.map.sendToField(new GameRolePlayRemoveChallengeMessage(this.fightId));
                break;
            case STATE_FINISH:
                return;
        }

        this.stopTimer("gameLoop");

        this.sendToField(this.myResult);

        this.creationTime = 0;
        this.fightTime = 0;
        this.ageBonus = 0;
        this.endAllSequences();
        this.fighters().forEach(fighter -> fighter.endFight());

        this.kickSpectators(true);

        this.fightCells.values().forEach((c) -> {
            c.clear();
        });
        this.fightCells.clear();
        this.myTimers.clear();
        this.fightWorker.dispose();
        this.myTeam1.dispose();
        this.myTeam2.dispose();

        this.myFightCells = null;
        this.fightCells = null;
        this.myTeam1 = null;
        this.myTeam2 = null;
        this.fightWorker = null;
        this.activableObjects.values().forEach(x -> x.clear());
        this.activableObjects.clear();
        this.activableObjects = null;
        this.myTimers = null;
        this.m_sequences.clear();
        this.m_sequences = null;
        this.contextualIdProvider = null;

        //this.Glyphes = null;
        this.map.removeFight(this);
        this.fightState = fightState.STATE_FINISH;
        this.fightLoopState = fightLoopState.STATE_END_FIGHT;
        if (myTimers != null) {
            try {
                myTimers.values().stream().forEach((CR) -> {
                    CR.cancel();
                });
            } catch (Exception e) {
            } finally {
                myTimers.clear();
                myTimers = null;
            }
        }

    }

    public abstract GameFightEndMessage leftEndMessage(CharacterFighter fighter);

    protected boolean isStarted() {
        return this.fightState != fightState.STATE_INIT && this.fightState != fightState.STATE_PLACE;
    }

    private void kickSpectators(boolean End) {

    }

    public Fighter getFighterOnCell(int cellId) {
        return this.getAliveFighters().filter(Fighter -> Fighter.getCellId() == cellId).findFirst().orElse(null);
    }

    public FightTeam getWinners() {
        if (!this.myTeam1.hasFighterAlive()) {
            return this.myTeam2;
        } else if (!this.myTeam2.hasFighterAlive()) {
            return this.myTeam1;
        }

        return null;
    }

    public FightTeam getLoosers() {
        return this.getEnnemyTeam(this.getWinners());
    }

    /// <summary>
    ///  initialize des tours
    /// </summary>
    /*public void RemakeTurns() {
     this.fightWorker.RemakeTurns(this.fighters());
     }*/
    public synchronized void addNamedParty(CharacterFighter fighter, int outcome) {
        if (fighter instanceof CharacterFighter) {
            if (((CharacterFighter) fighter).getCharacter().getClient() != null && ((CharacterFighter) fighter).getCharacter().getClient().getParty() != null
                    && !((CharacterFighter) fighter).getCharacter().getClient().getParty().partyName.isEmpty()
                    && !Arrays.stream(this.myResult.namedPartyTeamsOutcomes)
                    .anyMatch(x -> x.team.partyName.equalsIgnoreCase(((CharacterFighter) fighter).getCharacter().getClient().getParty().partyName))) {
                this.myResult.namedPartyTeamsOutcomes = ArrayUtils.add(this.myResult.namedPartyTeamsOutcomes, new NamedPartyTeamWithOutcome(new NamedPartyTeam(fighter.getTeam().id, ((CharacterFighter) fighter).getCharacter().getClient().getParty().partyName), outcome));
            }
        }
    }

    public void addActivableObject(Fighter caster, FightActivableObject obj) {
        if (!activableObjects.containsKey(caster)) {
            activableObjects.put(caster, new CopyOnWriteArrayList<>());
        }
        activableObjects.get(caster).add(obj);
    }

    //Dont rename vulnerble
    public Stream<Fighter> fighters() {
        return Stream.concat(this.myTeam1.getFighters(), this.myTeam2.getFighters());
    }

    public Stream<Fighter> getAliveFighters() {
        return Stream.concat(this.myTeam1.getAliveFighters(), this.myTeam2.getAliveFighters());
    }

    public Stream<Fighter> getDeadFighters() {
        //Need Performance Test return Stream.of(this.myTeam1.getDeadFighters(), this.myTeam2.getDeadFighters()).flatMap(x -> x);
        return Stream.concat(this.myTeam1.getDeadFighters(), this.myTeam2.getDeadFighters());
    }

    public FightTeam getTeam1() { //red
        return myTeam1;
    }

    public FightTeam getTeam2() {
        return myTeam2;
    }

    public Fighter getFighter(int FighterId) {
        return this.fighters().filter(x -> x.getID() == FighterId).findFirst().orElse(null);
    }

    public boolean hasObjectOnCell(FightObjectType type, short cell) {
        if (!this.fightCells.containsKey(cell)) {
            return false;
        }
        return this.fightCells.get(cell).hasObject(type);
    }

    public boolean canPutObject(short cellId) {
        if (!this.fightCells.containsKey(cellId)) {
            return false;
        }
        return this.fightCells.get(cellId).canPutObject();
    }

    public boolean isCellWalkable(short CellId) {
        if (!this.fightCells.containsKey(CellId)) {
            return false;
        }

        return this.fightCells.get(CellId).canWalk();
    }

    public FightCell getCell(short CellId) {
        if (this.fightCells.containsKey(CellId)) {
            return this.fightCells.get(CellId);
        }
        return null;
    }

    public Fighter hasEnnemyInCell(short CellId, FightTeam Team) {
        if (CellId == -1) {
            return null;
        }
        return this.fightCells.get(CellId).hasEnnemy(Team);
    }

    public Fighter hasFriendInCell(short CellId, FightTeam Team) {
        if (CellId == -1) {
            return null;
        }
        return this.fightCells.get(CellId).hasFriend(Team);
    }

    public GameFightTurnListMessage getFightTurnListMessage() {
        return new GameFightTurnListMessage(this.getAliveFighters().filter(x -> !(x instanceof StaticFighter))
                .sorted((e1, e2) -> Integer.compare(e2.getInitiative(false), e1.getInitiative(false)))
                .mapToInt(x -> x.getID()).toArray(), this.getDeadFighters().filter(x -> !(x instanceof StaticFighter))
                .mapToInt(x -> x.getID()).toArray());
    }

    private synchronized FightCell getFreeSpawnCell(FightTeam Team) {
        for (FightCell Cell : this.myFightCells.get(Team).values()) {
            if (Cell.canWalk()) {
                return Cell;
            }
        }
        if (!this.myFightCells.get(Team).isEmpty()) {
            return this.myFightCells.get(Team).values().stream().filter(x -> x.getObjectsAsFighter() == null).findFirst().get();
        }
        return null;
    }

    @Override
    public void actorMoved(Path Path, IGameActor Actor, short newCell, byte newDirection) {
        //((Fighter) actor).setCell(fightCells.get(newCell));
        Actor.setDirection(newDirection);
    }

    public void stopTimer(String Name) {
        synchronized ($mutex_lock) {
            try {
                if (this.myTimers.containsKey(Name)) {
                    myTimers.get(Name).cancel();
                    this.myTimers.remove(Name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void startTimer(CancellableScheduledRunnable CR, String Name) {
        synchronized ($mutex_lock) {
            try {
                this.myTimers.put(Name, CR);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public short getPlacementTimeLeft() {
        if (this.isStarted()) {
            return 0;
        }
        double num = (double) (this.getStartTimer() - (Instant.now().getEpochSecond() - this.creationTime)) * 10;
        if (num < 0.0) {
            num = 0.0;
        }
        return (short) num;
    }

    public enum FightLoopState {

        STATE_WAIT_START, STATE_WAIT_TURN, STATE_WAIT_ACTION, STATE_WAIT_READY, STATE_WAIT_END, STATE_WAIT_AI, STATE_END_TURN, STATE_END_FIGHT,
    }

}
