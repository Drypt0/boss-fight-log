package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Session totals across every recorded kill, shown above the individual kill cards.
 */
class SummaryPanel extends JPanel
{
	private final JPanel rows = new JPanel(new GridLayout(0, 2, 4, 2));

	SummaryPanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(6, 7, 6, 7));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		rows.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(rows, BorderLayout.CENTER);

		update(java.util.Collections.emptyList());
	}

	void update(List<BossFight> fights)
	{
		rows.removeAll();

		if (fights.isEmpty())
		{
			JLabel empty = new JLabel("No kills recorded yet");
			empty.setFont(FontManager.getRunescapeSmallFont());
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			rows.add(empty);
			rows.revalidate();
			rows.repaint();
			return;
		}

		int kills = fights.size();
		int totalDamage = 0;
		int totalTaken = 0;
		int totalHits = 0;
		int totalAttempts = 0;
		int totalTicks = 0;
		double bestDps = 0;
		double bestTime = Double.MAX_VALUE;

		for (BossFight fight : fights)
		{
			totalDamage += fight.getDamageDealt();
			totalTaken += fight.getDamageTaken();
			totalHits += fight.getHits();
			totalAttempts += fight.getHits() + fight.getMisses();
			totalTicks += fight.getDurationTicks();
			bestDps = Math.max(bestDps, fight.getDps());
			bestTime = Math.min(bestTime, fight.getDurationSeconds());
		}

		double totalSeconds = totalTicks * BossFight.SECONDS_PER_TICK;
		// Overall DPS is total damage over total time rather than the mean of each kill's DPS, so a
		// long fight counts for more than a short one.
		double overallDps = totalSeconds > 0 ? totalDamage / totalSeconds : 0;
		double accuracy = totalAttempts > 0 ? (double) totalHits / totalAttempts : 0;

		addRow("Kills", Formats.number(kills));
		addRow("Total time", Formats.duration(totalSeconds));
		addRow("Avg kill", Formats.duration(totalSeconds / kills));
		addRow("Best kill", Formats.duration(bestTime));
		addRow("Avg DPS", Formats.dps(overallDps));
		addRow("Best DPS", Formats.dps(bestDps));
		addRow("Dmg taken", Formats.number(totalTaken));
		addRow("Accuracy", Formats.percent(accuracy));

		rows.revalidate();
		rows.repaint();
	}

	private void addRow(String label, String value)
	{
		JLabel key = new JLabel(label);
		key.setFont(FontManager.getRunescapeSmallFont());
		key.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JLabel val = new JLabel(value);
		val.setFont(FontManager.getRunescapeSmallFont());
		val.setForeground(ColorScheme.BRAND_ORANGE);

		rows.add(key);
		rows.add(val);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}
