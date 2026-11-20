# Analytics events

The custom events of Genshin Map and RDR2 Map for Google Analytics for Firebase. Collection is on in release
builds only, as for Crashlytics. Each game is a Firebase project of its own, so no event needs a game parameter.

## In the code

`core/analytics` is the same for every app and platform:

- `Analytics` comes from Koin (`analyticsModule`, which both apps include): `log(event)`, `setUserProperty`,
  `setDefaultParams`. It sends each event to the trackers of the platform: Firebase, and the log in a debug build,
  where Firebase sends nothing.
- An event is a class implementing `AnalyticsEvent`: a name and parameters of strings, numbers, booleans and
  enums; `Analytics` turns them into what Firebase takes (a boolean becomes `true` or `false`, an enum its name in
  lower case, a string is cut to 100 characters). The common events are in `AnalyticsEvents.kt`. An app or a
  platform gets events of its own by implementing `AnalyticsEvent` in its module or source set.
- `ScreenTracker` follows the screen on top of the navigation and whether the app is in the foreground, logs
  `screen_view` and tells the listeners which screen the user sees.
- `MapAnalytics` of `feature/map/ui-common` logs `map_ready` and `map_time`.
- `AnalyticsId` is a random id of the installation, kept on the device: the user id of Analytics and of
  Crashlytics, shown in the settings as Analytics ID, a tap copies it.
- `CrashReporter` comes from Koin too: `log`, `setKeys`, `recordNonFatal` for Crashlytics on Android; on iOS it
  only prints in a debug build for now.

## Rules

- Names are snake_case: up to 40 characters for events and parameters, 24 for user properties. A string value
  is up to 100 characters, 36 for a user property. An event has at most 25 parameters, the test of
  `core/analytics` checks the common ones.
- No personal data. The only text the user types that is sent is the search term: trimmed, lowercased and cut
  to 100 characters. The privacy policy ([privacy-policy/index.html](privacy-policy/index.html)) says so, keep
  the two in step.
- Firebase logs `first_open`, `session_start`, `user_engagement`, `app_update`, `os_update`, `app_remove` and,
  with Crashlytics, `app_exception` on its own, don't duplicate them. Its automatic `screen_view` is off in the
  manifests and the Info.plist files: it would see only `MainActivity` and the root view controller.
- `map_id` of the open map goes with every event logged after the map got its tiles: a default parameter. The
  names of the maps and categories are translated, so the ids are sent; the names are in Remote Config and
  `marker_catalog.json`.

## Events

| Event | Fires when | Parameters |
|---|---|---|
| `screen_view` | A screen is shown: at the launch, on the way to another screen, back from the background | `screen_name`, `screen_class`, `previous_screen` |
| `map_ready` | The first tile of a map is on the screen | `source`, `load_ms`, `launch_ms`, `tiles_failed`, `was_hidden` |
| `map_time` | The user leaves the map screen: for settings or the background, which closing the app goes through | `screen_ms`, `ready_ms`, `is_ready`, `exit_to`, `has_map`, `is_first_visit`, `tiles_loaded`, `tiles_failed`, `tile_error` |
| `search` | A search term stays for 1.5 s | `search_type`, `search_term`, `results_count` |
| `marker_found_change` | Found is pressed on a card | `location_id`, `category_id`, `is_found`, `found_total` |
| `setting_change` | A setting gets another value | `setting_name`, `setting_value` |
| `found_markers_transfer` | The found markers are exported to a file or imported from one in the settings; a cancelled picker sends nothing | `transfer_type`, `transfer_result`, `markers_count` |
| `route_build` | A build of a route ends: built, cancelled, no roads on the map, or failed | `route_result`, `locations_count`, `fast_travels`, `build_ms` |
| `route_exit` | The route is dropped: the user ends it, or hides markers of it in the marker list | `end_reason`, `locations_count`, `found_count`, `route_page` |

`map_ready` and `map_time` show how long the map takes to load and the visits it never loaded in: `is_ready`
is `false` and `tile_error` tells why. Google Maps doesn't tell when it has drawn a tile, so the first tile is
taken as drawn on the second frame after the map got it, which is close to it to a few tens of milliseconds.

`search` isn't sent for a term under 2 characters or for the same term twice in a row. A term with no results
matters most: it tells what people look for and don't find.

## Parameters

