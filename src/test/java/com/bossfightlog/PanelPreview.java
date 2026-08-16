package com.bossfightlog;

import java.awt.BorderLayout;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Renders the panel in a bare window with synthetic kills, so the layout can be looked at without
 * launching the client and killing something.
 * <p>
 * The sample data deliberately stresses the layout rather than looking tidy: the longest boss name
 * in the registry, a five figure damage total, a flawless kill, a sub-minute kill and one that runs
 * past an hour. If it survives these it will survive real fights.
 * <p>
 * Test sources only. Nothing here ships in the plugin.
 */
public class PanelPreview
{
	private static final List<BossFight> SAMPLE = Arrays.asList(
		// Ordinary kill, the shape most cards will take.
		new BossFight("Scurrius", 200, 561, 22, 27, 3, 45, 120, Instant.now()),
		// Longest name in the registry against the widest kill time.
		new BossFight("Thermonuclear smoke devil", 301, 12450, 187, 214, 46, 62, 640, Instant.now()),
		// Flawless: no damage taken, perfect accuracy.
		new BossFight("Obor", 106, 240, 0, 12, 0, 31, 47, Instant.now()),
		// Fast kill, tests the m:ss floor.
		new BossFight("Bryophyta", 128, 180, 14, 9, 1, 38, 18, Instant.now()),
		// Rough kill: heavy damage taken, poor accuracy.
		new BossFight("Corporeal Beast", 785, 2640, 891, 88, 61, 51, 1180, Instant.now()),
		// Runs past an hour, tests the h:mm:ss branch.
		new BossFight("Vet'ion", 454, 31200, 2450, 640, 190, 74, 6400, Instant.now())
	);

	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() -> {
			BossFightLogPanel panel = new BossFightLogPanel();
			panel.setClearListener(() -> panel.setFights(Arrays.asList()));
			panel.setFights(SAMPLE);

			JFrame frame = new JFrame("Boss Fight Log — panel preview");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().setBackground(ColorScheme.DARK_GRAY_COLOR);
			frame.getContentPane().add(panel.getWrappedPanel(), BorderLayout.CENTER);
			frame.setSize(PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH + 16, 760);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}
