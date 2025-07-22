package com.grounditem.helper.panel;

import com.grounditem.helper.data.GroundItemEntry;
import com.grounditem.helper.util.GroundItemSortType; // Import the new enum
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.api.ItemComposition; // Needed for item price

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class GroundItemFinderPanel extends PluginPanel
{
    private final JPanel listPanel;
    private final ItemManager itemManager;
    private final Consumer<GroundItemEntry> itemHighlightCallback; // Changed from Consumer<WorldPoint>

    private List<GroundItemEntry> currentItems;
    private GroundItemSortType currentSortType = GroundItemSortType.VALUE_DESC;

    private final JComboBox<GroundItemSortType> sortComboBox;

    public GroundItemFinderPanel(ItemManager itemManager, Consumer<GroundItemEntry> itemHighlightCallback) // Changed parameter type
    {
        super();
        this.itemManager = itemManager;
        this.itemHighlightCallback = itemHighlightCallback; // Assign the new Consumer
        setLayout(new BorderLayout());

        // --- Top Panel for controls (e.g., sort) ---
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        controlPanel.setBorder(new EmptyBorder(5, 5, 0, 5));

        controlPanel.add(new JLabel("Sort by:"));
        sortComboBox = new JComboBox<>(GroundItemSortType.values());
        sortComboBox.setSelectedItem(currentSortType); // Set default selection
        sortComboBox.addActionListener(e -> {
            currentSortType = (GroundItemSortType) sortComboBox.getSelectedItem();
            if (currentItems != null) {
                updateItems(currentItems); // Re-sort and update the list
            }
        });
        controlPanel.add(sortComboBox);

        add(controlPanel, BorderLayout.NORTH); // Add control panel to the top

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateItems(List<GroundItemEntry> items)
    {
        this.currentItems = items; // Store the items for re-sorting

        listPanel.removeAll();

        if (items.isEmpty())
        {
            listPanel.add(new JLabel("No nearby ground items."));
        }
        else
        {
            List<GroundItemEntry> sortedItems = new java.util.ArrayList<>(items); // Create a mutable copy
            sortItems(sortedItems); // Sort the items

            for (GroundItemEntry item : sortedItems) // Iterate over sorted items
            {
                listPanel.add(createGroundItemEntryPanel(item));
                listPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            listPanel.add(Box.createVerticalGlue());
        }

        revalidate();
        repaint();
    }

    private void sortItems(List<GroundItemEntry> items)
    {
        Comparator<GroundItemEntry> comparator;

        switch (currentSortType)
        {
            case NAME_ASC:
                comparator = Comparator.comparing(GroundItemEntry::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case QUANTITY_ASC:
                comparator = Comparator.comparingInt(GroundItemEntry::getQuantity);
                break;
            case QUANTITY_DESC:
                comparator = Comparator.comparingInt(GroundItemEntry::getQuantity).reversed();
                break;
            case VALUE_DESC:
                comparator = Comparator.comparingLong(item -> getItemValue(item.getItemId(), item.getQuantity()));
                comparator = comparator.reversed();
                break;
            default:
                comparator = Comparator.comparing(GroundItemEntry::getName, String.CASE_INSENSITIVE_ORDER); // Default to name asc
                break;
        }
        items.sort(comparator);
    }

    private long getItemValue(int itemId, int quantity)
    {
        // This is we fetch the item price.
        // ItemManager.getItemPrice is a blocking call, but for sorting, it's generally fine
        // as it caches prices.
        ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        return (long) itemManager.getItemPrice(itemId) * quantity;
    }

    private JPanel createGroundItemEntryPanel(GroundItemEntry item)
    {
        JPanel itemPanel = createBaseItemPanel();

        JLabel imageLabel = createImageLabel(item);
        itemPanel.add(imageLabel, BorderLayout.WEST);

        JPanel detailsPanel = createDetailsPanel(item);
        itemPanel.add(detailsPanel, BorderLayout.CENTER);

        JButton actionButton = createActionButton(item);
        itemPanel.add(actionButton, BorderLayout.EAST);

        return itemPanel;
    }

    private JPanel createBaseItemPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setBackground(getBackground().darker());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JLabel createImageLabel(GroundItemEntry item)
    {
        JLabel label = new JLabel();
        label.setBorder(new EmptyBorder(5, 5, 5, 5));

        BufferedImage itemImage = itemManager.getImage(item.getItemId(), item.getQuantity(), false);
        if (itemImage != null)
        {
            label.setIcon(new ImageIcon(itemImage));
        }
        return label;
    }

    private JPanel createDetailsPanel(GroundItemEntry item) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(5, 0, 5, 5));
        panel.setBackground(getBackground().darker());

        panel.add(createBoldLabel(item.getName()));
        panel.add(new JLabel("Quantity: " + item.getQuantity()));

        long itemValue = getItemValue(item.getItemId(), item.getQuantity());
        panel.add(new JLabel("Value: " + String.format("%,d", itemValue) + " GP"));

        return panel;
    }

    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, (float) 12.0));
        return label;
    }

    private JButton createActionButton(GroundItemEntry item)
    {
        JButton button = new JButton("Highlight");
        button.addActionListener(e -> {
            if (itemHighlightCallback != null) // Use the new callback
            {
                itemHighlightCallback.accept(item); // Pass the GroundItemEntry
            }
        });
        return button;
    }

}