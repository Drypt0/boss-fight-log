package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Totals across every recorded kill, as a grid of stat tiles.
 * <p>
 * Each tile is a figure over a caption rather than a label beside a value, so the numbers line up
 * as a block and the eye lands on them first. The RuneScape fonts are bitmaps and cannot be scaled,
 * so the headline figures use the default font, which is a real outline font.
 */
class SummaryPanel extends JPanel
{
	private static final Font VALUE_FONT = Fonts.bold(18f);
	private static final Font CAPTION_FONT = Fonts.regular(10f);

	private final JLabel heading = new JLabel("ALL TIME");
	private final JLabel headingValue = new JLabel();
	private final JPanel grid = new JPanel(new GridLayout(0, 2, 1, 1));

	SummaryPanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		heading.setFont(CAPTION_FONT);
		heading.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		headingValue.setFont(CAPTION_FONT);
		headingValue.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		headingValue.setHorizontalAlignment(SwingConstants.RIGHT);

		JPanel headingRow = new JPanel(new BorderLayout());
		headingRow.setOpaque(false);
		headingRow.setBorder(new EmptyBorder(0, 2, 6, 2));
		headingRow.add(heading, BorderLayout.WEST);
		headingRow.add(headingValue, BorderLayout.EAST);

		// The gap colour shows through as hairline rules between tiles.
		grid.setBackground(ColorScheme.BORDER_COLOR);

		add(headingRow, BorderLayout.NORTH);
		add(grid, BorderLayout.CENTER);

		update(Collections.emptyList());
	}

	void update(List<BossFight> fights)
	{
		grid.removeAll();

		if (fights.isEmpty())
		{
			headingValue.setText("");
			grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			JLabel empty = new JLabel("No kills recorded yet", SwingConstants.CENTER);
			empty.setFont(CAPTION_FONT);
			empty.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			grid.add(empty);
			revalidateAll();
			return;
		}

		grid.setBackground(ColorScheme.BORDER_COLOR);

		int kills = fights.size();
		int timedKills = 0;
		int totalDamage = 0;
		int totalTaken = 0;
		int totalHits = 0;
		int totalAttempts = 0;
		int totalTicks = 0;
		double bestDps = 0;
		double bestTime = Double.MAX_VALUE;

		for (BossFight fight : fights)
		{
			totalTaken += fight.getDamageTaken();
			totalHits += fight.getHits();
			totalAttempts += fight.getHits() + fight.getMisses();
			totalDamage += fight.getDamageDealt();

			// Kills landed inside a single tick have damage but no duration. They are counted as
			// kills, but contribute to no figure that divides one by the other.
			if (!fight.isDpsMeasurable())
			{
				continue;
			}

			timedKills++;
			totalTicks += fight.getDurationTicks();
			bestDps = Math.max(bestDps, fight.getDps());
			bestTime = Math.min(bestTime, fight.getDurationSeconds());
		}

		double timedSeconds = totalTicks * BossFight.SECONDS_PER_TICK;
		double timedDamage = 0;
		for (BossFight fight : fights)
		{
			if (fight.isDpsMeasurable())
			{
				timedDamage += fight.getDamageDealt();
			}
		}

		// Overall DPS is total damage over total time rather than the mean of each kill's DPS, so a
		// long fight counts for more than a short one.
		double overallDps = timedSeconds > 0 ? timedDamage / timedSeconds : 0;
		double accuracy = totalAttempts > 0 ? (double) totalHits / totalAttempts : 0;

		headingValue.setText(Formats.duration(timedSeconds) + " total");

		addTile(Formats.dps(overallDps), "AVG DPS");
		addTile(timedKills > 0 ? Formats.duration(timedSeconds / timedKills) : "—", "AVG KILL");
		addTile(timedKills > 0 ? Formats.dps(bestDps) : "—", "BEST DPS");
		addTile(bestTime == Double.MAX_VALUE ? "—" : Formats.duration(bestTime), "BEST KILL");
		addTile(Formats.number(totalDamage), "DMG DEALT");
		addTile(Formats.number(totalTaken), "DMG TAKEN");
		addTile(Formats.percent(accuracy), "ACCURACY");
		addTile(Formats.number(kills), "KILLS");

		revalidateAll();
	}

	private void addTile(String value, String caption)
	{
		JPanel tile = new JPanel();
		tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
		tile.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		tile.setBorder(new EmptyBorder(8, 9, 8, 6));

		JLabel valueLabel = new JLabel(value);
		valueLabel.setFont(VALUE_FONT);
		valueLabel.setForeground(ColorScheme.TEXT_COLOR);
		valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel captionLabel = new JLabel(caption);
		captionLabel.setFont(CAPTION_FONT);
		captionLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		tile.add(valueLabel);
		tile.add(captionLabel);
		grid.add(tile);
	}

	private void revalidateAll()
	{
		grid.revalidate();
		grid.repaint();
		revalidate();
		repaint();
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}
