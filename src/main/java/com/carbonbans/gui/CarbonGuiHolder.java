package com.carbonbans.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CarbonGuiHolder implements InventoryHolder {

    private Inventory inventory;
    private final GuiType type;
    private int page;
    private final Object context; // target player uuid for ALTS, null otherwise

    public CarbonGuiHolder(GuiType type, int page, Object context) {
        this.type = type;
        this.page = page;
        this.context = context;
    }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }

    public GuiType getType() { return type; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public Object getContext() { return context; }
}