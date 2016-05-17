package org.inventivetalent.inventoryfill;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.boundingbox.BoundingBox;
import org.inventivetalent.boundingbox.BoundingBoxAPI;
import org.inventivetalent.pluginannotations.PluginAnnotations;
import org.inventivetalent.pluginannotations.command.Command;
import org.inventivetalent.pluginannotations.command.OptionalArg;
import org.inventivetalent.pluginannotations.command.Permission;
import org.inventivetalent.vectors.d3.Vector3DDouble;

import java.util.List;
import java.util.Set;

public class InventoryFill extends JavaPlugin implements Listener {

	@Override
	public void onEnable() {
		PluginAnnotations.loadAll(this, this);
	}

	@Command(name = "fillinventory",
			 aliases = {
					 "inventoryfill",
					 "fillinv",
					 "fill" },
			 usage = "<Item[:Data][xAmount]> [replace]",
			 description = "Fill the inventory you are looking at\n" + "item - item type\n" + "data - item data\n" + "amount - amount per slot",
			 min = 1,
			 max = 2,
			 fallbackPrefix = "inventoryfill")
	@Permission("inventoryfill.fill")
	public void inventoryFill(Player sender, String itemString, @OptionalArg(def = "false") Boolean replace) {
		Inventory inventory = null;

		Block targetBlock = sender.getTargetBlock((Set<Material>) null, 16);
		if (targetBlock != null && (targetBlock.getState() instanceof InventoryHolder)) {
			inventory = ((InventoryHolder) targetBlock.getState()).getInventory();
		} else {
			List<Entity> entities = sender.getNearbyEntities(16, 16, 16);
			Entity[] entityArray = entities.toArray(new Entity[entities.size()]);
			BoundingBox[] boxArray = new BoundingBox[entityArray.length];
			for (int i = 0; i < boxArray.length; i++) {
				boxArray[i] = BoundingBoxAPI.getAbsoluteBoundingBox(entityArray[i]);
			}

			Vector3DDouble locationVector = new Vector3DDouble(sender.getLocation());
			Vector3DDouble direction = new Vector3DDouble(sender.getLocation().getDirection());

			doubleLoop:
			for (double d = 0; d < 16; d += 0.05) {
				Vector3DDouble vector = direction.clone().multiply(d).add(0, sender.getEyeHeight(), 0).add(locationVector);
				for (int i = 0; i < boxArray.length; i++) {
					if (boxArray[i].contains(vector)) {
						Entity entity = entityArray[i];
						if (entity instanceof InventoryHolder) {
							inventory = ((InventoryHolder) entity).getInventory();
							break doubleLoop;
						}
					}
				}
			}
		}

		if (inventory == null) {
			sender.sendMessage("§cYou are not looking at an inventory! Use /fillMyInventory to fill your own inventory");
			return;
		}

		ItemStack item;
		try {
			item = parseItem(itemString);
		} catch (Exception e) {
			sender.sendMessage("§cInvalid item");
			return;
		}
		fill(inventory, item, replace);
		sender.sendMessage("§aInventory filled!");
	}

	@Command(name = "fillmyinventory",
			 aliases = {
					 "fillmyinv",
					 "fillme" },
			 usage = "<Item[:Data][xAmount]> [replace]",
			 description = "Fill your own inventory\n" + "item - item type\n" + "data - item data\n" + "amount - amount per slot",
			 min = 1,
			 max = 2,
			 fallbackPrefix = "inventoryfill")
	@Permission("inventoryfill.self")
	public void fillMyInventory(Player sender, String itemString, @OptionalArg(def = "false") Boolean replace) {
		ItemStack item;
		try {
			item = parseItem(itemString);
		} catch (Exception e) {
			sender.sendMessage("§cInvalid item");
			return;
		}
		fill(sender.getInventory(), item, replace);
		sender.sendMessage("§aInventory filled!");
	}

	void fill(Inventory inventory, ItemStack itemStack, boolean replace) {
		for (int i = 0; i < inventory.getSize(); i++) {
			if (!replace) {
				if (inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) { continue; }
			}
			inventory.setItem(i, itemStack);
		}
	}

	ItemStack parseItem(String string) {
		int amount = 1;
		Material material;
		byte data = 0;

		if (string.contains("x")) {
			String[] split = string.split("x");
			amount = Integer.parseInt(split[1]);
			string = split[0];
		}

		if (string.contains(":")) {
			String[] split = string.split(":");
			material = Material.valueOf(split[0].toUpperCase());
			data = Byte.valueOf(split[1]);
		} else {
			material = Material.valueOf(string.toUpperCase());
		}

		return new ItemStack(material, amount, (short) 0, data);
	}

}
