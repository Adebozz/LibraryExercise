package reporting;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Iterator;

/**
 * Appends each report to a text file.
 *
 * The file is opened per report and closed straight after, so no file handle
 * is held between reports and nothing needs closing at shutdown. Reports are
 * appended so earlier ones are kept, and written one at a time so concurrent
 * reports cannot interleave.
 */
public class FileOutputService implements OutputService {
	private final Path path;

	public FileOutputService(String path) {
		this.path = Path.of(path);
	}

	public synchronized void outputExtent(String title, Collection subscribers) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
			writer.write("\n" + title + "\n-----------\n");
			OutputServiceContributor subscriber;
			for (Iterator iter = subscribers.iterator(); iter.hasNext();) {
				subscriber = (OutputServiceContributor)iter.next();
				writer.write(subscriber.getFormattedString());
				writer.write("\n");
			}
		}
	}

	public String getName() {
		return "File output service";
	}
}
