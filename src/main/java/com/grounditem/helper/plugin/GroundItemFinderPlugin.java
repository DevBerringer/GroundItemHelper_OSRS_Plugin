package com.grounditem.helper.plugin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import com.grounditem.helper.data.GroundItemEntry;
import com.grounditem.helper.panel.GroundItemFinderPanel;
import com.grounditem.helper.overlay.GroundItemHighlightOverlay;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
        name = "Ground Item Finder"
)
@NoArgsConstructor
@AllArgsConstructor
public class GroundItemFinderPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private GroundItemHighlightOverlay groundItemHighlightOverlay;

    private final List<GroundItemEntry> nearbyItems = new ArrayList<>();

    private WorldPoint currentlyHighlightedTile = null;
    private GroundItemEntry currentlyHighlightedItem = null;
    private GroundItemFinderPanel panel;
    private NavigationButton navButton;

    @Override
    protected void startUp() {
        panel = new GroundItemFinderPanel(itemManager, this::highlightTile);
        BufferedImage icon = getBufferedImageIcon();

        navButton = NavigationButton.builder()
                .tooltip("Ground Item Finder")
                .icon(icon)
                .priority(6)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        overlayManager.add(groundItemHighlightOverlay);

        log.info("Ground Item Finder Plugin started");
    }

    // In shutDown method, also clear currentlyHighlightedItem
    @Override
    protected void shutDown() {
        nearbyItems.clear();

        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }

        overlayManager.remove(groundItemHighlightOverlay);
        groundItemHighlightOverlay.clearHighlightedTile();
        currentlyHighlightedItem = null;

        panel = null;

        log.info("Ground Item Finder Plugin stopped");
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        scanGroundItems();
        if (panel != null)
        {
            panel.updateItems(nearbyItems);
        }

        if (currentlyHighlightedItem != null) {
            boolean foundSpecificItem = false;
            for (GroundItemEntry item : nearbyItems) {
                if (item.getItemId() == currentlyHighlightedItem.getItemId() &&
                        item.getLocation().equals(currentlyHighlightedItem.getLocation()) &&
                        item.getQuantity() == currentlyHighlightedItem.getQuantity())
                {
                    foundSpecificItem = true;
                    break;
                }
            }
            if (!foundSpecificItem) {
                clearHighlight(currentlyHighlightedItem.getLocation());
                currentlyHighlightedItem = null;
            }
        }
    }

    private BufferedImage getBufferedImageIcon() {
        try {
            return ImageIO.read(Objects.requireNonNull(GroundItemFinderPlugin.class.getResourceAsStream("/search-icon.png")));
        }
        catch (IOException ex)
        {
            log.warn("Failed to load panel icon", ex);
            return null;
        }
    }

    private void highlightTile(GroundItemEntry itemToHighlight) // Change parameter to GroundItemEntry
    {
        groundItemHighlightOverlay.setHighlightedTile(itemToHighlight.getLocation());
        this.currentlyHighlightedItem = itemToHighlight;
        this.currentlyHighlightedTile = itemToHighlight.getLocation();
    }

    private void clearHighlight(WorldPoint worldPoint) // Renamed parameter to avoid confusion, it's not strictly about the passed point
    {
        if (Objects.nonNull(this.currentlyHighlightedTile)) {
            groundItemHighlightOverlay.clearHighlightedTile();
            this.currentlyHighlightedTile = null;
        }
    }

    private void scanGroundItems()
    {
        nearbyItems.clear();

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null)
            return;

        int plane = localPlayer.getWorldLocation().getPlane();

        Tile[][] tiles = client.getScene().getTiles()[plane];
        if (tiles == null)
            return;

        Scene scene = client.getScene();
        if (scene == null)
            return;

        LocalPoint playerLocal = localPlayer.getLocalLocation();
        if (playerLocal == null)
            return;

        int playerX = playerLocal.getSceneX();
        int playerY = playerLocal.getSceneY();

        int radius = 3; // 9x9 area

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                int scanX = playerX + dx;
                int scanY = playerY + dy;

                if (scanX < 0 || scanX >= Constants.SCENE_SIZE) // Use Constants.SCENE_SIZE for robustness
                    continue;
                if (scanY < 0 || scanY >= Constants.SCENE_SIZE) // Use Constants.SCENE_SIZE for robustness
                    continue;

                Tile tile = tiles[scanX][scanY];
                if (tile == null)
                    continue;

                List<TileItem> groundItems = tile.getGroundItems();

                if (groundItems != null)
                {
                    for (TileItem tileItem : groundItems)
                    {
                        ItemComposition itemDefinition = client.getItemDefinition(tileItem.getId());
                        WorldPoint worldPoint = WorldPoint.fromLocal(client, tile.getLocalLocation());
                        String name = itemDefinition.getName();
                        nearbyItems.add(new GroundItemEntry(name, tileItem.getId(), tileItem.getQuantity(), worldPoint));
                    }
                }
            }
        }
    }
}