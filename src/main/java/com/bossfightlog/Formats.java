package com.bossfightlog;

import java.awt.Color;

/**
 * Display formatting shared by the panel widgets.
 */
final class Formats
{
	/**
	 * Differences smaller than this are rounding rather than performance.
	 */
	private static final double TREND_NOISE_THRESHOLD = 0.01d;

	/**
	 * Sampled from the reference design. RuneLite's progress colours are pitched for a bar that
	 * has to read at a glance across the client; on a dense panel of figures they shout.
	 */
	static final Color TREND_UP = new Color(0x4DBB5F);
	static final Color TREND_DOWN = new Color(0xE04B4B);

	/**
	 * Card fill, and the fill while hovered.
	 * <p>
	 * RuneLite's hover colour doubles the luminance of a card, from grey 30 to grey 60, which is
	 * built for list rows on a lighter background and reads as a flash here. Eight points is enough
	 * to show which card the pointer is on without the panel lighting up under it.
	 */
	static final Color CARD_COLOR = new Color(30, 30, 30);
	static final Color CARD_HOVER_COLOR = new Color(38, 38, 38);

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
	 * As {@link #percentSigned}, but keeping a decimal place. Used where rounding a small
	 * difference to a signed zero would be more confusing than the extra digit.
	 */
	static String percentSignedPrecise(double fraction)
	{
		return String.format("%+.1f%%", fraction * 100);
	}

	/**
	 * The trend as plain text for a standalone label, where the colour is carried by the component
	 * rather than by markup. Empty when there is no baseline; "on par" inside the noise threshold,
	 * because a blank there would look identical to a first kill.
	 */
	static String trendText(Double fraction)
	{
		if (fraction == null)
		{
			return "";
		}
		return Math.abs(fraction) < TREND_NOISE_THRESHOLD ? "on par" : percentSigned(fraction);
	}

	/**
	 * Renders a trend as coloured HTML.
	 * <p>
	 * A null fraction means there is nothing to compare against, and renders as nothing at all. A
	 * fraction inside the noise threshold renders as "on par" instead: below a percent the sign is
	 * rounding rather than performance, but staying silent would make an average kill look
	 * identical to a first one, which are not the same thing at all.
	 * <p>
	 * Arrow glyphs are avoided because the RuneScape font does not carry them.
	 */
	static String trendHtml(Double fraction)
	{
		if (fraction == null)
		{
			return "";
		}

		if (Math.abs(fraction) < TREND_NOISE_THRESHOLD)
		{
			return "  <font color='" + hex(net.runelite.client.ui.ColorScheme.MEDIUM_GRAY_COLOR)
				+ "'>on par</font>";
		}

		Color color = fraction > 0 ? TREND_UP : TREND_DOWN;

		return "  <font color='" + hex(color) + "'>" + percentSigned(fraction) + "</font>";
	}
}
