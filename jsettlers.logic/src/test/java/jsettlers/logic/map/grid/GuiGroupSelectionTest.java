package jsettlers.logic.map.grid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsettlers.common.action.SelectGroupAction;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.selectable.ISelectionSet;
import jsettlers.input.GuiInterface;
import jsettlers.logic.buildings.military.occupying.OccupyingBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.military.SoldierMovable;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.player.PlayerSetting;
import jsettlers.main.JSettlersGame;
import jsettlers.network.synchronic.timer.NetworkTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GuiGroupSelectionTest {
    private final List<MainGrid> grids = new ArrayList<>();
    private MainGrid grid;
    private NetworkTimer clock;
    private GuiInterface gui;
    private ISelectionSet selected;
    private ShortPoint2D centered;
    private ISelectionSet selectedWhenCentered;
    private int selectionChanges;

    @Before public void setup() {
        clock = new NetworkTimer(true);
        MatchConstants.init(clock, 0L);
        grid = new MainGrid("group-selection", "Group selection", (short) 64, (short) 64,
                new PlayerSetting[] {new PlayerSetting((byte) 0), new PlayerSetting((byte) 1)});
        grids.add(grid);
        attachGui();
    }

    private void attachGui() {
        IMapInterfaceConnector connector = (IMapInterfaceConnector) Proxy.newProxyInstance(
                IMapInterfaceConnector.class.getClassLoader(), new Class<?>[] {IMapInterfaceConnector.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setSelection")) {
                        selected = (ISelectionSet) args[0];
                        selectionChanges++;
                    } else if (method.getName().equals("scrollTo")) {
                        centered = (ShortPoint2D) args[0];
                        selectedWhenCentered = selected;
                    }
                    return null;
                });
        gui = new GuiInterface(connector, clock, task -> fail("Selection recall must not schedule network tasks"),
                grid.getGuiInputGrid(), () -> {}, (byte) 0, false);
    }

    @After public void cleanup() {
        if (gui != null) gui.stop();
        grids.forEach(MainGrid::stopThreads);
        JSettlersGame.clearState();
    }

    @Test public void actionCopiesIdsDeduplicatesAndCannotBeChangedAfterDispatch() {
        List<Integer> ids = new ArrayList<>(Arrays.asList(1, 2, 1, null));
        SelectGroupAction action = new SelectGroupAction(ids, true);
        ids.clear();
        assertEquals(Arrays.asList(1, 2), action.getIds());
        assertTrue(action.isCenter());
        try {
            action.getIds().add(3);
            fail("IDs must be immutable");
        } catch (UnsupportedOperationException expected) {
            // The scheduled local action keeps its original snapshot.
        }
    }

    @Test public void selectsCurrentOwnUnitsAndCentersAfterSelectionWithoutNetworkCommands() {
        Movable first = spawn(EMovableType.SWORDSMAN_L1, 0, 8, 8);
        Movable second = spawn(EMovableType.PIKEMAN_L1, 0, 12, 12);
        Movable enemy = spawn(EMovableType.SWORDSMAN_L1, 1, 20, 20);
        Movable hidden = spawn(EMovableType.SWORDSMAN_L1, 0, 15, 15);
        hidden.setVisible(false);
        Movable dead = spawn(EMovableType.SWORDSMAN_L1, 0, 18, 18);
        dead.kill();
        Movable worker = spawn(EMovableType.DIGGER, 0, 22, 22);
        gui.action(new SelectGroupAction(Arrays.asList(first.getID(), second.getID(), enemy.getID(),
                hidden.getID(), dead.getID(), worker.getID(), Integer.MAX_VALUE), true));
        assertEquals(2, selected.getSize());
        assertTrue(selected.contains(first));
        assertTrue(selected.contains(second));
        assertFalse(enemy.isSelected());
        assertFalse(hidden.isSelected());
        assertFalse(worker.isSelected());
        assertEquals(new ShortPoint2D(10, 10), centered);
        assertSame(selected, selectedWhenCentered);
        assertEquals(1, selectionChanges);
    }

    @Test public void emptyAndInvalidGroupsPreserveSelectionAndDoNotMoveCamera() {
        Movable original = spawn(EMovableType.SWORDSMAN_L1, 0, 8, 8);
        gui.action(new SelectGroupAction(List.of(original.getID()), false));
        ISelectionSet before = selected;
        gui.action(new SelectGroupAction(List.of(), true));
        gui.action(new SelectGroupAction(List.of(Integer.MAX_VALUE), true));
        assertSame(before, selected);
        assertTrue(original.isSelected());
        assertNull(centered);
        assertEquals(1, selectionChanges);
    }

    @Test public void resolvesReplacementInstanceRatherThanKeepingStaleUnitReference() {
        Movable original = spawn(EMovableType.SWORDSMAN_L1, 0, 8, 8);
        int id = original.getID();
        ((AttackableHumanMovable) original).defectTo(original.getPlayer());
        Movable replacement = (Movable) MovableManager.getMovableByID(id);
        assertNotSame(original, replacement);
        gui.action(new SelectGroupAction(List.of(id), false));
        assertSame(replacement, selected.get(0));
        assertFalse(original.isAlive());
        assertTrue(replacement.isSelected());
    }

    @Test public void garrisonAndSoldierAssignedToTowerCannotBeRecalledAsFieldUnits() {
        OccupyingBuilding tower = (OccupyingBuilding) grid.constructBuildingAt(new ShortPoint2D(40, 40),
                EBuildingType.TOWER, grid.getPartitionsGrid().getPlayer((byte) 0), true);
        tower.initializeGarrison(List.of(EMovableType.SWORDSMAN_L1));
        SoldierMovable garrison = (SoldierMovable) tower.getOccupiers().get(0).getMovable();
        SoldierMovable assigned = (SoldierMovable) spawn(EMovableType.SWORDSMAN_L1, 0, 8, 8);
        assertTrue(assigned.moveToTower(tower));
        gui.action(new SelectGroupAction(List.of(garrison.getID(), assigned.getID()), true));
        assertEquals(0, selectionChanges);
        assertNull(centered);
    }

    @Test public void specialUnitsCanBeSelectedAndIdsResolveAfterGameLoad() throws Exception {
        Movable priest = spawn(EMovableType.MAGE, 0, 8, 8);
        int id = priest.getID();
        gui.action(new SelectGroupAction(List.of(id), false));
        assertSame(priest, selected.get(0));
        gui.stop();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        GameSerializer serializer = new GameSerializer();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { serializer.save(grid, out); }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            grid = serializer.load(in);
        }
        grids.add(grid);
        attachGui();
        gui.action(new SelectGroupAction(List.of(id), true));
        assertNotSame(priest, selected.get(0));
        assertSame(MovableManager.getMovableByID(id), selected.get(0));
        assertEquals(new ShortPoint2D(8, 8), centered);
    }

    private Movable spawn(EMovableType type, int player, int x, int y) {
        return Movable.createMovable(type, grid.getPartitionsGrid().getPlayer((byte) player),
                new ShortPoint2D(x, y), grid.movablePathfinderGrid);
    }
}
