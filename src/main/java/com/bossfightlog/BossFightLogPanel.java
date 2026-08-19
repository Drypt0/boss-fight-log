package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
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
	private final JLabel allKillsTab = new JLabel("ALL KILLS");
	private final JLabel byBossTab = new JLabel("BY BOSS");
	private final JLabel status = new JLabel();

	private List<BossFight> fights = new ArrayList<>();
	private boolean groupByBoss;

	BossFightLogPanel()
	{
		setLayout(new BorderLayout(0, 4));
		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);

		setFights(Collections.emptyList());
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
		status.setText(fights.isEmpty()
			? ""
			: fights.size() + " · " + Formats.duration(SummaryPanel.totalSeconds(fights)));
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

		content.revalidate();
		content.repaint();
	}

	private static JPanel wrap(JPanel card)
	{
		JPanel spacer = new JPanel(new BorderLayout());
		spacer.setOpaque(false);
		spacer.setBorder(new EmptyBorder(0, 0, 3, 0));
		spacer.setAlignmentX(Component.LEFT_ALIGNMENT);
		spacer.add(card, BorderLayout.CENTER);
		spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, spacer.getPreferredSize().height));
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
		title.setFont(Fonts.bold(14f));
		title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		status.setFont(Fonts.regular(10f));
		status.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		status.setHorizontalAlignment(SwingConstants.RIGHT);

		titleRow.add(title, BorderLayout.WEST);
		titleRow.add(status, BorderLayout.EAST);

		header.add(titleRow, BorderLayout.NORTH);
		header.add(summary, BorderLayout.CENTER);
		header.add(buildTabs(), BorderLayout.SOUTH);
		return header;
	}

	private JPanel buildTabs()
	{
		JPanel tabs = new JPanel(new GridLayout(1, 2, 1, 0));
		tabs.setBackground(ColorScheme.MEDIUM_GRAY_COLOR.darker());
		tabs.setBorder(new MatteBorder(1, 1, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR.darker()));

		configureTab(allKillsTab, false);
		configureTab(byBossTab, true);

		tabs.add(allKillsTab);
		tabs.add(byBossTab);
		return tabs;
	}

	private void configureTab(JLabel tab, boolean grouped)
	{
		tab.setFont(Fonts.bold(11f));
		tab.setHorizontalAlignment(SwingConstants.CENTER);
		tab.setOpaque(true);
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

	/**
	 * The selected tab is filled and lit; the other sits flush with the panel behind it, so which
	 * view is showing is legible without reading either label.
	 */
	private void updateTabStyles()
	{
		styleTab(allKillsTab, !groupByBoss);
		styleTab(byBossTab, groupByBoss);
	}

	private static void styleTab(JLabel tab, boolean selected)
	{
		tab.setBackground(selected ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		tab.setForeground(selected ? ColorScheme.TEXT_COLOR : ColorScheme.MEDIUM_GRAY_COLOR);
		// The fill and the text colour carry the selection on their own; an underline as well was
		// one marker too many for a two item strip.
		tab.setBorder(new EmptyBorder(6, 0, 6, 0));
	}
}
