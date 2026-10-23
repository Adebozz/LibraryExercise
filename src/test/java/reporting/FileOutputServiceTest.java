package reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileOutputServiceTest {

	@TempDir
	Path dir;

	@Test
	void reportIsWrittenToFile() throws Exception {
		Path file = dir.resolve("report.txt");
		FileOutputService output = new FileOutputService(file.toString());

		output.outputExtent("Loans", items("Book - Dune", "Game - Halo"));

		List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		assertEquals(List.of("", "Loans", "-----------", "Book - Dune", "Game - Halo"), lines);
	}

	@Test
	void laterReportsAreAddedNotOverwritten() throws Exception {
		Path file = dir.resolve("report.txt");
		FileOutputService output = new FileOutputService(file.toString());

		output.outputExtent("Resources", items("Book - Dune"));
		output.outputExtent("Loans", items("Game - Halo"));

		String content = Files.readString(file, StandardCharsets.UTF_8);
		assertTrue(content.contains("Resources") && content.contains("Book - Dune"), "first report kept: " + content);
		assertTrue(content.contains("Loans") && content.contains("Game - Halo"), "second report written: " + content);
	}

	/**
	 * Several librarians may run reports at once. Each report must appear as one
	 * contiguous block: every item line follows its own report's title.
	 */
	@Test
	void concurrentReportsDoNotInterleave() throws Exception {
		Path file = dir.resolve("report.txt");
		FileOutputService output = new FileOutputService(file.toString());
		int reports = 20;
		int itemsPerReport = 200;

		ExecutorService pool = Executors.newFixedThreadPool(reports);
		List<Future<?>> futures = new ArrayList<>();
		for (int r = 0; r < reports; r++) {
			int report = r;
			List<OutputServiceContributor> lines = new ArrayList<>();
			for (int i = 0; i < itemsPerReport; i++) {
				lines.add(item("item " + report + "-" + i));
			}
			futures.add(pool.submit(() -> {
				output.outputExtent("Report " + report, lines);
				return null;
			}));
		}
		for (Future<?> future : futures) {
			future.get();
		}
		pool.shutdown();

		String currentReport = null;
		int itemLines = 0;
		for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
			if (line.startsWith("Report ")) {
				currentReport = line.substring("Report ".length());
			}
			else if (line.startsWith("item ")) {
				assertEquals(currentReport, line.substring("item ".length(), line.indexOf('-')), "line out of place: " + line);
				itemLines++;
			}
		}
		assertEquals(reports * itemsPerReport, itemLines, "every line of every report written");
	}

	private static List<OutputServiceContributor> items(String... lines) {
		List<OutputServiceContributor> items = new ArrayList<>();
		for (String line : lines) {
			items.add(item(line));
		}
		return items;
	}

	private static OutputServiceContributor item(String line) {
		return () -> line;
	}
}
