import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import java.awt.*;

@ScriptManifest(name = "ChopChop", description = "Participates in RuneScape Deforestation", author = "Gab & Scennix", version = 1.0, category = Category.WOODCUTTING, image = "")
public class TestScript extends AbstractScript {
    private final Area bank = new Area(3207, 3220, 3209, 3216, 2);
    private final Area trees = new Area(3195, 3215, 3179, 3240);
    private final Player me = Players.getLocal();
    private boolean firstTime = true;
    private int treesChopped = 0;

    @Override
    public void onStart() {
        Logger.log("ChopChop script has started!");
    }

    @Override
    public int onLoop() {
        if (firstTime) {
            Logger.log("First time");
            if (!bank.contains(me.getTile())) {
                Logger.log("Going to bank");
                goTo(bank, "Bank");
                return Calculations.random(500, 1500);
            } else {
                Logger.log("Arrived at bank");
                Bank.open();
                Sleep.sleepUntil(Bank::isOpen, 5000);
                depositEquipment();
                depositItems();
                String bestAxe = bestAvailableAxe();
                Logger.log("Best axe available: " + bestAxe);
                if (bestAxe.equals("noaxe")) {
                    Logger.log("Suitable axe not found in bank...");
                } else {
                    Logger.log("Equipping axe");
                    while (Inventory.isEmpty()) {
                        Bank.withdraw(bestAxe);
                        Sleep.sleepUntil(() -> !Inventory.isEmpty(), 5000);
                    }
                    while (Bank.isOpen()) {
                        Bank.close();
                        Sleep.sleepUntil(() -> !Bank.isOpen(), 5000);
                    }
                    Inventory.interact(bestAxe, "Wield");
                }
                firstTime = false;
                return Calculations.random(500, 1500);
            }
        }
        if (Inventory.isFull()) {
            Logger.log("Inventory is full");
            if (!bank.contains(me.getTile())) {
                goTo(bank, "Bank");
                return Calculations.random(500, 1500);
            } else {
                Logger.log("Depositing all items");
                while (!Bank.isOpen()) {
                    Bank.open();
                    Sleep.sleepUntil(Bank::isOpen, 5000);
                }
                depositItems();
                return Calculations.random(500, 1500);
            }
        } else {
            if (!trees.contains(me.getTile())) {
                goTo(trees, "Trees");
                return Calculations.random(500, 1500);
            } else if (!me.isAnimating() && !me.isMoving()) {
                Logger.log("Finding nearest Tree");
                GameObject tree = GameObjects
                        .closest(t -> t.getName().equalsIgnoreCase("tree") && trees.contains(t.getTile()));
                if (tree != null && tree.interact("Chop down")) {
                    Logger.log("Chopping tree");
                    Sleep.sleepUntil(() -> !me.isAnimating(), 13000);
                }
                treesChopped++;
            }
        }
        return Calculations.random(500, 1500);
    }

    @Override
    public void onExit() {
        Logger.log("ChopChop script has ended!");
    }

    @Override
    public void onPaint(Graphics g) {
        int fromTop = 35;
        g.setColor(Color.WHITE);
        g.drawString("Trees chopped: " + treesChopped, 10, fromTop);
        fromTop += 20;
        g.drawString("Total profit: " + (treesChopped * 58), 10, fromTop);
    }


    public void goTo(Area target, String description) {
        Logger.log("Begin navigation to: " + description);
        Player me = Players.getLocal();
        while (!target.contains(me.getTile())) {
            if (!me.isMoving()) {
                Walking.walk(target.getRandomTile());
            }
            Sleep.sleep(500, 3500);
        }
        Logger.log("You are now in: " + description);
    }

    public String bestAvailableAxe() {
        int atk = Skills.getRealLevel(Skill.ATTACK);
        int wc = Skills.getRealLevel(Skill.WOODCUTTING);
        int[] levels = new int[] { 1, 1, 6, 11, 21, 31, 41 };
        String[] axes = new String[] { "Bronze axe", "Iron axe", "Steel axe", "Black axe", "Mithril axe", "Adamant axe",
                "Rune axe" };
        boolean done = false;
        for (int i = levels.length - 1; i >= 0; i--) {
            if (wc >= levels[i] && atk >= levels[i] - 1 && Bank.contains(axes[i])) {
                return axes[i];
            }
        }
        return "noaxe";
    }

    public void depositItems() {
        if (Bank.isOpen()) {
            Bank.depositAllItems();
            Sleep.sleepUntil(Inventory::isEmpty, 5000);
        }
    }

    public void depositEquipment() {
        if (Bank.isOpen()) {
            Bank.depositAllEquipment();
            Sleep.sleepUntil(Equipment::isEmpty, 5000);
        }
    }
}
