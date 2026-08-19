package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * One completed kill.
 * <p>
 * A stripe down the left edge carries how the kill compared with the others, so a long list can be
 * skimmed for the good and bad ones without reading a single number. Colour is used for meaning
 * only: orange for the kill time and green or red for the comparison. The supporting figures
 * stay uniformly grey, so nothing competes with the two that carry the result.
 */
class FightCard extends JPanel
{
	private static final Font DPS_FONT = Fonts.monoBold(16f);
	private static final Font NAME_FONT = Fonts.regular(12f);
	private static final Font TIME_FONT = Fonts.monoBold(14f);
	/**
	 * The supporting line is the widest thing on a card and monospaced text is wider than
	 * proportional, so it takes a size down to survive five figure damage totals.
	 */
	private static final Font CAPTION_FONT = Fonts.mono(10f);
	private static final int STRIPE_WIDTH = 3;

	/**
	 * Usable width inside the panel once its scrollbar has taken its share.
	 */
	private static final int MAX_WIDTH = PluginPanel.PANEL_WIDTH - PluginPanel.SCROLLBAR_WIDTH;

	/**
	 * @param dpsTrend signed fraction against this boss's other kills, or null when there is
	 *                 nothing to compare against
	 */
	FightCard(BossFight fight, Double dpsTrend)
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(stripeBorder(stripeColor(dpsTrend)));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		add(buildHeader(fight), BorderLayout.NORTH);
		add(buildBody(fight, dpsTrend), BorderLayout.CENTER);

		setToolTipText(buildTooltip(fight, dpsTrend));
		installHover(this, stripeColor(dpsTrend));
	}

	private static CompoundBorder stripeBorder(Color stripe)
	{
		return new CompoundBorder(
			new MatteBorder(0, STRIPE_WIDTH, 0, 0, stripe),
			new EmptyBorder(5, 8, 5, 8));
	}

	/**
	 * Grey covers both a first kill and one that landed on the average: neither is a result worth
	 * colouring.
	 */
	private static Color stripeColor(Double dpsTrend)
	{
		if (dpsTrend == null || Math.abs(dpsTrend) < 0.01)
		{
			return ColorScheme.MEDIUM_GRAY_COLOR;
		}
		return dpsTrend > 0 ? Formats.TREND_UP : Formats.TREND_DOWN;
	}

	private static JPanel buildHeader(BossFight fight)
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);

		// Centre slot rather than west: west takes its full preferred width and prints over the
		// time on a long boss name, whereas centre takes what is left and truncates.
		JLabel name = new JLabel(fight.getBossName() + " (lvl-" + fight.getCombatLevel() + ")");
		name.setFont(NAME_FONT);
		name.setForeground(ColorScheme.TEXT_COLOR);
		name.setMinimumSize(new Dimension(0, name.getPreferredSize().height));

		JLabel time = new JLabel(Formats.duration(fight.getDurationSeconds()));
		time.setFont(TIME_FONT);
		time.setForeground(ColorScheme.BRAND_ORANGE);
		time.setBorder(new EmptyBorder(0, 6, 0, 0));

		header.add(name, BorderLayout.CENTER);
		header.add(time, BorderLayout.EAST);
		return header;
	}

	private static JPanel buildBody(BossFight fight, Double dpsTrend)
	{
		JPanel body = new JPanel(new BorderLayout());
		body.setOpaque(false);
		body.setBorder(new EmptyBorder(1, 0, 0, 0));

		JPanel rateRow = new JPanel(new BorderLayout());
		rateRow.setOpaque(false);

		String muted = Formats.hex(ColorScheme.MEDIUM_GRAY_COLOR);

		// A kill landed inside a single tick has no duration to divide by, so there is no rate to
		// show. Printing one would be inventing it.
		JLabel rate = new JLabel(fight.isDpsMeasurable()
			? "<html>" + Formats.dps(fight.getDps())
				+ " <font size='2' color='" + muted + "'>DPS</font></html>"
			: "<html><font color='" + muted + "'>instant kill</font></html>");
		rate.setFont(DPS_FONT);
		rate.setForeground(ColorScheme.TEXT_COLOR);

		JLabel trend = new JLabel(Formats.trendText(dpsTrend), SwingConstants.RIGHT);
		trend.setFont(CAPTION_FONT);
		trend.setForeground(stripeColor(dpsTrend));

		rateRow.add(rate, BorderLayout.CENTER);
		rateRow.add(trend, BorderLayout.EAST);

		JLabel detail = new JLabel("<html>"
			+ "<font color='" + muted + "'>" + Formats.number(fight.getDamageDealt()) + " dealt &middot; </font>"
			+ "<font color='" + muted + "'>" + Formats.number(fight.getDamageTaken()) + " taken</font>"
			+ "<font color='" + muted + "'> &middot; " + Formats.percent(fight.getAccuracy()) + "</font>"
			+ "</html>");
		detail.setFont(CAPTION_FONT);
		detail.setBorder(new EmptyBorder(1, 0, 0, 0));

		body.add(rateRow, BorderLayout.NORTH);
		body.add(detail, BorderLayout.CENTER);
		return body;
	}

	private static String buildTooltip(BossFight fight, Double dpsTrend)
	{
		String trend = dpsTrend == null
			? "First recorded kill of this boss"
			: Formats.percentSignedPrecise(dpsTrend) + " DPS against your other kills of this boss";

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
	private static void installHover(FightCard card, Color stripe)
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

	/**
	 * The column this sits in is only ever as wide as the panel, so a long boss name must not be
	 * allowed to report a preferred width past it. Without this the widest card widens the whole
	 * column and every card loses its right hand edge.
	 */
	@Override
	public Dimension getPreferredSize()
	{
		Dimension size = super.getPreferredSize();
		return new Dimension(Math.min(size.width, MAX_WIDTH), size.height);
	}

	@Override
	public Dimension getMinimumSize()
	{
		Dimension size = super.getMinimumSize();
		return new Dimension(0, size.height);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
	}
}
