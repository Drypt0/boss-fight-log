package com.bossfightlog;

import java.awt.Color;

/**
 * Display formatting shared by the panel widgets.
 */
final class Formats
{
	private Formats()
	{
	}

	/**
	 * Renders a colour as a CSS hex string, for use inside HTML labels.
	 */
	static String hex(Color color)
	{
		return String.format("#%06x", color.getRGB() & 0xFFFFFF);
	}

	/**
	 * Formats a duration as m:ss, or h:mm:ss once it runs past an hour.
	 */
	static String duration(double seconds)
	{
		int total = (int) Math.round(seconds);
		int hours = total / 3600;
		int minutes = (total % 3600) / 60;
		int secs = total % 60;

		return hours > 0
			? String.format("%d:%02d:%02d", hours, minutes, secs)
			: String.format("%d:%02d", minutes, secs);
	}

	static String dps(double dps)
	{
		return String.format("%.1f", dps);
	}

	/**
	 * @param fraction a value in the range 0..1
	 */
	static String percent(double fraction)
	{
		return String.format("%.0f%%", fraction * 100);
	}

	static String number(int value)
	{
		return String.format("%,d", value);
	}

	/**
	 * @param fraction a signed value, where 0.12 reads as +12%
	 */
	static String percentSigned(double fraction)
	{
		return String.format("%+.0f%%", fraction * 100);
	}

	/**
	 * Renders a trend as coloured HTML, or nothing at all when there is no baseline to compare
	 * against. Arrow glyphs are avoided because the RuneScape font does not carry them.
	 */
	static String trendHtml(Double fraction)
	{
		if (fraction == null)
		{
			return "";
		}

		// Below a percent the difference is noise, and a flickering +0%/-0% reads as meaningful.
		if (Math.abs(fraction) < 0.01)
		{
			return "";
		}

		Color color = fraction > 0
			? net.runelite.client.ui.ColorScheme.PROGRESS_COMPLETE_COLOR
			: net.runelite.client.ui.ColorScheme.PROGRESS_ERROR_COLOR;

		return "  <font color='" + hex(color) + "'>" + percentSigned(fraction) + "</font>";
	}
}
