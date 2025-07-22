package com.grounditem.helper.overlay;

import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.Objects;

public class GroundItemHighlightOverlay extends Overlay
{
    private final Client client;

    @Setter
    private WorldPoint highlightedTile; // The tile to highlight

    @Inject
    public GroundItemHighlightOverlay(Client client, TooltipManager tooltipManager)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    public void clearHighlightedTile()
    {
        this.highlightedTile = null;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (highlightedTile == null)
        {
            return null; // Nothing to draw
        }

        if (highlightedTile.getPlane() == client.getPlane())
        {
            LocalPoint lp = LocalPoint.fromWorld(client, highlightedTile);
            if (lp != null)
            {
                Polygon tilePoly = Perspective.getCanvasTilePoly(client, lp);
                if (Objects.nonNull(tilePoly))
                {
                    graphics.setColor(new Color(96, 186, 0, 80)); // Yellow, semi-transparent fill
                    graphics.fill(tilePoly);
                    graphics.setColor(Color.YELLOW); // Yellow border
                    graphics.setStroke(new BasicStroke(2)); // Thicker border
                    graphics.draw(tilePoly);
                }
            }
        }

        return null;
    }
}
