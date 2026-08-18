package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Session totals across every recorded kill.
 * <p>
 * Kills and average DPS are promoted to a hero row because they are what the panel is glanced at
 * for; everything else is supporting detail in a quieter list below a divider. A flat list of
 * equally weighted rows makes the reader search for the number they came for.
 */
class SummaryPanel extends JPanel
{
	private final JPanel hero = new JPanel(new GridLayout(1, 2, 4, 0));
	private final JPanel details = new JPanel();

	SummaryPanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		hero.setOpaque(false);
		hero.setBorder(new EmptyBorder(0, 0, 8, 0));

		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		details.setOpaque(false);

		add(hero, BorderLayout.NORTH);
		add(details, BorderLayout.CENTER);

		update(Collections.emptyList());
	}

	void update(List<BossFight> fights)
	{
		hero.removeAll();
		details.removeAll();

		if (fights.isEmpty())
		{
			JLabel empty = new JLabel("No kills recorded yet", SwingConstants.CENTER);
			empty.setFont(FontManager.getRunescapeSmallFont());
			empty.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
			hero.add(empty);
			revalidateAll();
			return;
		}

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

			// Kills landed inside a single tick have damage but no duration. They are counted
			// as kills, but contribute to no figure that divides one by the other.
			if (!fight.isDpsMeasurable())
			{
				continue;
			}

			timedKills++;
			totalDamage += fight.getDamageDealt();
			totalTicks += fight.getDurationTicks();
			bestDps = Math.max(bestDps, fight.getDps());
			bestTime = Math.min(bestTime, fight.getDurationSeconds());
		}

		double totalSeconds = totalTicks * BossFight.SECONDS_PER_TICK;
		// Overall DPS is total damage over total time rather than the mean of each kill's DPS, so a
		// long fight counts for more than a short one.
		double overallDps = totalSeconds > 0 ? totalDamage / totalSeconds : 0;
		double accuracy = totalAttempts > 0 ? (double) totalHits / totalAttempts : 0;

		hero.add(heroStat(Formats.number(kills), kills == 1 ? "KILL" : "KILLS"));
		hero.add(heroStat(Formats.dps(overallDps), "AVG DPS"));

		details.setBorder(new CompoundBorder(
			new MatteBorder(1, 0, 0, 0, ColorScheme.BORDER_COLOR),
			new EmptyBorder(4, 0, 0, 0)));
		addDetail("Avg kill", timedKills > 0 ? Formats.duration(totalSeconds / timedKills) : "—");
		addDetail("Best kill", bestTime == Double.MAX_VALUE ? "—" : Formats.duration(bestTime));
		addDetail("Best DPS", timedKills > 0 ? Formats.dps(bestDps) : "—");
		addDetail("Total time", Formats.duration(totalSeconds));
		addDetail("Dmg taken", Formats.number(totalTaken));
		addDetail("Accuracy", Formats.percent(accuracy));

		revalidateAll();
	}

	private static JPanel heroStat(String value, String caption)
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setOpaque(false);

		JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
		valueLabel.setFont(FontManager.getRunescapeBoldFont());
		valueLabel.setForeground(ColorScheme.BRAND_ORANGE);
		valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel captionLabel = new JLabel(caption, SwingConstants.CENTER);
		captionLabel.setFont(FontManager.getRunescapeSmallFont());
		captionLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		captionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		block.add(valueLabel);
		block.add(captionLabel);
		return block;
	}

	private void addDetail(String label, String value)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(3, 0, 0, 0));

		JLabel key = new JLabel(label);
		key.setFont(FontManager.getRunescapeSmallFont());
		key.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);

		// Values are right-aligned so the column reads as a column rather than a ragged edge.
		JLabel val = new JLabel(value, SwingConstants.RIGHT);
		val.setFont(FontManager.getRunescapeSmallFont());
		val.setForeground(ColorScheme.TEXT_COLOR);

		row.add(key, BorderLayout.WEST);
		row.add(val, BorderLayout.EAST);

		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
		details.add(row);
	}

	private void revalidateAll()
	{
		hero.revalidate();
		hero.repaint();
		details.revalidate();
		details.repaint();
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}
