package com.bossfightlog;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.ui.FontManager;

/**
 * Picks the clearest interface font available on the machine.
 * <p>
 * RuneLite's default font is Java's logical "Dialog" family, which the runtime maps to whatever it
 * likes: usually Microsoft Sans Serif or Arial, neither of which was drawn for small interface
 * text. The RuneScape fonts are pixel faces built for one size, so they blur when scaled to the
 * sizes the summary figures need.
 * <p>
 * Rather than bundle a typeface and carry it in the jar, this resolves the first genuinely good
 * face already installed and falls back through the list, ending at whatever RuneLite would have
 * used anyway. The panel looks native on each platform instead of identical on all of them, which
 * for a side panel inside someone else's client is the better trade.
 */
final class Fonts
{
	/**
	 * In preference order: Segoe UI ships with Windows, Inter and Roboto are common developer
	 * installs, and the last two are the usual Linux and general fallbacks.
	 */
	private static final String[] PREFERRED = {
		"Segoe UI",
		"Inter",
		"Roboto",
		"Noto Sans",
		"DejaVu Sans",
	};

	private static final String FAMILY;
	private static final String SEMIBOLD_FAMILY;

	static
	{
		Set<String> available = new HashSet<>(Arrays.asList(
			GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

		String chosen = null;
		for (String candidate : PREFERRED)
		{
			if (available.contains(candidate))
			{
				chosen = candidate;
				break;
			}
		}

		FAMILY = chosen;
		// A true semibold reads better than synthetic bold for figures, where heavy strokes close
		// up the counters at small sizes.
		SEMIBOLD_FAMILY = chosen != null && available.contains(chosen + " Semibold")
			? chosen + " Semibold"
			: null;
	}

	private Fonts()
	{
	}

	static Font regular(float size)
	{
		return FAMILY == null
			? FontManager.getDefaultFont().deriveFont(size)
			: new Font(FAMILY, Font.PLAIN, Math.round(size));
	}

	static Font bold(float size)
	{
		if (SEMIBOLD_FAMILY != null)
		{
			return new Font(SEMIBOLD_FAMILY, Font.PLAIN, Math.round(size));
		}
		return FAMILY == null
			? FontManager.getDefaultBoldFont().deriveFont(size)
			: new Font(FAMILY, Font.BOLD, Math.round(size));
	}

	/**
	 * @return the resolved family, for logging at startup
	 */
	static String describe()
	{
		return FAMILY == null ? "RuneLite default" : FAMILY + (SEMIBOLD_FAMILY != null ? " (+ Semibold)" : "");
	}
}
