package org.powerbot.game.api.methods.tab;

import java.util.Arrays;
import java.util.LinkedList;

import org.powerbot.game.api.methods.Tabs;
import org.powerbot.game.api.methods.Widgets;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.tab.Inventory;
import org.powerbot.game.api.util.Filter;
import org.powerbot.game.api.wrappers.node.Item;
import org.powerbot.game.api.wrappers.widget.Widget;
import org.powerbot.game.api.wrappers.widget.WidgetChild;

public class Equipment {

	private static final int[] WIDGET_PARENTS = {387, 667};
	
	public enum Slot {
		
		HELMET(6, 0, 0),
		CAPE(9, 1, 1),
		NECK(12, 2, 2),
		WEAPON(15, 3, 3),
		BODY(18, 4, 4),
		SHIELD(21, 5, 5),
		LEGS(24, 7, 7),
		HANDS(27, 9, 9),
		FEET(30, 10, 10),
		RING(33, 12, 12),
		AMMO(37, 13, 13),
		AURA(46, 14, 14);		
		
		private final int tabChildIndex;
		private final int bankChildIndex;
		private final int appearanceIndex;
		
		Slot(final int tabChildIndex, final int bankChildIndex, final int appearanceIndex) {
			this.tabChildIndex = tabChildIndex;
			this.bankChildIndex = bankChildIndex;
			this.appearanceIndex = appearanceIndex;
		}
		
		public int getAppearanceIndex() {
			return appearanceIndex;
		}
		
		public int getIndex() {
			return getWidget().getId() == 387 ? tabChildIndex : bankChildIndex;
		}
		
	}
	
	public static boolean isOpen() {
		return Tabs.EQUIPMENT.isOpen();
	}
	
	public static boolean open() {
		return open(false);
	}
	
	public static boolean open(final boolean functionKeys) {
		return Tabs.EQUIPMENT.open(functionKeys);
	}
	
	public static Widget getWidget() {
		for (final int i : WIDGET_PARENTS) {
			final Widget widget = Widgets.get(i);
			if (widget.validate()) {
				return widget;
			}
		}
		return null;
	}
	
	public static Item[] getItems() {
		return getItems(false);
	}
	
	public static Item[] getItems(final boolean cached) {
		final LinkedList<Item> items = new LinkedList<Item>();
		final Widget parent = getWidget();
		if (parent == null) {
			Tabs.EQUIPMENT.open();
		}
		final int id = parent == null ? -1 : parent.getId();
		final WidgetChild[] children = id == 387 ? parent.getChildren() : parent.getChild(9).getChildren();
		final Slot[] slots = Slot.values();
		if (!cached) {
			open();
		} 
		final int[] ids = Players.getLocal().getAppearance();
		for (final Slot slot : slots) {
			final WidgetChild child = children[slot.getIndex()];
			if (cached && !isOpen()) {
				final int idx = slot.getAppearanceIndex();
				if((idx == 12 || idx == 13) && parent.validate()) {
					final Slot theSlot = (idx == 12 ? Slot.RING : Slot.AMMO);
					items.add(new Item(children[theSlot.getIndex()]));
					continue;
				}
				items.add((ids[idx] == child.getChildId() ? new Item(child) : new Item(ids[idx], 1)));
				continue;
			}
			items.add(new Item(children[slot.getIndex()]));
		}
		return items.toArray(new Item[items.size()]);
	}
	
	public static Item getItem(final Slot slot) {
		return getItem(false, slot);
	}
	
	public static Item getItem(final boolean cached, final Slot slot) {
		return getItems(cached)[slot.ordinal()];
	}
	
	public static Item getItem(final int id) {
		return getItem(false, id);
	}
	
	public static Item getItem(final boolean cached, final int id) {
		for (final Item item : getItems(cached)) {
			if (item.getId() == id) {
				return item;
			}
		}
		return null;
	}
	
	public static boolean contains(final int id) {
		return contains(false, id);
	}
	
	public static boolean contains(final boolean cached, final int id) {
		return getItem(cached, id) != null;
	}
	
	public static boolean containsAll(final int...ids) {
		return containsAll(false, ids);
	}
	
	public static boolean containsAll(final boolean cached, final int...ids) {
		for (final int id : ids) {
			if (!contains(cached, id)) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean unequip(final Slot slot) {
		return unequip(getItem(slot));
	}
	
	public static boolean unequip(final Item item) {
		return unequip(item.getId());
	}
	
	public static boolean unequip(final int id) {
		if (!isOpen()) {
			open();
		}
		Item item = getItem(id);
		if (item != null) {
			if (item.getWidgetChild().interact("Remove")) {
				final long timeout = System.currentTimeMillis();
				while (((System.currentTimeMillis() - timeout) < 3000) && (item = getItem(id)) != null);
				if (item == null) {
					return true;
				}
			}
		}
		return !contains(id);
	}
	
	public static boolean equip(final int...ids) {
		return equip(Inventory.getItems(new Filter<Item>() {
			@Override
			public boolean accept(final Item item) {
				Arrays.sort(ids);
				return Arrays.binarySearch(ids, item.getId()) >= 0;
			}
		}));
	}
	
	public static boolean equip(final Item...items) {
		int count = items.length;
		int interacted = 0;
		for (final Item item : items) {
			if (item == null) {
				count -= 1;
				continue;
			}
			final int preCount = Inventory.getCount(true, item.getId());
			final WidgetChild child = item.getWidgetChild();
			String interact = "Wield";
			for (final String string : child.getActions()) {
				if (string == null) {
					continue;
				}
				if (string.contains("Wear") || string.contains("Equip") || string.contains("Wield")) {
					interact = string;
					break;
				}
			}
			if (child.interact(interact)) {
				final long timeout = System.currentTimeMillis();
				while (((System.currentTimeMillis() - timeout) < 3000) && (Inventory.getCount(true, item.getId()) == preCount));
				if (Inventory.getCount(true, item.getId()) != preCount) {
					interacted += 1;
				}
			}
		}
		return interacted == count;
	}
}
