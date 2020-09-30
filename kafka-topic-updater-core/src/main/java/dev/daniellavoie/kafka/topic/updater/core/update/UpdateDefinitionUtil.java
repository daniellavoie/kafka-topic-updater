package dev.daniellavoie.kafka.topic.updater.core.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.daniellavoie.kafka.topic.updater.core.InvalidUpdateDefinitionException;

public abstract class UpdateDefinitionUtil {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final String SEMVER_REGEX = "^(?P<major>0|[1-9]\\d*)\\.(?P<minor>0|[1-9]\\d*)\\.(?P<patch>0|[1-9]\\d*)(?:-(?P<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?P<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";

	public static UpdateEntry buildUpdateEntry(Resource resource) {
		try {
			String filename = resource.getFilename();
			if (!filename.endsWith(".json")) {
				throw new InvalidUpdateDefinitionException(
						resource.getFile().getAbsolutePath() + " is not a json file.");
			}
			String[] parts = resource.getFilename().split("__");

			if (parts.length != 2) {
				throw new InvalidUpdateDefinitionException("filename for " + resource.getFile().getAbsolutePath()
						+ " does not match the expect VX_X_X__Upgrade_Description.json format");
			}

			String version = parts[0].split("V")[0].replace("_", ".");
			String description = parts[1].split("\\.json")[0].replace("_", " ");

			getSemver(version);

			String rawUpdateDefinition = readUpdateDefinition(resource);
			UpdateDefinition updateDefinition = OBJECT_MAPPER.readValue(rawUpdateDefinition, UpdateDefinition.class);

			return UpdateEntry.builder().version(version)

					.checksum(computeChecksum(rawUpdateDefinition))

					.description(description)

					.updateDefinition(updateDefinition)

					.build();
		} catch (IOException ex) {
			throw new InvalidUpdateDefinitionException(ex);
		}
	}

	public static String readUpdateDefinition(Resource resource) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			return reader.lines().collect(Collectors.joining());
		} catch (IOException e) {
			throw new InvalidUpdateDefinitionException(e);
		}
	}

	private static String computeChecksum(String rawUpdateDefinition) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] bytes = rawUpdateDefinition.getBytes(("UTF-8"));
			md.update(bytes);
			byte[] digest = md.digest();

			return Base64.getEncoder().encodeToString(digest);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Semver getSemver(String version) {
		Matcher matcher = Pattern.compile(SEMVER_REGEX).matcher(version);

		if (matcher.find()) {
			return Semver.builder()

					.major(Integer.parseInt(matcher.group(0)))

					.minor(Integer.parseInt(matcher.group(1)))

					.patch(Integer.parseInt(matcher.group(2)))

					.pre(matcher.group(3))

					.build();
		} else {
			throw new InvalidUpdateDefinitionException("");
		}
	}
}
