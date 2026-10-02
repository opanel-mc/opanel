use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

use pumpkin::server::Server;
use serde::Serialize;

const OVERWORLD_NAME: &str = "minecraft:overworld";
const NANOS_PER_MILLI: f64 = 1_000_000.0;

pub(crate) struct Uptimer {
    started_at: Instant,
}

impl Uptimer {
    pub(crate) fn new() -> Self {
        Self {
            started_at: Instant::now(),
        }
    }

    pub(crate) fn current(&self) -> u64 {
        duration_millis(self.started_at.elapsed())
    }
}

fn duration_millis(duration: Duration) -> u64 {
    duration.as_millis().try_into().unwrap_or(u64::MAX)
}

#[derive(Default)]
struct WorldTimeSnapshot {
    current: i64,
    do_daylight_cycle: bool,
    paused: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct IngameTime {
    pub(crate) current: i64,
    pub(crate) do_daylight_cycle: bool,
    pub(crate) paused: bool,
    pub(crate) mspt: f64,
}

impl IngameTime {
    pub(crate) fn from_server(server: &Server) -> Self {
        let worlds = server.worlds.load();
        let overworld = worlds
            .iter()
            .find(|world| world.dimension.minecraft_name == OVERWORLD_NAME)
            .or_else(|| worlds.first());
        let world_time = overworld.map(|world| {
            let do_daylight_cycle = world.level_info.load().game_rules.advance_time;
            let level_time = world
                .level_time
                .lock()
                .unwrap_or_else(std::sync::PoisonError::into_inner);

            WorldTimeSnapshot {
                current: level_time.time_of_day,
                do_daylight_cycle,
                paused: level_time.paused,
            }
        });

        Self::from_snapshot(
            world_time,
            server.tick_rate_manager.is_frozen(),
            server.get_mspt(),
            server.tick_rate_manager.nanoseconds_per_tick(),
        )
    }

    fn from_snapshot(
        world_time: Option<WorldTimeSnapshot>,
        server_paused: bool,
        measured_mspt: f64,
        nanoseconds_per_tick: i64,
    ) -> Self {
        let world_time = world_time.unwrap_or_default();
        let target_mspt = nanoseconds_per_tick as f64 / NANOS_PER_MILLI;

        Self {
            current: world_time.current,
            do_daylight_cycle: world_time.do_daylight_cycle,
            paused: server_paused || world_time.paused,
            // Pumpkin measures tick work only. The dashboard needs the complete tick period so
            // its client-side clock does not run too quickly while the server sleeps normally.
            mspt: measured_mspt.max(target_mspt),
        }
    }
}

pub(crate) fn unix_time_millis() -> u128 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_or(0, |duration| duration.as_millis())
}

pub(crate) fn game_tick_to_time(ticks: i64) -> String {
    let minutes = ticks.rem_euclid(24_000) * 60 / 1_000;
    format!("{:02}:{:02}", (minutes / 60 + 6) % 24, minutes % 60)
}

#[cfg(test)]
mod tests {
    use std::time::Duration;

    use serde_json::json;

    use super::{IngameTime, WorldTimeSnapshot, duration_millis};

    #[test]
    fn game_clock_never_displays_sixty_minutes() {
        for (tick, expected) in [
            (0, "06:00"),
            (999, "06:59"),
            (1000, "07:00"),
            (18000, "00:00"),
            (24000, "06:00"),
        ] {
            assert_eq!(super::game_tick_to_time(tick), expected);
        }
    }

    #[test]
    fn duration_millis_saturates_at_u64_max() {
        assert_eq!(duration_millis(Duration::from_millis(42)), 42);
        assert_eq!(duration_millis(Duration::MAX), u64::MAX);
    }

    #[test]
    fn ingame_time_uses_defaults_and_server_pause_without_a_world() {
        let time = IngameTime::from_snapshot(None, true, 5.0, 50_000_000);

        assert_eq!(
            serde_json::to_value(time).unwrap(),
            json!({
                "current": 0,
                "doDaylightCycle": false,
                "paused": true,
                "mspt": 50.0
            })
        );
    }

    #[test]
    fn ingame_time_combines_world_state_and_preserves_slower_ticks() {
        let time = IngameTime::from_snapshot(
            Some(WorldTimeSnapshot {
                current: 6_000,
                do_daylight_cycle: true,
                paused: true,
            }),
            false,
            75.0,
            50_000_000,
        );

        assert_eq!(time.current, 6_000);
        assert!(time.do_daylight_cycle);
        assert!(time.paused);
        assert_eq!(time.mspt, 75.0);
    }
}
