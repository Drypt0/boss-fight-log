package com.bossfightlog;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

	/**
	 * How many superseded copies to keep alongside the live file.
	 */
	private static final int MAX_BACKUPS = 5;
	private static final String BACKUP_SUFFIX = ".bak.json";
	private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

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

	/**
	 * Persists after copying whatever is on disk aside first. Used for destructive changes, so a
	 * mistaken clear costs a few clicks to undo rather than the entire history.
	 *
	 * @param fights snapshot to persist; the caller must not mutate it afterwards
	 */
	void saveWithBackup(String character, List<BossFight> fights)
	{
		List<BossFight> snapshot = new ArrayList<>(fights);
		// Both steps run in one task so the copy cannot be scheduled after the overwrite.
		executor.execute(() -> {
			File file = fileFor(character);
			backup(file);
			writeFile(file, snapshot);
		});
	}

	private void backup(File file)
	{
		if (!file.exists())
		{
			return;
		}

		try
		{
			String stamp = BACKUP_STAMP.format(LocalDateTime.now());
			File copy = new File(directory, file.getName().replace(".json", "") + "-" + stamp + BACKUP_SUFFIX);
			Files.copy(file.toPath(), copy.toPath(), StandardCopyOption.REPLACE_EXISTING);
			log.debug("Backed up fight history to {}", copy);
			prune(file.getName().replace(".json", ""));
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not back up fight history before overwriting {}", file, e);
		}
	}

	/**
	 * Keeps only the most recent backups, so the folder does not accumulate copies forever.
	 */
	private void prune(String prefix)
	{
		File[] backups = directory.listFiles((dir, name) ->
			name.startsWith(prefix + "-") && name.endsWith(BACKUP_SUFFIX));

		if (backups == null || backups.length <= MAX_BACKUPS)
		{
			return;
		}

		Arrays.sort(backups, Comparator.comparing(File::getName));
		for (int i = 0; i < backups.length - MAX_BACKUPS; i++)
		{
			if (!backups[i].delete())
			{
				log.debug("Could not remove old backup {}", backups[i]);
			}
		}
	}

	private List<BossFight> readFile(File file)
	{
		if (!file.exists())
		{
			return new ArrayList<>();
		}

		try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))
		{
			BossFight[] fights = gson.fromJson(reader, BossFight[].class);
			return fights == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(fights));
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
				gson.toJson(fights.toArray(new BossFight[0]), BossFight[].class, writer);
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
}
