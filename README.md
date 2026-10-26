# CarbonBans

A Minecraft server moderation plugin providing ban, mute, warn, kick, history, player-check, ban-list, mute-list, and alternate-account commands.

## Project status

This archive packages the available Java source and project files for publishing on GitHub. It does **not** claim that the plugin has passed a full security audit or that every feature has been tested on every server version. Review and test it on a staging server before using it on a production server.

## Requirements

- Java 17
- Maven 3.8+
- A compatible Bukkit/Spigot/Paper server

The current `pom.xml` compiles against Spigot API `1.20.4-R0.1-SNAPSHOT`. If you intend to support a newer Paper version, update and test the API dependency deliberately.

## Build from source

From the directory containing `pom.xml`, run:

```bash
mvn clean package
```

The plugin JAR should be generated at:

```text
target/CarbonBans.jar
```

Do not commit the `target/` directory or a locally built JAR as a substitute for source code. GitHub Actions is configured to build the project from the committed source.

## GitHub publishing

1. Create a new GitHub repository named `CarbonBans`.
2. Extract this archive.
3. Upload the contents of the `CarbonBans` folder to the repository root (so `pom.xml` is at the root).
4. Commit and push the files.
5. Check the **Actions** tab to see whether the automated build succeeds.

## Data and privacy

CarbonBans stores moderation records in its SQLite database. The supplied implementation also records player IP addresses for alternate-account lookup. IP addresses are personal data: tell players and staff what is collected, restrict access to database backups, set a retention policy, and delete records when they are no longer needed. Review your applicable laws and server rules before enabling this feature.

## Contributing

Open an issue to report a bug or propose a feature. Pull requests should include a clear description and testing notes. Never include real player IP addresses, UUIDs, credentials, database files, or private server logs in issues or pull requests.

## License

Released under the MIT License; see [LICENSE](LICENSE).
