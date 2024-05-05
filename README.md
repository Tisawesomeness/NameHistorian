# NameHistorian

Records and displays player name changes.

![Screenshot of /history command](docs/demo.png)

**Minecraft Version:** 1.8 - 1.20.6

## Commands

### `/history <player>`

Displays the name history of a player.

**Permission:** `namehistorian.history` (default: op)

### `/namehistorian reload`

Reloads the config and translations.

**Permission:** `namehistorian.reload` (default: op)

## Translations

To change the plugin's messages, go to the `plugins/NameHistorian/translations` folder and edit the `en.properties` file.

To add a new language, create a new `<locale_id>.properties` file.

- For example, use `es.properties` for Spanish, or `pt_BR.properties` for Portuguese (Brazil).
- A list of locale IDs can be found [here](https://www.localeplanet.com/java/).

Edit `config.yml` to change the default language or to toggle per-player translations.

## Building

To build NameHistorian, run `gradlew build`. The output jars are in the `build/libs` directory.
