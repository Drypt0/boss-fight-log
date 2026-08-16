package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One completed kill, laid out like a Loot Tracker entry: identity and headline number in the
 * header, detail underneath. The headline number here is the kill time rather than a loot value,
 * since that is the figure worth comparing between kills.
 */
class FightCard extends JPanel
{
	FightCard(BossFight fight)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(5, 7, 5, 7));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		add(buildHeader(fight), BorderLayout.NORTH);
		add(buildStats(fight), BorderLayout.CENTER);

		setToolTipText(buildTooltip(fight));
	}

	private static JPanel buildHeader(BossFight fight)
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel name = new JLabel(fight.getBossName() + " (lvl-" + fight.getCombatLevel() + ")");
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JLabel time = new JLabel(Formats.duration(fight.getDurationSeconds()));
		time.setFont(FontManager.getRunescapeSmallFont());
		time.setForeground(ColorScheme.BRAND_ORANGE);

		header.add(name, BorderLayout.WEST);
		header.add(time, BorderLayout.EAST);
		return header;
	}

	private static JPanel buildStats(BossFight fight)
	{
		JPanel stats = new JPanel(new GridLayout(2, 2, 4, 1));
		stats.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stats.setBorder(new EmptyBorder(4, 0, 0, 0));

		stats.add(stat(Formats.dps(fight.getDps()) + " dps"));
		stats.add(stat(Formats.number(fight.getDamageDealt()) + " dealt"));
		stats.add(stat(Formats.number(fight.getDamageTaken()) + " taken"));
		stats.add(stat(Formats.percent(fight.getAccuracy()) + " acc"));

		return stats;
	}

	private static JLabel stat(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
		return label;
	}

	private static String buildTooltip(BossFight fight)
	{
		return "<html>"
			+ "Max hit: " + fight.getMaxHit() + "<br>"
			+ "Hits landed: " + fight.getHits() + " of " + (fight.getHits() + fight.getMisses()) + "<br>"
			+ "Fight time: " + Formats.duration(fight.getDurationSeconds()) + " (idle time excluded)"
			+ "</html>";
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}
