package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * One boss's aggregate row in the grouped view, which expands on click to reveal the individual
 * kills behind it.
 */
class GroupCard extends JPanel
{
	private static final java.awt.Font NAME_FONT = Fonts.regular(11f);
	private static final java.awt.Font COUNT_FONT = Fonts.bold(13f);
	private static final java.awt.Font DPS_FONT = Fonts.bold(16f);
	private static final java.awt.Font CAPTION_FONT = Fonts.regular(10f);
	private static final int MAX_WIDTH = PluginPanel.PANEL_WIDTH - PluginPanel.SCROLLBAR_WIDTH;

	private final BossGroup group;
	private final List<BossFight> allFights;
	private final JPanel summaryRow = new JPanel(new BorderLayout());
	private final JPanel expansion = new JPanel();
	private final JLabel killCount = new JLabel();

	private boolean expanded;
	private boolean built;

	GroupCard(BossGroup group, List<BossFight> allFights)
	{
		this.group = group;
		this.allFights = allFights;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(6, 8, 6, 8));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setCursor(new Cursor(Cursor.HAND_CURSOR));

		expansion.setLayout(new BoxLayout(expansion, BoxLayout.Y_AXIS));
		expansion.setOpaque(false);
		expansion.setBorder(new EmptyBorder(6, 0, 0, 0));
		expansion.setVisible(false);

		buildSummary();
		add(summaryRow, BorderLayout.NORTH);
		add(expansion, BorderLayout.CENTER);

		setToolTipText("Click to " + "show each kill");
		installClickAndHover();
	}

	private void buildSummary()
	{
		summaryRow.setOpaque(false);

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);

		// Centre slot so a long boss name truncates rather than printing over the kill count.
		JLabel name = new JLabel(group.getBossName() + " (lvl-" + group.getCombatLevel() + ")");
		name.setFont(NAME_FONT);
		name.setForeground(ColorScheme.TEXT_COLOR);
		name.setMinimumSize(new Dimension(0, name.getPreferredSize().height));

		killCount.setText(group.getKills() + (group.getKills() == 1 ? " kill" : " kills"));
		killCount.setFont(COUNT_FONT);
		killCount.setForeground(ColorScheme.BRAND_ORANGE);
		killCount.setBorder(new EmptyBorder(0, 6, 0, 0));

		header.add(name, BorderLayout.CENTER);
		header.add(killCount, BorderLayout.EAST);

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		body.setBorder(new EmptyBorder(3, 0, 0, 0));

		String muted = Formats.hex(ColorScheme.MEDIUM_GRAY_COLOR);

		JLabel dps = new JLabel("<html>" + Formats.dps(group.getAverageDps())
			+ " <font color='" + muted + "'>avg dps</font></html>");
		dps.setFont(DPS_FONT);
		dps.setForeground(ColorScheme.TEXT_COLOR);
		dps.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel detail = new JLabel("<html><font color='" + muted + "'>"
			+ "avg " + Formats.duration(group.getAverageSeconds())
			+ " &middot; best " + Formats.duration(group.getBestSeconds())
			+ " &middot; " + Formats.percent(group.getAccuracy()) + " acc"
			+ "</font></html>");
		detail.setFont(CAPTION_FONT);
		detail.setAlignmentX(Component.LEFT_ALIGNMENT);

		body.add(dps);
		body.add(detail);

		summaryRow.add(header, BorderLayout.NORTH);
		summaryRow.add(body, BorderLayout.CENTER);
	}

	private void toggle()
	{
		expanded = !expanded;

		// Individual cards are built the first time they are needed, so a long history does not pay
		// for rows nobody opens.
		if (expanded && !built)
		{
			for (BossFight fight : group.getFightsNewestFirst())
			{
				FightCard card = new FightCard(fight, BossGroup.dpsTrend(fight, allFights));
				card.setBackground(ColorScheme.DARK_GRAY_COLOR);
				card.setBorder(new EmptyBorder(5, 6, 5, 6));
				JPanel spacer = new JPanel(new BorderLayout());
				spacer.setOpaque(false);
				spacer.setBorder(new EmptyBorder(0, 0, 3, 0));
				spacer.add(card, BorderLayout.CENTER);
				spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, spacer.getPreferredSize().height));
				expansion.add(spacer);
			}
			built = true;
		}

		expansion.setVisible(expanded);
		setToolTipText("Click to " + (expanded ? "hide" : "show") + " each kill");

		revalidate();
		repaint();
	}

	private void installClickAndHover()
	{
		MouseAdapter listener = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				toggle();
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		};

		// Only the summary row toggles: clicks on an expanded kill card should not collapse it.
		addMouseListener(listener);
		addRecursively(summaryRow, listener);
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
