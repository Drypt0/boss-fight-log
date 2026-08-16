package com.bossfightlog;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Reads and writes recorded kills under {@code .runelite/boss-fight-log/}, one file per character.
 * <p>
 * All disk access runs on the shared executor rather than the client thread. Callbacks are handed
 * back on that executor thread, so callers are responsible for marshalling onto whichever thread
 * owns the data they are updating.
 */
@Slf4j
@Singleton
class FightStore
{
	/**
	 * Oldest kills are dropped past this point. Without a cap the file grows for as long as the
	 * plugin is installed.
	 */
	static final int MAX_FIGHTS = 1000;

	private static final Type FIGHT_LIST = new TypeToken<List<BossFight>>()
	{
	}.getType();

	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final File directory;

	@Inject
	private FightStore(Gson gson, ScheduledExecutorService executor)
	{
		// Instant has no built-in Gson representation, so it is stored as epoch millis.
		this.gson = gson.newBuilder()
			.registerTypeAdapter(Instant.class,
				(JsonSerializer<Instant>) (src, type, ctx) -> new JsonPrimitive(src.toEpochMilli()))
			.registerTypeAdapter(Instant.class,
				(JsonDeserializer<Instant>) (json, type, ctx) -> Instant.ofEpochMilli(json.getAsLong()))
			.create();
		this.executor = executor;
		this.directory = new File(RuneLite.RUNELITE_DIR, "boss-fight-log");
	}

	void load(String character, Consumer<List<BossFight>> callback)
	{
		executor.execute(() -> {
			List<BossFight> fights = readFile(fileFor(character));
			callback.accept(fights);
		});
	}

	/**
	 * @param fights snapshot to persist; the caller must not mutate it afterwards
	 */
	void save(String character, List<BossFight> fights)
	{
		List<BossFight> snapshot = new ArrayList<>(fights);
		executor.execute(() -> writeFile(fileFor(character), snapshot));
	}

	private List<BossFight> readFile(File file)
	{
		if (!file.exists())
		{
			return new ArrayList<>();
		}

		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
		{
			List<BossFight> fights = gson.fromJson(reader, FIGHT_LIST);
			return fights != null ? fights : new ArrayList<>();
		}
		catch (IOException | RuntimeException e)
		{
			// A corrupt or truncated file should cost the history, not the plugin.
			log.warn("Could not read fight history from {}, starting empty", file, e);
			return new ArrayList<>();
		}
	}

	private void writeFile(File file, List<BossFight> fights)
	{
		try
		{
			Files.createDirectories(directory.toPath());
			try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))
			{
				gson.toJson(fights, FIGHT_LIST, writer);
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not write fight history to {}", file, e);
		}
	}

	/**
	 * Display names allow spaces and are case insensitive in game, so they are folded to a stable
	 * lower case form and stripped of anything that is awkward in a filename.
	 */
	private File fileFor(String character)
	{
		String safe = character.replace('\u00A0', ' ')
			.toLowerCase()
			.trim()
			.replaceAll("[^a-z0-9]+", "_");

		if (safe.isEmpty())
		{
			safe = "unknown";
		}

		return new File(directory, safe + ".json");
	}

	static List<BossFight> capped(List<BossFight> fights)
	{
		if (fights.size() <= MAX_FIGHTS)
		{
			return fights;
		}
		return new ArrayList<>(fights.subList(fights.size() - MAX_FIGHTS, fights.size()));
	}

	static List<BossFight> unmodifiable(List<BossFight> fights)
	{
		return Collections.unmodifiableList(fights);
	}
}
