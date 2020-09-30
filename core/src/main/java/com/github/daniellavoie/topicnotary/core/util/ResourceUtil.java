package com.github.daniellavoie.topicnotary.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.daniellavoie.topicnotary.core.migration.exception.InvalidMigrationDefinitionException;

public class ResourceUtil {
	public static List<Resource> getResourceFiles(String path) {
		return path.startsWith("classpath:") ? getResourcesFromClasspath(path.substring(10))
				: getResourcesFromPath(path);
	}

	private static List<Resource> getResourcesFromClasspath(String path) {
		List<Resource> filenames = new ArrayList<>();

		try (InputStream in = getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalArgumentException("could not find any migration in classpath path " + path);
			}

			try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
				String resource;

				while ((resource = br.readLine()) != null) {
					filenames.add(new Resource(path + "/" + resource, true));
				}
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return filenames;
	}

	private static List<Resource> getResourcesFromPath(String path) {
		try {
			return Files.list(Paths.get(path))
					.map(childPath -> new Resource(path + "/" + childPath.getFileName().toString(), false))
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static InputStream getResourceAsStream(String resource) {
		final InputStream in = getContextClassLoader().getResourceAsStream(resource);

		return in == null ? ResourceUtil.class.getResourceAsStream(resource) : in;
	}

	private static ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	public static String readResource(Resource resource) {
		if (resource.isInClasspath()) {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(getResourceAsStream(resource.getFilename())))) {
				return reader.lines().collect(Collectors.joining());
			} catch (IOException e) {
				throw new InvalidMigrationDefinitionException(e);
			}
		} else {
			try {
				return Files.readAllLines(Paths.get(resource.getFilename())).stream().collect(Collectors.joining("\n"));
			} catch (IOException e) {
				throw new InvalidMigrationDefinitionException(e);
			}
		}
	}
}