| Parameter | Type | Values |
|---|---|---|
| `map_id` | number | The id of the map in Remote Config, a default parameter |
| `screen_name`, `screen_class` | string | `map`, `settings`. Compose screens have no classes, the name is the class too |
| `previous_screen` | string | The screen left for this one, `background` when the app comes back; none at the launch |
| `source` | string | `launch` (the first map since the start of the app), `map_switch` (another map is picked), `reload` (the same map gets its tiles again: the activity is recreated, the language changes) |
| `load_ms` | number | From the moment the map got its tile layer to the first tile on the screen |
| `launch_ms` | number | From the start of the app to the first tile on the screen, with `launch` only |
| `tiles_failed` | number | The tiles that couldn't be loaded: before the first one drawn in `map_ready`, since the map got its tiles in `map_time` |
| `was_hidden` | string | `true` when the map screen was left while the tiles were loading, the times include the time away then |
| `screen_ms` | number | From the moment the map screen was shown to the event |
| `ready_ms` | number | From `map_ready` to the event, or from the moment the screen was shown when the map was ready before; none when it isn't ready |
| `is_ready` | string | `true` when the open map has its first tile drawn |
| `exit_to` | string | The screen the user went to, or `background` |
| `has_map` | string | `false` when no map was open: the remote config hasn't come |
| `is_first_visit` | string | `true` for the first visit of the map screen since the start of the app |
| `tiles_loaded` | number | The tiles with an image the open map got since it got its tile layer |
| `tile_error` | string | Why the last tile couldn't be loaded: `timeout`, `no_network`, `ssl`, `http_<code>` (not 404, there are no tiles past the edges of the maps), `decode`, `url_error_<code>` (iOS) or the name of the error |
| `search_type` | string | `map` (the search field of the map), `marker_list` (the search of the list), `link` (a search link in a description) |
| `search_term` | string | The term: trimmed, lowercased, up to 100 characters |
| `results_count` | number | `map`, `link`: the markers found, up to the limit of the request. `marker_list`: the categories found |
| `location_id` | number | The id of the marker |
| `category_id` | string | The id of its category in `marker_catalog.json`: `o3`, `33` |
| `is_found` | string | `true`, `false`: the new state |
| `found_total` | number | The found markers of the game after the change |
| `setting_name` | string | `hide_markers_while_moving`, `cluster_markers`, `theme`, `dynamic_color`, `language` |
| `setting_value` | string | `true`, `false`; `system`, `light`, `dark`; a language tag or `system` |
| `route_result` | string | `built`, `cancelled`, `unavailable` (the map has no roads), `failed` |
| `locations_count` | number | The markers of the route: the ones the filters show on the whole map and not found yet |
| `fast_travels` | number | The fast travel points of a built route, the first one included; none when it wasn't built |
| `build_ms` | number | From the press of the route button to the end of the build |
| `transfer_type` | string | `export`, `import` |
| `transfer_result` | string | `done`, `failed` (the file couldn't be written), `not_a_file`, `other_game` (a file of another game) |
| `markers_count` | number | Export: the markers in the file. Import: the ones it added that weren't found before |
| `end_reason` | string | `ended` (End in the dialog of the route button), `markers_hidden` (markers of the route were hidden in the marker list) |
| `found_count` | number | The markers of the route found by the time it's ended |
| `route_page` | number | The marker of the route the user had come to, from 0 |

## Next events

Not in the code yet:

| Event | Fires when | Parameters |
|---|---|---|
| `map_open` | A map is shown, at launch or when another one is picked | `source`: `restored`, `default`, `selector` |
| `marker_open` | The card of a marker opens | `location_id`, `category_id`, `source`: `map`, `link`; `is_found` |
| `marker_list_open` | The marker list opens | `visible_categories` |
| `category_visibility_change` | A category is shown or hidden in the list | `category_id`, `is_visible`, `from_search` |
| `all_categories_visibility_change` | Show all or Hide all is pressed | `is_visible` |
| `category_cycle_change` | A cycle of a category is picked (RDR2) | `category_id`, `cycle` |
| `dungeons_toggle` | The dungeons button is pressed | `is_active` |
| `dungeon_floor_select` | A floor is picked on a card | `location_id`, `floor_id` |
| `description_link_click` | A link in the description of a marker is opened | `location_id`, `link_type`: `marker`, `search`, `external`; `link_domain` |
| `settings_link_click` | The privacy policy or a link of the signature is opened | `link`: `privacy_policy`, `telegram`, `github`, `instagram` |

User properties for later, set at launch and whenever they change: `ui_language`, `theme_mode`,
`dynamic_color`, `cluster_markers`, `found_markers` (`0`, `1-9`, `10-99`, `100-999`, `1000+`).

## Analytics setup

GA4 reports show a parameter only once it's registered in Admin → Custom definitions, and only from that day
on, so register them before the release:

- Event-scoped dimensions: `map_id`, `previous_screen`, `source`, `was_hidden`, `is_ready`, `exit_to`, `has_map`,
  `is_first_visit`, `tile_error`, `search_type`, `category_id`, `is_found`, `setting_name`, `setting_value`,
  `route_result`, `end_reason`, `transfer_type`, `transfer_result`.
- Metrics: `load_ms`, `launch_ms`, `screen_ms`, `ready_ms`, `build_ms` (milliseconds), `tiles_loaded`,
  `tiles_failed`, `results_count`, `found_total`, `locations_count`, `fast_travels`, `found_count`, `route_page`, `markers_count`.
- `search_term` is the built-in Search term dimension, `screen_name` and `screen_class` are built in too.
  `location_id` has too many values for the reports.

## Crash reports

Crashlytics shows the events of the session before a crash or a non-fatal error as breadcrumbs, the Logs tab of
the event. Besides:

- Keys: `screen`, `map_id`, `map_ready`, `tile_error` (the last reason a tile failed), `setting_<name>` for each
  setting of `setting_change`.
- Log: the loads of the maps (`Map 2: loading, launch`, `Map 2: drawn in 300 ms`), each new reason a tile failed
  for, the fetches of Remote Config and their errors.
- Non-fatal errors: `MapNotDrawnException` when the user stays on the map screen for 10 s without the map, once a
  load, with the tiles loaded and failed; the `SerializationException` of a Remote Config value that can't be
  parsed.
- The user id is the analytics ID: a user who tells it from the settings is found in the search of Crashlytics
  and in the `user_id` of the BigQuery export of Analytics.

## Checking the events

A debug build prints the events instead of sending them: `Analytics: map_ready {…}` in Logcat or in the console of
Xcode, and the reports as `Crash log: …`, `Crash keys: {…}`, `Crash non-fatal: …`. DebugView of the Firebase console shows the events of a release build when debug mode is on:
`adb shell setprop debug.firebase.analytics.app com.onthecrow.genshinmap` on Android, the `-FIRDebugEnabled`
launch argument on iOS.
