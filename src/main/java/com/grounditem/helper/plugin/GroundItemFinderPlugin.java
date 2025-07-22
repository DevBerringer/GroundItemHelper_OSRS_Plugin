package com.grounditem.helper.plugin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import com.grounditem.helper.domain.GroundItemEntry;
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
        name = "GroundItems Finder"
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

    private GroundItemFinderPanel panel;
    private NavigationButton navButton;

    private final List<GroundItemEntry> nearbyItems = new ArrayList<>();
    private WorldPoint currentlyHighlightedTile = null;
    private GroundItemEntry currentlyHighlightedItem = null;

    /**
     * Called when the plugin is enabled.
     * <p>
     * This method initializes the plugin's components, including:
     * <ul>
     * <li>Creating the {@link GroundItemFinderPanel} and setting its highlight callback.</li>
     * <li>Loading the plugin's navigation icon.</li>
     * <li>Building and adding the navigation button to the RuneLite client toolbar.</li>
     * <li>Adding the {@link GroundItemHighlightOverlay} to the overlay manager for rendering.</li>
     * </ul>
     * A log message is also produced to indicate the plugin has started.
     */
    @Override
    protected void startUp() {
        panel = new GroundItemFinderPanel(client, itemManager, this::highlightTile);
        BufferedImage icon = getBufferedImageIcon();

        navButton = NavigationButton.builder()
                .tooltip("GroundItems Finder")
                .icon(icon)
                .priority(6)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        overlayManager.add(groundItemHighlightOverlay);
    }

    /**
     * Called when the plugin is disabled.
     * <p>
     * This method cleans up resources used by the plugin, including:
     * <ul>
     * <li>Clearing the list of nearby items.</li>
     * <li>Removing the navigation button from the client toolbar if it exists.</li>
     * <li>Removing the {@link GroundItemHighlightOverlay} from the overlay manager.</li>
     * <li>Clearing any active item highlight on the overlay.</li>
     * <li>Resetting the {@link #currentlyHighlightedItem} to null.</li>
     * <li>Setting the panel reference to null.</li>
     * </ul>
     * A log message is also produced to indicate the plugin has stopped.
     */
    @Override
    protected void shutDown() {
        nearbyItems.clear();

        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }

        overlayManager.remove(groundItemHighlightOverlay);
        groundItemHighlightOverlay.clearHighlightedTile();
        currentlyHighlightedItem = null; // Clear the stored highlighted item

        panel = null;
    }

    /**
     * Subscribes to and handles {@link GameTick} events from the RuneLite client.
     * <p>
     * This method is invoked once every game tick (approximately 0.6 seconds). It performs the following operations:
     * <ul>
     * <li>Calls {@code scanGroundItems()} to refresh the list of nearby ground items.</li>
     * <li>Updates the {@link GroundItemFinderPanel} with the latest list of nearby items if the panel is active.</li>
     * <li>Checks if a specific item is currently highlighted. If so, it iterates through the {@link #nearbyItems}
     * list to determine if the previously highlighted item (identified by its ID, quantity, and exact location)
     * is still present on the ground.</li>
     * <li>If the {@link #currentlyHighlightedItem} is no longer found on the ground, the highlight is cleared
     * from the game world, and the internal reference to the highlighted item is set to null.</li>
     * </ul>
     *
     * @param event The {@link GameTick} event, triggered by the game engine.
     */
    @Subscribe
    public void onGameTick(GameTick event) {
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
                clearHighlight();
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

    private void highlightTile(GroundItemEntry itemToHighlight) {
        groundItemHighlightOverlay.setHighlightedTile(itemToHighlight.getLocation());
        this.currentlyHighlightedItem = itemToHighlight;
        this.currentlyHighlightedTile = itemToHighlight.getLocation();
    }

    private void clearHighlight() {
        if (Objects.nonNull(this.currentlyHighlightedTile)) {
            groundItemHighlightOverlay.clearHighlightedTile();
            this.currentlyHighlightedTile = null;
        }
    }

    private void scanGroundItems() {
        nearbyItems.clear();

        Player player = client.getLocalPlayer();
        if (player == null)
            return;

        LocalPoint playerLocal = player.getLocalLocation();
        if (playerLocal == null)
            return;

        int playerX = playerLocal.getSceneX();
        int playerY = playerLocal.getSceneY();

        int radius = 3; // 9x9 area

        Tile[][] tiles = client.getScene().getTiles()[client.getPlane()];

        for (int dx = -radius; dx <= radius; dx++)
        {
            for (int dy = -radius; dy <= radius; dy++)
            {
                int scanX = playerX + dx;
                int scanY = playerY + dy;

                if (scanX < 0 || scanX >= Constants.SCENE_SIZE)
                    continue;
                if (scanY < 0 || scanY >= Constants.SCENE_SIZE)
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

                        long value = (long) itemManager.getItemPrice(tileItem.getId()) * tileItem.getQuantity();

                        nearbyItems.add(new GroundItemEntry(name, tileItem.getId(), tileItem.getQuantity(), worldPoint, value));
                    }
                }
            }
        }
    }
}
