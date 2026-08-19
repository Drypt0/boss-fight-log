package com.bossfightlog;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
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
		// Repeated kills of one boss, so grouping has something to collapse and the trend figures
		// have a baseline. DPS is varied deliberately: 561/120 ticks is the middle of the spread,
		// with a clearly better and a clearly worse kill either side of it.
		new BossFight("Scurrius", 200, 561, 22, 27, 3, 45, 120, Instant.now()),
		new BossFight("Scurrius", 200, 548, 41, 26, 6, 42, 148, Instant.now()),
		new BossFight("Scurrius", 200, 570, 8, 28, 1, 45, 96, Instant.now()),
		new BossFight("Scurrius", 200, 559, 30, 27, 4, 39, 133, Instant.now()),
		// Lands within a fraction of a percent of the other four: reads as on par, not as a
		// blank, which is what a first kill looks like.
		new BossFight("Scurrius", 200, 540, 25, 26, 4, 41, 120, Instant.now()),
		// Longest name in the registry against the widest kill time.
		new BossFight("Thermonuclear smoke devil", 301, 12450, 187, 214, 46, 62, 640, Instant.now()),
		// Flawless: no damage taken, perfect accuracy.
		new BossFight("Obor", 106, 240, 0, 12, 0, 31, 47, Instant.now()),
		// Killed inside a single tick: damage but no measurable rate.
		new BossFight("Obor", 106, 240, 0, 2, 0, 180, 0, Instant.now()),
		// Fast kill, tests the m:ss floor.
		new BossFight("Bryophyta", 128, 180, 14, 9, 1, 38, 18, Instant.now()),
		// Rough kill: heavy damage taken, poor accuracy.
		new BossFight("Corporeal Beast", 785, 2640, 891, 88, 61, 51, 1180, Instant.now()),
		new BossFight("Corporeal Beast", 785, 2710, 640, 91, 48, 57, 1020, Instant.now()),
		// Runs past an hour, tests the h:mm:ss branch.
		new BossFight("Vet'ion", 454, 31200, 2450, 640, 190, 74, 6400, Instant.now())
	);

	public static void main(String[] args) throws Exception
	{
		if (args.length > 0)
		{
			renderToFile(args[0]);
			return;
		}

		SwingUtilities.invokeLater(() -> {
			BossFightLogPanel panel = new BossFightLogPanel();
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
	/**
	 * Paints the panel into a PNG without showing a window, so the layout can be inspected as a
	 * file rather than only with a pair of eyes in front of the client.
	 */
	/** Prints any component demanding more width than the panel allows. */
	private static void dumpWide(java.awt.Component c, int depth)
	{
		int limit = PluginPanel.PANEL_WIDTH;
		if (c.getPreferredSize().width > limit)
		{
			StringBuilder pad = new StringBuilder();
			for (int i = 0; i < depth; i++)
			{
				pad.append("  ");
			}
			String text = c instanceof javax.swing.JLabel ? " \"" + ((javax.swing.JLabel) c).getText() + "\"" : "";
			System.out.println(pad + c.getClass().getSimpleName() + " pref=" + c.getPreferredSize().width + text);
		}
		if (c instanceof java.awt.Container)
		{
			for (java.awt.Component child : ((java.awt.Container) c).getComponents())
			{
				dumpWide(child, depth + 1);
			}
		}
	}

	/** Forces layout through the whole tree, which setSize alone does not do for children. */
	private static void layOutTree(java.awt.Component c)
	{
		c.doLayout();
		if (c instanceof java.awt.Container)
		{
			for (java.awt.Component child : ((java.awt.Container) c).getComponents())
			{
				layOutTree(child);
			}
		}
	}

	private static void renderToFile(String path) throws Exception
	{
		final BossFightLogPanel[] holder = new BossFightLogPanel[1];
		SwingUtilities.invokeAndWait(() -> {
			holder[0] = new BossFightLogPanel();
			holder[0].setFights(SAMPLE);
			if (System.getProperty("bfl.grouped") != null)
			{
				holder[0].setGroupByBoss(true);
			}
		});

		// setFights marshals onto the EDT, so let that queued rebuild run before measuring.
		SwingUtilities.invokeAndWait(() -> {
		});

		BossFightLogPanel panel = holder[0];
		SwingUtilities.invokeAndWait(() -> {
			JFrame frame = new JFrame();
			JComponent wrapped = panel.getWrappedPanel();
			frame.getContentPane().add(wrapped, BorderLayout.CENTER);
			frame.pack();

			// Size the wrapped panel itself, not the frame: a JFrame adds window borders, so
			// sizing the frame leaves the scroll pane narrower than asked and its viewport
			// narrower still, which clips the panel and looks like a layout bug that is not there.
			int width = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH;
			int height = Math.max(panel.getPreferredSize().height + 4, 400);
			wrapped.setSize(width, height);
			wrapped.doLayout();
			layOutTree(wrapped);

			System.out.println("wrapped = " + wrapped.getSize() + ", panel = " + panel.getSize());

			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setColor(ColorScheme.DARK_GRAY_COLOR);
			g.fillRect(0, 0, width, height);
			wrapped.printAll(g);
			g.dispose();

			try
			{
				ImageIO.write(image, "png", new File(path));
				System.out.println("wrote " + path + " (" + width + "x" + height + ")");
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
			frame.dispose();

		});

		System.exit(0);
	}

}
