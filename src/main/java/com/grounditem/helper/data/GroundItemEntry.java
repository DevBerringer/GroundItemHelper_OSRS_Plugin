package com.grounditem.helper.data;

import lombok.Value;
import net.runelite.api.coords.WorldPoint;

@Value
public class GroundItemEntry
{
    String name;
    int itemId;
    int quantity;
    WorldPoint location;
}
