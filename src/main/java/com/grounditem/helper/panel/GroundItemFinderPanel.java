package com.grounditem.helper.panel;

import com.grounditem.helper.domain.GroundItemEntry;
import com.grounditem.helper.domain.enums.GroundItemSortType;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.game.ItemManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.Objects;

public class GroundItemFinderPanel extends PluginPanel {

    private static final Color LOW_VALUE_COLOR = new Color(70, 70, 70);
    private static final Color MEDIUM_VALUE_COLOR = new Color(80, 80, 50); // Hint of yellow/gold
    private static final Color HIGH_VALUE_COLOR = new Color(50, 80, 50); // Hint of green
    private static final Color VERY_HIGH_VALUE_COLOR = new Color(90, 70, 30); // More prominent gold/orange
    private static final Color EXTREME_VALUE_COLOR = new Color(70, 50, 70); // Hint of purple (expensive)

    // Define value thresholds
    private static final long THRESHOLD_MEDIUM = 50_000;  // 50k GP
    private static final long THRESHOLD_HIGH = 250_000; // 250k GP
    private static final long THRESHOLD_VERY_HIGH = 1_000_000; // 1M GP
    private static final long THRESHOLD_EXTREME = 10_000_000; // 10M GP

    private final JPanel listPanel;
    private final ItemManager itemManager;
    private final Consumer<GroundItemEntry> itemHighlightCallback;
    private final Client client;

    private List<GroundItemEntry> currentItems;
    private GroundItemSortType currentSortType = GroundItemSortType.VALUE_DESC;

    public GroundItemFinderPanel(Client client, ItemManager itemManager, Consumer<GroundItemEntry> itemHighlightCallback) {
        super();
        this.client = client;
        this.itemManager = itemManager;
        this.itemHighlightCallback = itemHighlightCallback;
        setLayout(new BorderLayout());

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Initialize UI components
        add(createControlPanel(), BorderLayout.NORTH);
        add(createScrollableListPanel(), BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.setBorder(new EmptyBorder(5, 5, 0, 5));

        panel.add(new JLabel("Sort by:"));
        JComboBox<GroundItemSortType> sortComboBox = new JComboBox<>(GroundItemSortType.values());
        sortComboBox.setSelectedItem(currentSortType);
        sortComboBox.addActionListener(e -> {
            GroundItemSortType selectedSortType = (GroundItemSortType) sortComboBox.getSelectedItem();
            if (Objects.nonNull(selectedSortType) && selectedSortType != currentSortType) {
                currentSortType = selectedSortType;
                if (Objects.nonNull(currentItems)) {
                    updateItems(currentItems);
                }
            }
        });
        panel.add(sortComboBox);
        return panel;
    }

    private JScrollPane createScrollableListPanel()
    {
        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    public void updateItems(List<GroundItemEntry> items) {
        this.currentItems = new java.util.ArrayList<>(items);

        listPanel.removeAll();

        if (items.isEmpty()) {
            listPanel.add(new JLabel("No nearby ground items."));
        }
        else {
            sortItems(currentItems);

            for (GroundItemEntry item : currentItems) {
                listPanel.add(createGroundItemEntryPanel(item));
                listPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            listPanel.add(Box.createVerticalGlue());
        }

        revalidate();
        repaint();
    }

    private void sortItems(List<GroundItemEntry> items) {
        Comparator<GroundItemEntry> comparator;

        switch (currentSortType) {
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
                comparator = Comparator.comparingLong(GroundItemEntry::getValue).reversed();
                break;
            default:
                comparator = Comparator.comparing(GroundItemEntry::getName, String.CASE_INSENSITIVE_ORDER); // Default to name asc
                break;
        }
        items.sort(comparator);
    }

    private Color getItemBackgroundColor(long value)
    {
        if (value >= THRESHOLD_EXTREME)
        {
            return EXTREME_VALUE_COLOR;
        }
        else if (value >= THRESHOLD_VERY_HIGH)
        {
            return VERY_HIGH_VALUE_COLOR;
        }
        else if (value >= THRESHOLD_HIGH)
        {
            return HIGH_VALUE_COLOR;
        }
        else if (value >= THRESHOLD_MEDIUM)
        {
            return MEDIUM_VALUE_COLOR;
        }
        else
        {
            return LOW_VALUE_COLOR; // Default for low value items
        }
    }

    private JPanel createGroundItemEntryPanel(GroundItemEntry item) {
        JPanel itemPanel = createBaseItemPanel();

        itemPanel.setBackground(getItemBackgroundColor(item.getValue()));

        itemPanel.add(createImageLabel(item), BorderLayout.WEST);
        itemPanel.add(createDetailsPanel(item), BorderLayout.CENTER);
        itemPanel.add(createActionButton(item), BorderLayout.EAST);

        return itemPanel;
    }

    private JPanel createBaseItemPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY.darker()));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JLabel createImageLabel(GroundItemEntry item) {
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
        panel.setBackground(getItemBackgroundColor(item.getValue()));

        panel.add(createBoldLabel(item.getName()));
        panel.add(new JLabel("Quantity: " + item.getQuantity()));

        panel.add(new JLabel("Value: " + formatGpValue(item.getValue())));
        return panel;
    }

    private String formatGpValue(long value) {
        if (value >= 1_000_000) {
            return String.format("%,.1fM GP", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%,.1fK GP", value / 1_000.0);
        }
        return String.format("%,d GP", value);
    }

    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12.0f));
        return label;
    }

    private JButton createActionButton(GroundItemEntry item) {
        JButton button = new JButton("Highlight");
        button.addActionListener(e -> {
            if (itemHighlightCallback != null) {
                itemHighlightCallback.accept(item);
            }
        });
        return button;
    }
}
