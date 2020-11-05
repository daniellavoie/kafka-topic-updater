package com.github.daniellavoie.topicnotary.core.migration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.daniellavoie.topicnotary.core.migration.exception.InvalidMigrationDefinitionException;
import com.github.daniellavoie.topicnotary.core.util.Resource;
import com.github.daniellavoie.topicnotary.core.util.ResourceUtil;

public abstract class MigrationEntryUtil {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final Pattern SEMVER_REGEX = Pattern
			.compile("(\\d+)\\.(\\d+)(?:\\.)?(\\d*)(\\.|-|\\+)?([0-9A-Za-z-.]*)?");

	private static void assertAclEntry(AclEntry aclEntry, MigrationDefinition migrationDefinition,
			MigrationEntry migrationEntry) {
		String[] principalEntries = aclEntry.getPrincipal().split(":");

		if (principalEntries.length != 2) {
			throw new InvalidMigrationDefinitionException(
					"Principal definition doesn not march `Type:Name` format for acl entry of topic `"
							+ migrationDefinition.getTopic() + "` in migration file " + migrationEntry.getVersion()
							+ " - " + migrationEntry.getDescription() + ".");
		}
	}

	private static void assertMigrationEntry(MigrationEntry migrationEntry) {
		migrationEntry.getDefinitions().stream()

				.forEach(migrationDefinition -> assertMigrationDefinition(migrationDefinition, migrationEntry));
	}

	private static void assertMigrationDefinition(MigrationDefinition migrationDefinition,
			MigrationEntry migrationEntry) {
		Optional.ofNullable(migrationDefinition.getCreateAcls()).ifPresent(acls -> acls.stream()
				.forEach(aclEntry -> assertAclEntry(aclEntry, migrationDefinition, migrationEntry)));

		Optional.ofNullable(migrationDefinition.getDeleteAcls()).ifPresent(acls -> acls.stream()
				.forEach(aclEntry -> assertAclEntry(aclEntry, migrationDefinition, migrationEntry)));
	}

	private static MigrationEntry buildUpdateEntry(String environment, Resource resource) {
		try {
			String filename = resource.getFilename();
			if (!filename.endsWith(".json")) {
				throw new InvalidMigrationDefinitionException(resource.getFilename() + " is not a json file.");
			}
			String[] parts = resource.getFilename().split("__");

			if (parts.length != 2) {
				throw new InvalidMigrationDefinitionException("filename for " + resource.getFilename()
						+ " does not match the expect `VX_X_X[-XXXX]__Upgrade_Description.json` format (e.g.: V2_4_8-dev.1__Test_Release_Candidate.json)");
			}

			String version = parts[0].split("V")[1].replace("_", ".");
			String description = parts[1].split("\\.json")[0].replace("_", " ");

			validateSemver(version);

			String rawUpdateDefinition = ResourceUtil.readResource(resource);
			List<MigrationDefinition> definitions = OBJECT_MAPPER.readValue(rawUpdateDefinition,
					new TypeReference<List<MigrationDefinition>>() {
					});

			MigrationEntry migrationEntry = new MigrationEntry(environment, computeChecksum(rawUpdateDefinition),
					version, description, null, definitions);

			assertMigrationEntry(migrationEntry);

			return migrationEntry;
		} catch (IOException ex) {
			throw new InvalidMigrationDefinitionException(ex);
		}
	}

	protected static List<MigrationEntry> loadMigrationEntries(String environment, String migrationDir) {
		List<MigrationEntry> entries = ResourceUtil.getResourceFiles(migrationDir).stream()
				.map(resource -> MigrationEntryUtil.buildUpdateEntry(environment, resource))
				.collect(Collectors.toList());

		entries.sort(
				(o1, o2) -> (o1.getVersion() + o1.getDescription()).compareTo(o2.getVersion() + o2.getDescription()));
		;

		return entries;
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

	private static void validateSemver(String version) {
		Matcher matcher = SEMVER_REGEX.matcher(version);

		if (!matcher.find()) {
			throw new InvalidMigrationDefinitionException("version `" + version
					+ "` is not a valid semver. The format sementic is explained at https://semver.org/ ");
		}
	}
}
