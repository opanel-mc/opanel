import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://ko.minecraft.wiki/w/게임_규칙?oldid=101258
 * @see https://ko.minecraft.wiki/w/게임_규칙?oldid=94920
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 */
export const wikiUrl = "https://ko.minecraft.wiki/w/게임_규칙";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "",
    description: "밤낮의 순환과 달의 위상 변화가 진행되는가?"
  },
  advance_weather: {
    name: "",
    description: "날씨가 자연적으로 바뀌는가? 비활성화되어도 /weather 명령어로 여전히 날씨를 바꿀 수 있다."
  },
  allow_entering_nether_using_portals: {
    name: "",
    description: "네더 차원문을 통해 네더에 들어갈 수 있는가?"
  },
  block_drops: {
    name: "",
    description: "블록이 아이템을 떨구는가?"
  },
  block_explosion_drop_decay: {
    name: "",
    description: "블록 폭발 (예: 오버월드가 아닌 차원에서 침대를 사용하는 것)로 인해 부서진 블록이 블록 노획물을 모두 떨구는가 (false), 아니면 폭발의 중심으로부터의 거리에 따라 무작위로 떨구는가 (true)?"
  },
  command_block_output: {
    name: "",
    description: "명령 블록이 명령을 수행할 때 관리자에게 알리는가?"
  },
  command_blocks_work: {
    name: "",
    description: "게임 내에서 명령 블록을 사용할 수 있는가?"
  },
  drowning_damage: {
    name: "",
    description: "플레이어가 익사 피해를 입는가?"
  },
  elytra_movement_check: {
    name: "",
    description: "플레이어가 겉날개를 착용하고 있을 때 서버가 플레이어 속도를 확인하는가? 멀티플레이에서 지연으로 인한 버벅임을 완화할 수 있다."
  },
  ender_pearls_vanish_on_death: {
    name: "",
    description: "플레이어가 죽을 때 던전 엔더 진주가 사라지는가?"
  },
  entity_drops: {
    name: "",
    description: "몹이 아닌 개체가 아이템을 떨구는가?"
  },
  fall_damage: {
    name: "",
    description: "플레이어가 낙하 피해를 입는가?."
  },
  fire_damage: {
    name: "",
    description: "플레이어가 불, 용암, 모닥불 및 마그마 블록에서 피해를 입는가?"
  },
  fire_spread_radius_around_player: {
    name: "",
    description: "플레이어를 중심으로 불이 확산될 수 있는 최대 거리. -1로 설정해서 무한한 거리로 설정할 수 있다."
  },
  forgive_dead_players: {
    name: "",
    description: "플레이어가 죽을 때 대상 플레이어를 중심으로 65 × 21 × 65 범위 내에 있는 화난 중립적 몹이 화를 멈추는가?"
  },
  freeze_damage: {
    name: "",
    description: "플레이어가 가루눈에서 피해를 입는가?"
  },
  global_sound_events: {
    name: "",
    description: "위더 생성 소리, 엔더 드래곤 죽음 소리와 엔드 차원문 활성화 소리가 위치에 관계 없이 모든 플레이어에게 들리는가?"
  },
  immediate_respawn: {
    name: "",
    description: "플레이어가 사망 화면을 보지 않고 즉시 리스폰하는가?"
  },
  keep_inventory: {
    name: "",
    description: "플레이어가 죽고 나서 보관함에 아이템과 경험치를 계속 가지는가?"
  },
  lava_source_conversion: {
    name: "",
    description: "새 용암 원천 블록이 만들어질 수 있는가?"
  },
  limited_crafting: {
    name: "",
    description: "플레이어가 잠금 해제한 제작법으로만 제작할 수 있는가?"
  },
  locator_bar: {
    name: "",
    description: "플레이어와 개체의 위치를 표시하는 플레이어의 위치 표시 바가 켜져 있는가?"
  },
  log_admin_commands: {
    name: "",
    description: "관리자 명령어가 서버 기록에 기록되는가?"
  },
  max_block_modifications: {
    name: "",
    description: "/clone, /fill 또는 /fillbiome을 사용해서 바꿀 수 있는 블록의 최대 가수."
  },
  max_command_forks: {
    name: "",
    description: "한 틱에서 만들 수 있는 갈림길 (맥락)의 최대 개수. 명령 블록과 함수에 적용된다."
  },
  max_command_sequence_length: {
    name: "",
    description: "한 틱에서 실행할 수 있는 명령어 연쇄의 최대 길이. 명령 블록과 함수에 적용된다."
  },
  max_entity_cramming: {
    name: "",
    description: "0.5초마다 6 ()만큼의 개체 중첩 피해를 입을 때까지 몹이나 플레이어가 밀 수 있는 개체의 최대 수. 0으로 설정하면 규칙이 비활성화된다. 피해는 서바이벌 또는 모험 모드에 있는 플레이어와 대부분의 몹 (박쥐 제외)에 적용된다. 밀 수 있는 개체에는 관전 모드에 있지 않은 플레이어, 박쥐가 아닌 모든 몹, 보트와 광산 수레가 포함된다."
  },
  max_minecart_speed: {
    name: "",
    description: "광산 수레가 도달할 수 있는 최고 속력."
  },
  max_snow_accumulation_height: {
    name: "",
    description: "각 블록에 쌓일 수 있는 눈 층의 최대 개수."
  },
  mob_drops: {
    name: "",
    description: "몹이 아이템과 경험 구슬을 떨구는가?"
  },
  mob_explosion_drop_decay: {
    name: "",
    description: "몹 폭발 (예: 크리퍼의 폭발)로 인해 부서진 블록이 블록 노획물을 모두 떨구는가 (false), 아니면 폭발의 중심으로부터의 거리에 따라 무작위로 떨구는가 (true)?"
  },
  mob_griefing: {
    name: "",
    description: "크리퍼, 좀비, 엔더맨, 가스트, 위더, 엔더 드래곤, 토끼, 양, 주민, 좀벌레, 눈 골렘 및 엔드 수정이 블록을 바꿀 수 있는가? 그리고 몹이 아이템을 주울 수 있는가? false로 설정되면, 피글린은 금 주괴를 줍지 않지만, 여전히 금 주괴를 피글린에 사용해서 물물교환할 수 있다. 유사하게, 주민은 음식 아이템을 줍지 않지만, 이미 보관함에 있는 음식을 다 사용할 때까지 계속 교배할 수 있다. 이는 좀비화 피글린이나 드라운드와 같은 좀비 종류의 몹이 거북알로 결오를 탐색할 수 있는지도 결정한다."
  },
  natural_health_regeneration: {
    name: "",
    description: "배고픔이 충분히 차 있으면 플레이어가 자연적으로 생명력을 재생할 수 있는가? 황금 사과, 재생 효과와 같은 외부적 생명력 회복에는 영향을 주지 않는다."
  },
  player_movement_check: {
    name: "",
    description: "서버가 플레이어의 속도를 확인하는가?"
  },
  players_nether_portal_creative_delay: {
    name: "",
    description: "크리에이티브 모드에 있는 플레이어가 차원을 이동하기 전에 네더 차원문 안에 머물러 있어야 할 시간."
  },
  players_nether_portal_default_delay: {
    name: "",
    description: "크리에이티브 모드에 있지 않은 플레이어가 차원을 이동하기 전에 네더 차원문 안에 머물러 있어야 할 시간."
  },
  players_sleeping_percentage: {
    name: "",
    description: "오버월드에 있는 플레이어 중 밤을 건너뛰기 위해 잠을 자야하는 플레이어의 백분율. 값이 0 이하면 한 명만 자도 밤을 건너뛸 수 있으며, 100 초과면 밤을 건너뛸 수 없다."
  },
  projectiles_can_break_blocks: {
    name: "",
    description: "발사체가 파괴할 수 있는 블록이 발사체에 의해 파괴되는가? 후렴화, 뾰족한 점적석 및 장식된 도자기가 여기에 포함된다."
  },
  pvp: {
    name: "",
    description: "플레이어가 다른 플레이어에게 피해를 입힐 수 있는가?"
  },
  raids: {
    name: "",
    description: "습격이 발생하는가?"
  },
  random_tick_speed: {
    name: "",
    description: "게임 틱 당 청크 부분 당 무작위 블록 틱 (식물 성장, 잎 시듦 등)이 발생하는 빈도. 0 이하의 값은 무작위 틱을 비활성화시키며, 숫자가 클수록 무작위 틱이 증가한다. 더 높은 정수값으로 설정하면 시듦과 성장 속도가 빨라진다. 이 값이 4096보다 크면 잎 시듦과 식물 성장이 즉각적으로 발생한다."
  },
  reduced_debug_info: {
    name: "",
    description: "디버그 화면이 축소된 정보를 보여주며, F3 + B (개체 히트박스) 및 F3 + G (청크 경계) 효과가 숨겨지는가?"
  },
  respawn_radius: {
    name: "",
    description: "플레이어가 서버에 처음으로 접속하거나 본인의 리스폰 지점이 없을 때 생성될 위치가 세계 생성 좌표로부터 떨어져 있을 수 있는 최대 블록 개수. 기본 게임 모드가 모험 모드인 서버에는 효과가 없다."
  },
  send_command_feedback: {
    name: "",
    description: "플레이어가 실행한 명령어의 피드백이 대화창에 나타나는가? 그리고 명령 블록이 기본적으로 출력 텍스트를 저장하는가?"
  },
  show_advancement_messages: {
    name: "",
    description: "발전 과제가 대화창에 공지되는가?"
  },
  show_death_messages: {
    name: "",
    description: "플레이어가 사망할 때 대화창에 사망 문구가 표시되는가? 그리고 애완동물이 죽을 때 주인에게 사망 문구가 표시되는가?"
  },
  spawn_mobs: {
    name: "",
    description: "몹이 자연적으로 생성되거나 고양이, 팬텀, 우민 정찰, 떠돌이 상인이나 좀비 전투와 같은 전역 생성 논리에 따라 생성되는가? 몬스터 생성기, 습격이나 철 골렘과 같은 특수 생성 시도에는 영향을 주지 않는다."
  },
  spawn_monsters: {
    name: "",
    description: "몬스터가 자연적으로 생성되는가?"
  },
  spawn_patrols: {
    name: "",
    description: "우민 정찰이 생성되는가?"
  },
  spawn_phantoms: {
    name: "",
    description: "밤에 팬텀이 생성되는가?"
  },
  spawn_wandering_traders: {
    name: "",
    description: "떠돌이 상인이 생성되는가?"
  },
  spawn_wardens: {
    name: "",
    description: "워든이 생성되는가?"
  },
  spawner_blocks_work: {
    name: "",
    description: "몬스터 생성기 블록이 작동하는가?"
  },
  spectators_generate_chunks: {
    name: "",
    description: "관전 모드의 플레이어가 청크를 불러올 수 있는가?"
  },
  spread_vines: {
    name: "",
    description: "덩굴이 다른 블록으로 퍼질 수 있는가? 동굴 덩굴, 늘어진 덩굴 및 휘어진 덩굴에는 영향을 주지 않는다."
  },
  tnt_explodes: {
    name: "",
    description: "TNT가 점화 후에 폭발하는가?"
  },
  tnt_explosion_drop_decay: {
    name: "",
    description: "TNT 폭발로 인해 부서진 블록이 블록 노획물을 모두 떨구는가 (false? 아니면 폭발의 중심으로부터의 거리에 따라 무작위로 떨구는가 (true)?"
  },
  universal_anger: {
    name: "",
    description: "화난 중립적 몹이 화나게 한 플레이어 뿐만 아니라 주변의 아무 플레이어를 공격하는가? forgiveDeadPlayers가 false일 때 가장 잘 작동한다."
  },
  water_source_conversion: {
    name: "",
    description: "새 물 원천 블록이 만들어질 수 있는가?"
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "",
    description: "불 및 용암 틱이 플레이어로부터 8청크 볌위 밖에서 발생할 수 있는가?"
  },
  allowEnteringNetherUsingPortals: {
    name: "",
    description: "네더 차원문을 통해 네더에 들어갈 수 있는가?"
  },
  announceAdvancements: {
    name: "",
    description: "발전 과제가 대화창에 공지되는가?"
  },
  blockExplosionDropDecay: {
    name: "",
    description: "블록 폭발 (예: 오버월드가 아닌 차원에서 침대를 사용하는 것)로 인해 부서진 블록이 블록 노획물을 모두 떨구는가 (false)? 아니면 폭발의 중심으로부터의 거리에 따라 무작위로 떨구는가 (true)?"
  },
  commandBlockOutput: {
    name: "",
    description: "명령 블록이 명령을 수행할 때 관리자에게 알리는가?"
  },
  commandBlocksEnabled: {
    name: "",
    description: "게임 내에서 명령 블록을 사용할 수 있는가?"
  },
  commandModificationBlockLimit: {
    name: "",
    description: "clone}}, /fill 또는 /fillbiome 명령어로 바꿀 수 있는 최대 블록 수."
  },
  disableElytraMovementCheck: {
    name: "",
    description: "플레이어가 겉날개를 착용하고 있을 때 서버가 플레이어 속도 확인을 건너뛰는가? 멀티플레이에서 지연으로 인한 버벅임을 완화할 수 있다."
  },
  disablePlayerMovementCheck: {
    name: "",
    description: "서버가 플레이어 속도 확인을 건너뛰는가?"
  },
  disableRaids: {
    name: "",
    description: "습격이 비활성화되어 있는가?"
  },
  doDaylightCycle: {
    name: "",
    description: "밤낮의 순환과 달의 위상 변화가 진행되는가?"
  },
  doEntityDrops: {
    name: "",
    description: "몹이 아닌 개체가 아이템을 떨구는가?"
  },
  doFireTick: {
    name: "",
    description: "불이 자연적으로 확산되고 꺼지는가?"
  },
  doImmediateRespawn: {
    name: "",
    description: "레이어가 사망 화면을 보지 않고 즉시 리스폰하는가?"
  },
  doInsomnia: {
    name: "",
    description: "밤에 팬텀이 생성되는가?"
  },
  doLimitedCrafting: {
    name: "",
    description: "플레이어가 잠금 해제한 제작법으로만 제작할 수 있는가?"
  },
  doMobLoot: {
    name: "",
    description: "몹이 아이템과 경험 구슬을 떨구는가?"
  },
  doMobSpawning: {
    name: "",
    description: "몹이 자연적으로 생성되거나 고양이, 팬텀, 우민 정찰, 떠돌이 상인이나 좀비 전투와 같은 전역 생성 논리에 따라 생성되는가? 몬스터 생성기, 습격이나 철 골렘과 같은 특수 생성 시도에는 영향을 주지 않는다."
  },
  doPatrolSpawning: {
    name: "",
    description: "우민 정찰이 생성되는가?"
  },
  doTileDrops: {
    name: "",
    description: "블록이 아이템을 떨구는가?"
  },
  doTraderSpawning: {
    name: "",
    description: "떠돌이 상인이 생성되는가?"
  },
  doVinesSpread: {
    name: "",
    description: "덩굴이 다른 블록으로 퍼질 수 있는가? 동굴 덩굴, 늘어진 덩굴 및 휘어진 덩굴에는 영향을 주지 않는다."
  },
  doWardenSpawning: {
    name: "",
    description: "워든이 생성되는가?"
  },
  doWeatherCycle: {
    name: "",
    description: "날씨가 자연적으로 바뀌는가?"
  },
  drowningDamage: {
    name: "",
    description: "플레이어가 익사 피해를 입는가?"
  },
  enderPearlsVanishOnDeath: {
    name: "",
    description: "플레이어가 죽을 때 던전 엔더 진주가 사라지는가?"
  },
  fallDamage: {
    name: "",
    description: "플레이어가 낙하 피해를 입는가?"
  },
  fireDamage: {
    name: "",
    description: "플레이어가 불, 용암, 모닥불 및 마그마 블록에서 피해를 입는가?"
  },
  forgiveDeadPlayers: {
    name: "",
    description: "플레이어가 죽을 때 대상 플레이어를 중심으로 65 × 21 × 65 범위 내에 있는 화난 중립적 몹이 화를 멈추는가?"
  },
  freezeDamage: {
    name: "",
    description: "플레이어가 가루눈에서 피해를 입는가?"
  },
  globalSoundEvents: {
    name: "",
    description: "위더 생성 소리, 엔더 드래곤 죽음 소리와 엔드 차원문 활성화 소리가 위치에 관계 없이 모든 플레이어에게 들리는가?"
  },
  keepInventory: {
    name: "",
    description: "플레이어가 죽고 나서 보관함에 아이템과 경험치를 계속 가지는가?"
  },
  lavaSourceConversion: {
    name: "",
    description: "새 용암 원천 블록이 만들어질 수 있는가?"
  },
  locatorBar: {
    name: "",
    description: "플레이어와 개체의 위치를 표시하는 플레이어의 위치 표시 바가 켜져 있는가?"
  },
  logAdminCommands: {
    name: "",
    description: "관리자 명령어가 서버 기록에 기록되는가?"
  },
  maxCommandChainLength: {
    name: "",
    description: "한 틱에서 실행할 수 있는 명령어 연쇄의 최대 길이. 명령 블록과 함수에 적용된다."
  },
  maxCommandForkCount: {
    name: "",
    description: "한 틱에서 만들 수 있는 갈림길 (맥락)의 최대 개수. 명령 블록과 함수에 적용된다."
  },
  maxEntityCramming: {
    name: "",
    description: "0.5초마다 6 ()만큼의 개체 중첩 피해를 입을 때까지 몹이나 플레이어가 밀 수 있는 개체의 최대 수. 0 이하로 설정하면 규칙이 비활성화된다. 피해는 서바이벌 또는 모험 모드에 있는 플레이어와 박쥐를 제외한 모든 몹에 적용된다. 밀 수 있는 개체에는 관전 모드에 있지 않은 플레이어, 박쥐가 아닌 모든 몹, 보트와 광산 수레가 포함된다."
  },
  minecartMaxSpeed: {
    name: "",
    description: "광산 수레가 도달할 수 있는 최고 속력."
  },
  mobExplosionDropDecay: {
    name: "",
    description: "몹 폭발 (예: 크리퍼의 폭발)로 인해 부서진 블록이 블록 노획물을 모두 떨구는가 (false)? 아니면 폭발의 중심으로부터의 거리에 따라 무작위로 떨구는가 (true)?"
  },
  mobGriefing: {
    name: "",
    description: "크리퍼, 좀비, 엔더맨, 가스트, 위더, 엔더 드래곤, 토끼, 양, 주민, 좀벌레, 눈 골렘 및 엔드 수정이 블록을 바꿀 수 있는가? 그리고 몹이 아이템을 주울 수 있는가? mobGriefing이 false로 설정되면, 피글린은 금 주괴를 줍지 않지만, 여전히 금 주괴를 피글린에 사용해서 물물교환할 수 있다. 유사하게, 주민은 음식 아이템을 줍지 않지만, 이미 보관함에 있는 음식을 다 사용할 때까지 계속 교배할 수 있다. 이는 좀비화 피글린이나 드라운드와 같은 좀비 종류의 몹이 거북알로 길을 찾을 수 있는지도 결정한다."
  },
  naturalRegeneration: {
    name: "",
    description: "배고픔이 충분히 차 있으면 플레이어가 자연적으로 생명력을 재생할 수 있는가? 황금 사과, 재생 효과와 같은 외부적 생명력 회복에는 영향을 주지 않는다."
  },
  playersNetherPortalCreativeDelay: {
    name: "",
    description: "크리에이티브 모드에 있는 플레이어가 차원을 이동하기 전에 네더 차원문 안에 머물러 있어야 할 시간."
  },
  playersNetherPortalDefaultDelay: {
    name: "",
    description: "크리에이티브 모드에 있지 않은 플레이어가 차원을 이동하기 전에 네더 차원문 안에 머물러 있어야 할 시간."
  },
  playersSleepingPercentage: {
    name: "",
    description: "오버월드에 있는 플레이어 중 밤을 건너뛰기 위해 잠을 자야하는 플레이어의 백분율. 값이 0 이하면 한 명만 자도 밤을 건너뛸 수 있으며, 100 초과면 밤을 건너뛸 수 없다."
  },
  projectilesCanBreakBlocks: {
    name: "",
    description: "발사체가 파괴할 수 있는 블록이 발사체에 의해 파괴되는가? 후렴화, 뾰족한 점적석 및 장식된 도자기가 여기에 포함된다."
  },
  randomTickSpeed: {
    name: "",
    description: "게임 틱 당 청크 부분 당 무작위 블록 틱 (식물 성장, 잎 시듦 등)이 발생하는 빈도. 0 이하의 값은 무작위 틱을 비활성화시키며, 숫자가 클수록 무작위 틱이 증가한다. 더 높은 정수값으로 설정하면 시듦과 성장 속도가 빨라진다. 이 값이 4096보다 크면 잎 시듦과 식물 성장이 즉각적으로 발생한다."
  },
  reducedDebugInfo: {
    name: "",
    description: "디버그 화면이 모든 정보를 보여주는가 (false)? 아니면 축소된 정보를 보여주는가 (true)? 그리고 F3 + B (개체 히트박스) 및 F3 + G (청크 경계) 효과가 숨겨지는가?"
  },
  sendCommandFeedback: {
    name: "",
    description: "플레이어가 실행한 명령어의 피드백이 대화창에 나타나는가? 그리고 명령 블록이 기본적으로 출력 텍스트를 저장하는가?"
  },
  showDeathMessages: {
    name: "",
    description: "플레이어가 사망할 때 대화창에 사망 문구가 표시되는가? 그리고 애완동물이 죽을 때 주인에게 사망 문구가 표시되는가?"
  },
  snowAccumulationHeight: {
    name: "",
    description: "각 블록에 쌓일 수 있는 눈 층의 최대 개수."
  },
  spawnChunkRadius: {
    name: "",
    description: "세계의 생성 청크 크기."
  },
  spawnMonsters: {
    name: "",
    description: "몬스터가 자연적으로 생성되는가?"
  },
  spawnerBlocksEnabled: {
    name: "",
    description: "몬스터 생성기 블록이 작동하는가?"
  },
  spawnRadius: {
    name: "",
    description: "플레이어가 서버에 처음으로 접속하거나 본인의 리스폰 지점이 없을 때 생성될 위치가 세계 생성 좌표로부터 떨어져 있을 수 있는 최대 블록 개수. 기본 게임 모드가 모험 모드인 서버에는 효과가 없다."
  },
  spectatorsGenerateChunks: {
    name: "",
    description: "관전 모드의 플레이어가 청크를 불러올 수 있는가?"
  },
  tntExplodes: {
    name: "",
    description: "TNT가 점화 후에 폭발하는가?"
  },
  tntExplosionDropDecay: {
    name: "",
    description: "TNT 폭발로 인해 부서진 블록이 블록 노획물을 모두 떨구는가 (false)? 아니면 폭발의 중심으로부터의 거리에 따라 무작위로 떨구는가 (true)?"
  },
  universalAnger: {
    name: "",
    description: "화난 중립적 몹이 화나게 한 플레이어 뿐만 아니라 주변의 아무 플레이어를 공격하는가? forgiveDeadPlayers가 false일 때 가장 잘 작동한다."
  },
  waterSourceConversion: {
    name: "",
    description: "새 물 원천 블록이 만들어질 수 있는가?"
  },
};

export default gamerulePresets;
