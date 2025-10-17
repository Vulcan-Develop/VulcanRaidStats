# VulcanRaidStats

A Minecraft plugin for tracking and displaying detailed statistics during faction raids. Built for FactionsKore, this plugin provides real time raid stat tracking, grace period management, and an intuitive GUI for viewing raid performance.

## Features

### Stat Tracking
- **Combat Stats**: Kills, deaths, damage dealt/taken, and hit counts
- **Building Stats**: Blocks placed during raids
- **Real-time Updates**: All stats update live during active raids
- **Grace Period Support**: Stats freeze during grace periods to prevent padding

### Data Persistence
- Automatic save/load of raid data across server restarts
- JSON-based storage for easy data management
- Intelligent grace period restoration on startup

### Interactive GUI
- Fully configurable interface via config.yml
- Display overall faction stats and individual player rankings
- Top player leaderboards for each stat category
- Clickable raid listings from commands

### Integration
- Seamless integration with FactionsKore
- Support for raiding outposts
- Custom event API for other plugins

## Commands

| Command | Aliases | Description | Usage |
|---------|---------|-------------|-------|
| `/viewraid` | `/vr`, `/raidstats` | View detailed raid statistics | `/viewraid <raidUUID>` |

### Auto-Display Commands
Configured commands (like `/f raid`) automatically display active raids instead of executing. Configure in `config.yml` under `valid-commands`.

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `factionskore.admin.raid-claim` | Bypass command interception and access staff features | OP |

### Available Placeholders

**Overall Stats:**
- `{defending_kills}`, `{defending_deaths}`, `{defending_blocks_placed}`
- `{defending_damage_dealt}`, `{defending_damage_taken}`
- `{defending_damage_dealt_hits}`, `{defending_damage_dealt_hearts}`
- `{attacking_*}` - Same as above for attacking faction

**Rank Placeholders:**
- `{rank}` - Player ranking
- `{player_name}` - Player name
- `{stat_value}` - Stat value
- `{color}` - Faction color
- `{hearts}` - Damage in hearts (damage stats only)
- `{hits}` - Number of hits (damage stats only)

## Events

### RaidStatsEndEvent
Fired when a raid ends and all stats are finalized (after grace period).

```java
@EventHandler
public void onRaidEnd(RaidStatsEndEvent event) {
    RaidObject raid = event.getRaidObject();
    // Access final raid statistics
}
```

## Dependencies

### Required
- **FactionsKore**: Raid timing and management
- **FactionsUUID**: Faction data
- **VulcanLib**: Utility library

## Installation

This plugin is free through the Vulcan Loader found in the client panel [Here](https://vulcandev.net/).

## How It Works

### Raid Lifecycle

1. **Raid Start**: When a raid begins, a RaidObject is created to track stats
2. **Active Tracking**: All player actions (kills, damage, blocks) are recorded
3. **Raid End**: Raid enters grace period where stats are frozen
4. **Grace Expiry**: RaidStatsEndEvent is fired, raid data is removed
5. **Persistence**: Active raids are saved on shutdown and restored on startup

### Grace Period
- Prevents stat padding after official raid end
- Configurable duration via FactionsKore
- Stats are frozen but viewable during grace
- Automatically schedules cleanup when grace expires

### Stat Categories
- **Kills/Deaths**: Tracked on player death events
- **Damage**: Tracked on entity damage events (converted to hearts for display)
- **Blocks Placed**: Tracked in enemy base regions and outposts
- **Hits**: Counted separately for damage dealt/taken

## Need Help?
If you have questions or need help, just message xanthard001 on Discord. I will be happy to help.