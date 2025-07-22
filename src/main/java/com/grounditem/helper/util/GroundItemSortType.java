package com.grounditem.helper.util;

public enum GroundItemSortType
{
    NAME_ASC("Name (A-Z)"),
    QUANTITY_ASC("Quantity (Low-High)"),
    QUANTITY_DESC("Quantity (High-Low)"),
    VALUE_DESC("Value (High-Low)");

    private final String name;

    GroundItemSortType(String name)
    {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}