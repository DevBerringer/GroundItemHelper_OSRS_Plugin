# RuneLite Ground Item Finder Plugin

A RuneLite plugin that helps you easily locate and manage ground items in RuneScape. This plugin displays a list of nearby ground items in a dedicated panel, allowing you to sort them and highlight their exact location in the game world.

## Features

* **List Nearby Ground Items:** Displays all ground items within a configurable radius around your player.
* **Sortable List:**
    * Sort items by **Name** (A-Z or Z-A).
    * Sort items by **Quantity** (Low-High or High-Low).
    * Sort items by **Value** (Low-High or High-Low).
* **Highlight Ground Items:** Click a "Highlight" button next to an item in the list to draw a visual highlight on its tile in the game world.
* **Dynamic Highlight Clearing:** The highlight will automatically disappear when the specific highlighted item is picked up from the ground, even if other items remain on the same tile.
* **Efficient Scanning:** Ground items are scanned periodically (every few game ticks) to optimize performance, rather than every single tick.

## Installation

### From RuneLite Plugin Hub (Recommended)

Once this plugin is approved and available on the RuneLite Plugin Hub, you will be able to install it directly from the RuneLite client:

1.  Open the RuneLite client.
2.  Click the wrench icon to open the **Plugin Hub**.
3.  Search for "Ground Item Finder".
4.  Click the "Install" button next to the plugin.

### Manual Installation (For Developers/Testing)

If you wish to build and install the plugin manually:

1.  **Clone the Repository:**
    ```bash
    git clone <your-repository-url>
    cd ground-item-finder-plugin
    ```
2.  **Build the Project:**
    Using Maven:
    ```bash
    mvn clean install
    ```
3.  **Locate the JAR File:**
    The compiled JAR file will be in the `target/` directory (e.g., `ground-item-finder-plugin-1.0-SNAPSHOT.jar`).
4.  **Move to RuneLite Plugins Folder:**
    Copy this JAR file into your RuneLite plugins folder. The location varies by operating system:
    * **Windows:** `C:\Users\<YourUsername>\.runelite\externalplugins`
    * **macOS:** `~/Library/Application Support/RuneLite/externalplugins`
    * **Linux:** `~/.runelite/externalplugins`
5.  **Restart RuneLite:**
    Close and reopen your RuneLite client.

## Usage

1.  After installation, a new sidebar icon (a magnifying glass or similar search icon) will appear in your RuneLite client.
2.  Click this icon to open the **Ground Item Finder** panel.
3.  The panel will display a list of all ground items near your character.
4.  Use the "Sort by" dropdown at the top of the panel to reorder the list based on your preference (Name, Quantity, or Value).
5.  To highlight an item's location in the game world, click the "Highlight" button next to the item in the list.
6.  The highlighted tile will be outlined in yellow.
7.  The highlight will automatically disappear once you pick up the specific item you highlighted.

## Development

### Technologies Used

* Java
* RuneLite API
* Swing (for UI)

### Project Structure

* `com.grounditem.helper.plugin.GroundItemFinderPlugin`: The main plugin class, handles game tick events, item scanning, and UI panel management.
* `com.grounditem.helper.panel.GroundItemFinderPanel`: The Swing panel that displays the list of ground items and provides sorting options.
* `com.grounditem.helper.domain.GroundItemEntry`: A data class representing a single ground item with its name, ID, quantity, and location.
* `com.grounditem.helper.domain.enums.GroundItemSortType`: An enum defining the available sorting methods for the ground item list.
* `com.grounditem.helper.overlay.GroundItemHighlightOverlay`: Handles the drawing of the highlight on the game world tile.

## Contributing

Contributions are welcome! If you find a bug or have a feature suggestion, please open an issue or submit a pull request.