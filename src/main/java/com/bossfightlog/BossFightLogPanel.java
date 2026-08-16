package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The side panel: session totals on top, then one card per kill with the most recent first.
 * <p>
 * Rebuilt wholesale whenever the fight list changes. A session holds tens of kills rather than
 * thousands, so the simplicity is worth more than incremental updates would be.
 */
class BossFightLogPanel extends PluginPanel
{
	private final SummaryPanel summary = new SummaryPanel();
	private final JPanel fightList = new JPanel();
	private final JLabel clearButton = new JLabel("Clear");

	private Runnable onClear = () -> {
	};

	BossFightLogPanel()
	{
		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		fightList.setLayout(new BoxLayout(fightList, BoxLayout.Y_AXIS));
		fightList.setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);
		add(fightList, BorderLayout.CENTER);

		setFights(new ArrayList<>());
	}

	void setClearListener(Runnable onClear)
	{
		this.onClear = onClear;
	}

	/**
	 * Replaces the displayed fights. Safe to call from the client thread.
	 */
	void setFights(List<BossFight> fights)
	{
		List<BossFight> snapshot = new ArrayList<>(fights);
		SwingUtilities.invokeLater(() -> rebuild(snapshot));
	}

	private void rebuild(List<BossFight> fights)
	{
		summary.update(fights);

		fightList.removeAll();
		for (int i = fights.size() - 1; i >= 0; i--)
		{
			FightCard card = new FightCard(fights.get(i));
			card.setBorder(new EmptyBorder(0, 0, 4, 0));
			fightList.add(card);
		}

		clearButton.setVisible(!fights.isEmpty());

		fightList.revalidate();
		fightList.repaint();
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout(0, 6));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel titleRow = new JPanel(new BorderLayout());
		titleRow.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Boss Fight Log");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		clearButton.setFont(FontManager.getRunescapeSmallFont());
		clearButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
		clearButton.setVisible(false);
		clearButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				onClear.run();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				clearButton.setForeground(ColorScheme.BRAND_ORANGE);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				clearButton.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
		});

		titleRow.add(title, BorderLayout.WEST);
		titleRow.add(clearButton, BorderLayout.EAST);

		header.add(titleRow, BorderLayout.NORTH);
		header.add(summary, BorderLayout.CENTER);
		return header;
	}
}
