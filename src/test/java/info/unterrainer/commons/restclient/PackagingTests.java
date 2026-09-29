package info.unterrainer.commons.restclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Guards what the published jar contains. The main output directory is exactly
 * what the jar plugin packs, so a resource found there ships to every consumer.
 * The test classpath also holds {@code log4j2-test.xml} and dependency jars,
 * which is why each hit is checked by its location rather than by a plain
 * lookup.
 */
public class PackagingTests {

	private static final List<String> LOG4J2_CONFIG_NAMES = List.of("log4j2", "log4j2-test");
	private static final List<String> LOG4J2_CONFIG_EXTENSIONS = List.of("xml", "json", "yaml", "yml", "properties");

	@Test
	public void mainOutputShipsNoLog4j2Configuration() throws IOException {
		String mainOutput = RestClient.class.getProtectionDomain().getCodeSource().getLocation().toString();
		ClassLoader classLoader = PackagingTests.class.getClassLoader();

		List<String> shipped = new ArrayList<>();
		for (String name : LOG4J2_CONFIG_NAMES)
			for (String extension : LOG4J2_CONFIG_EXTENSIONS)
				for (URL url : Collections.list(classLoader.getResources(name + "." + extension)))
					if (url.toString().startsWith(mainOutput))
						shipped.add(url.toString());

		assertTrue(shipped.isEmpty(), "Logging configuration in the main output directory: " + shipped);
	}
}
