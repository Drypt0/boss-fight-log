package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One completed kill, laid out like a Loot Tracker entry: identity and headline number in the
 * header, detail underneath. The headline is the kill time rather than a loot value, since that is
 * the figure worth comparing between kills.
 * <p>
 * Colour carries meaning rather than decoration: orange for the kill time, plain text for DPS, red
 * for damage taken, and muted grey for everything supporting. That gives a reading order at a
 * glance instead of eight values competing for attention.
 */
class FightCard extends JPanel
{
	private static final Color DAMAGE_TAKEN_COLOR = ColorScheme.PROGRESS_ERROR_COLOR;

	/**
	 * @param dpsTrend signed fraction against this boss's other kills, or null when there is
	 *                 nothing to compare against
	 */
	FightCard(BossFight fight, Double dpsTrend)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(6, 8, 6, 8));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		add(buildHeader(fight), BorderLayout.NORTH);
		add(buildBody(fight, dpsTrend), BorderLayout.CENTER);

		setToolTipText(buildTooltip(fight, dpsTrend));
		installHover(this);
	}

	private static JPanel buildHeader(BossFight fight)
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);

		// The combat level is dimmed so the boss name reads first.
		JLabel name = new JLabel("<html>" + fight.getBossName()
			+ " <font color='" + Formats.hex(ColorScheme.MEDIUM_GRAY_COLOR) + "'>(lvl-"
			+ fight.getCombatLevel() + ")</font></html>");
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(ColorScheme.TEXT_COLOR);

		JLabel time = new JLabel(Formats.duration(fight.getDurationSeconds()));
		time.setFont(FontManager.getRunescapeBoldFont());
		time.setForeground(ColorScheme.BRAND_ORANGE);

		header.add(name, BorderLayout.WEST);
		header.add(time, BorderLayout.EAST);
		return header;
	}

	private static JPanel buildBody(BossFight fight, Double dpsTrend)
	{
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		body.setBorder(new EmptyBorder(3, 0, 0, 0));

		JLabel dps = new JLabel("<html>" + Formats.dps(fight.getDps())
			+ " <font color='" + Formats.hex(ColorScheme.MEDIUM_GRAY_COLOR) + "'>dps</font>"
			+ Formats.trendHtml(dpsTrend) + "</html>");
		dps.setFont(FontManager.getRunescapeBoldFont());
		dps.setForeground(ColorScheme.TEXT_COLOR);
		dps.setAlignmentX(Component.LEFT_ALIGNMENT);

		String muted = Formats.hex(ColorScheme.MEDIUM_GRAY_COLOR);
		JLabel detail = new JLabel("<html>"
			+ "<font color='" + muted + "'>" + Formats.number(fight.getDamageDealt()) + " dealt</font>"
			+ "<font color='" + muted + "'> &middot; </font>"
			+ "<font color='" + Formats.hex(DAMAGE_TAKEN_COLOR) + "'>" + Formats.number(fight.getDamageTaken()) + " taken</font>"
			+ "<font color='" + muted + "'> &middot; " + Formats.percent(fight.getAccuracy()) + " acc</font>"
			+ "</html>");
		detail.setFont(FontManager.getRunescapeSmallFont());
		detail.setAlignmentX(Component.LEFT_ALIGNMENT);

		body.add(dps);
		body.add(detail);
		return body;
	}

	private static String buildTooltip(BossFight fight, Double dpsTrend)
	{
		String trend = dpsTrend == null
			? "First recorded kill of this boss"
			: Formats.percentSigned(dpsTrend) + " DPS against your other kills of this boss";

		return "<html>"
			+ "Max hit: " + fight.getMaxHit() + "<br>"
			+ "Hits landed: " + fight.getHits() + " of " + (fight.getHits() + fight.getMisses()) + "<br>"
			+ "Fight time: " + Formats.duration(fight.getDurationSeconds()) + " (idle time excluded)<br>"
			+ trend
			+ "</html>";
	}

	/**
	 * Highlights the whole card on hover. The listener goes on every descendant as well, otherwise
	 * moving the pointer onto a child label reads as leaving the card.
	 */
	private static void installHover(FightCard card)
	{
		MouseAdapter hover = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				card.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		};

		addRecursively(card, hover);
	}

	private static void addRecursively(Container container, MouseAdapter listener)
	{
		container.addMouseListener(listener);
		for (Component child : container.getComponents())
		{
			if (child instanceof Container)
			{
				addRecursively((Container) child, listener);
			}
			else
			{
				child.addMouseListener(listener);
			}
		}
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}
