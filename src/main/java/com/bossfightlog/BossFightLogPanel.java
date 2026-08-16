package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
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
 * The side panel: session totals on top, then either one card per kill or one row per boss.
 * <p>
 * Rebuilt wholesale whenever the fight list changes. A history holds hundreds of kills rather than
 * millions, so the simplicity is worth more than incremental updates would be.
 */
class BossFightLogPanel extends PluginPanel
{
	private final SummaryPanel summary = new SummaryPanel();
	private final JPanel content = new JPanel();
	private final JLabel clearButton = new JLabel("Clear");
	private final JLabel allKillsTab = new JLabel("All kills");
	private final JLabel byBossTab = new JLabel("By boss");

	private List<BossFight> fights = new ArrayList<>();
	private boolean groupByBoss;
	private Runnable onClear = () -> {
	};

	BossFightLogPanel()
	{
		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		setFights(Collections.emptyList());
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
		SwingUtilities.invokeLater(() -> {
			this.fights = snapshot;
			rebuild();
		});
	}

	private void rebuild()
	{
		summary.update(fights);
		updateTabStyles();

		content.removeAll();

		if (groupByBoss)
		{
			for (BossGroup group : BossGroup.group(fights))
			{
				content.add(wrap(new GroupCard(group, fights)));
			}
		}
		else
		{
			for (int i = fights.size() - 1; i >= 0; i--)
			{
				BossFight fight = fights.get(i);
				content.add(wrap(new FightCard(fight, BossGroup.dpsTrend(fight, fights))));
			}
		}

		clearButton.setVisible(!fights.isEmpty());

		content.revalidate();
		content.repaint();
	}

	private static JPanel wrap(JPanel card)
	{
		JPanel spacer = new JPanel(new BorderLayout());
		spacer.setOpaque(false);
		spacer.setBorder(new EmptyBorder(0, 0, 4, 0));
		spacer.setAlignmentX(Component.LEFT_ALIGNMENT);
		spacer.add(card, BorderLayout.CENTER);
		return spacer;
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
		header.add(buildTabs(), BorderLayout.SOUTH);
		return header;
	}

	private JPanel buildTabs()
	{
		JPanel tabs = new JPanel(new BorderLayout());
		tabs.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabs.setBorder(new EmptyBorder(2, 0, 0, 0));

		configureTab(allKillsTab, false);
		configureTab(byBossTab, true);

		tabs.add(allKillsTab, BorderLayout.WEST);
		tabs.add(byBossTab, BorderLayout.EAST);
		return tabs;
	}

	private void configureTab(JLabel tab, boolean grouped)
	{
		tab.setFont(FontManager.getRunescapeSmallFont());
		tab.setCursor(new Cursor(Cursor.HAND_CURSOR));
		tab.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (groupByBoss != grouped)
				{
					groupByBoss = grouped;
					rebuild();
				}
			}
		});
	}

	private void updateTabStyles()
	{
		allKillsTab.setForeground(groupByBoss ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.BRAND_ORANGE);
		byBossTab.setForeground(groupByBoss ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR);
	}
}
