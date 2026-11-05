import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://ja.minecraft.wiki/w/ゲームルール?oldid=401404
 * @see https://ja.minecraft.wiki/w/ゲームルール?oldid=365158
 * @see https://ja.minecraft.wiki/w/ゲームルール?oldid=351869
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 */
export const wikiUrl = "https://ja.minecraft.wiki/w/ゲームルール";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "ゲーム内時刻の進行",
    description: "時刻が進行するどうか。"
  },
  advance_weather: {
    name: "天気の更新",
    description: "天候が自然に変動するかどうか。/weatherコマンドによる変更は可能。"
  },
  allow_entering_nether_using_portals: {
    name: "ネザーの許可",
    description: "ネザーポータルでオーバーワールドからネザーへ移動できるかどうか（逆は常に可）。"
  },
  block_drops: {
    name: "ブロックのドロップ",
    description: "ブロックが破壊されたときにアイテムをドロップするかどうか。"
  },
  block_explosion_drop_decay: {
    name: "ブロックの操作による爆発時にブロックがドロップアイテムを落とさない可能性",
    description: "ブロックが、他のブロック由来（ベッドなど。TNTを除く）の爆発によって破壊されたときのドロップアイテムの消失の仕様を以下のうちから設定する。"
  },
  command_block_output: {
    name: "コマンドブロックの実行結果の通知",
    description: "コマンドブロックによるコマンドの実行ログを管理者に通知するかどうか。"
  },
  command_blocks_work: {
    name: "コマンドブロックの有効化",
    description: "コマンドブロックが使えるかどうか。"
  },
  drowning_damage: {
    name: "溺水ダメージ",
    description: "プレイヤーが溺れているときにダメージを受けるかどうか。"
  },
  elytra_movement_check: {
    name: "エリトラの速度チェック",
    description: "エリトラを装備しているプレイヤーの速度チェックを行うかどうか。オフにすると、マルチプレイにおけるラグによるプレイヤーの不規則な移動の誤検知を解消できることがある。"
  },
  ender_pearls_vanish_on_death: {
    name: "プレイヤー死亡時のエンダーパールの消滅",
    description: "プレイヤーの投げたエンダーパールが、投げたプレイヤーが死亡したときに消滅するかどうか。"
  },
  entity_drops: {
    name: "設備エンティティのドロップ",
    description: "ボートやトロッコなどの非Mobエンティティが破壊されたときにアイテムをドロップするかどうか。"
  },
  fall_damage: {
    name: "落下ダメージ",
    description: "プレイヤーが落下ダメージを受けるかどうか。"
  },
  fire_damage: {
    name: "火傷ダメージ",
    description: "プレイヤーが炎や溶岩の中、焚き火やマグマブロックの上で炎上ダメージを受けるかどうか。"
  },
  fire_spread_radius_around_player: {
    name: "炎の延焼が起こる半径",
    description: "チャンク内で炎の延焼処理が起こるための、チャンクの中心とプレイヤーとの距離上限。-1では読み込まれているすべてのチャンクで処理が起き、0では延焼は起きない。"
  },
  forgive_dead_players: {
    name: "死亡したプレイヤーへの敵対を解除",
    description: "敵対した中立的Mobの近くで敵対対象のプレイヤーが死亡したとき、そのMobの敵対状態を解除するかどうか。"
  },
  freeze_damage: {
    name: "凍結ダメージ",
    description: "プレイヤーが粉雪に埋もれたときに凍結ダメージを受けるかどうか。"
  },
  global_sound_events: {
    name: "ワールド全体のサウンドイベント",
    description: "ボスの出現など、特定のサウンドイベントが発生したときのサウンドが場所に関わらず聞こえるかどうか。"
  },
  immediate_respawn: {
    name: "即時リスポーン",
    description: "死亡画面を表示せずに即座にリスポーンするかどうか。"
  },
  keep_inventory: {
    name: "死亡時のインベントリの保持",
    description: "プレイヤーの死亡時にインベントリ内のアイテムや経験値を保持するかどうか。"
  },
  lava_source_conversion: {
    name: "溶岩流から溶岩源への変化",
    description: "溶岩源同士に挟まれた溶岩流が、溶岩源に変化するかどうか。"
  },
  limited_crafting: {
    name: "クラフトにレシピを要求",
    description: "プレイヤーが既にロックを解除したレシピのみを作成できるようにするかどうか。"
  },
  locator_bar: {
    name: "プレイヤーの方向バーの有効化",
    description: "方向バーを有効にするかどうか。"
  },
  log_admin_commands: {
    name: "管理者ログへのコマンドの実行結果の通知",
    description: "サーバーログに管理コマンドを記録するかどうか。"
  },
  max_block_modifications: {
    name: "コマンドによるブロックの最大変更数",
    description: "/clone、/fill、/fillbiomeなどのコマンド一回の実行で編集できるブロックの最大数。"
  },
  max_command_forks: {
    name: "コマンドの最大実行文脈数",
    description: "1ティックの間に実行されるコマンドの「実行文脈」数の上限。"
  },
  max_command_sequence_length: {
    name: "コマンドの最大チェーン数",
    description: "1ティックの間に実行されるコマンド列の最大長。チェーンコマンドブロック・関数・チャット欄でのコマンドの実行に適用される。詳細はこちらも参照。"
  },
  max_entity_cramming: {
    name: "エンティティの最大密集数",
    description: "プレイヤーやMobが重なり合えるエンティティの最大数。これを超えると0.5秒ごとに3の窒息ダメージを受ける。0にすることで無効化できる。このダメージはサバイバルモードもしくはアドベンチャーモードのプレイヤー、そしてコウモリ以外のすべてのMobが受ける。重なり合えるエンティティにはスペクテイターモードでないプレイヤー、コウモリ以外のMob、さらにボートとトロッコが含まれる。"
  },
  max_minecart_speed: {
    name: "トロッコの最高速度",
    description: "トロッコの最高速度を、8-1000ブロック/秒の間で設定する。"
  },
  max_snow_accumulation_height: {
    name: "積雪の深さ",
    description: "降雪時に雪が積もる層の最大数。"
  },
  mob_drops: {
    name: "Mobのアイテムのドロップ",
    description: "Mobが倒されたときにアイテムと経験値オーブをドロップするかどうか。"
  },
  mob_explosion_drop_decay: {
    name: "Mob爆発時にブロックがドロップアイテムを落とさない可能性",
    description: "ブロックが、Mob由来（クリーパーなど）の爆発によって破壊されたときのドロップアイテムの消失の仕様を以下のうちから設定する。"
  },
  mob_griefing: {
    name: "Mobの破壊行動の許可",
    description: "クリーパー、ゾンビ、エンダーマン、ガスト、ウィザー、エンダードラゴン、ウサギ、ヒツジ、キツネ、村人、シルバーフィッシュ、スノウゴーレム、エンドクリスタルがブロックに変更を加えられるかどうか、およびMobがアイテムを拾えるかどうか。"
  },
  natural_health_regeneration: {
    name: "体力の自動回復",
    description: "プレイヤーの満腹度が十分にあるとき、体力を自然回復するかどうか。金のリンゴや再生能力等のようなその他の回復には影響しない。"
  },
  player_movement_check: {
    name: "プレイヤーの速度チェック",
    description: "エリトラ装備時以外を含めた、プレイヤーの速度チェック全般を行うかどうか。主に、マルチプレイにおけるラグによるプレイヤーの不規則な移動の誤検知解消に役立つ。"
  },
  players_nether_portal_creative_delay: {
    name: "クリエイティブモードでのネザーポータルの待機時間",
    description: "クリエイティブモードのプレイヤーがネザーポータルの中に立って何ティック経ったらディメンションを移動するか。"
  },
  players_nether_portal_default_delay: {
    name: "非クリエイティブモードでのネザーポータルの待機時間",
    description: "クリエイティブモード以外のプレイヤーがネザーポータルの中に立って何ティック経ったらディメンションを移動するか。"
  },
  players_sleeping_percentage: {
    name: "就寝率",
    description: "マルチプレイヤー時に夜を過ごすために寝る必要があるプレイヤーの割合（%）。オーバーワールドにいないプレイヤーはいないものとして計算される。0以下だと誰か1人でも就寝すれば夜を過ごせるようになり、100より大きい数字だと全員寝ても夜を過ごせなくなる。"
  },
  projectiles_can_break_blocks: {
    name: "飛び道具によるブロック破壊の可否",
    description: "飛び道具の着弾によって破壊されうるブロック（コーラスフラワーなど）が実際に破壊されるかどうかを設定できる。"
  },
  pvp: {
    name: "PvPの有効化",
    description: "他のプレイヤーにダメージを与えられるかどうか。"
  },
  raids: {
    name: "襲撃の発生",
    description: "襲撃を行うかどうか。"
  },
  random_tick_speed: {
    name: "ランダムティックの発生数",
    description: "植物の成長や葉の枯死などを引き起こすランダムティックが1ティックにつき1セクション（チャンクを更に垂直方向にも16ブロックずつ区切った立方体領域）あたり何回発生するか。ランダムティックは0以下の数値で無効化され、数値が大きいほどランダムティックが増加する。つまり、大きい数値を設定すれば、成長と枯死が早くなる。4096以上の値では成長と枯死が範囲内のすべてのブロックで毎ティック発生する。"
  },
  reduced_debug_info: {
    name: "デバッグ情報の制限",
    description: "デバッグ画面の情報を一部制限するかどうか。制限内容は本記事を参照。"
  },
  respawn_radius: {
    name: "リスポーン地点の半径",
    description: "プレイヤーの初期スポーン地点の範囲を、ワールドのスポーン地点からのブロック半径で指定する。"
  },
  send_command_feedback: {
    name: "コマンドの実行結果の表示",
    description: "プレイヤーが実行したコマンドのログをチャット欄に表示するかどうか。コマンドブロックがデフォルトでコマンドの出力を保存するかどうかにも影響する。"
  },
  show_advancement_messages: {
    name: "進捗達成の通知",
    description: "進捗の達成をチャット欄で知らせるかどうか。"
  },
  show_death_messages: {
    name: "死亡メッセージの表示",
    description: "プレイヤーの死亡時にチャット欄に死亡メッセージを表示するかどうか。ペットの死亡メッセージを飼い主に表示するかにも影響する。"
  },
  spawn_mobs: {
    name: "Mobのスポーン",
    description: "Mobが自然にスポーンするかどうか。モンスタースポナーからのスポーンには影響しない。"
  },
  spawn_monsters: {
    name: "モンスターのスポーン",
    description: "モンスターがスポーンするかどうか。"
  },
  spawn_patrols: {
    name: "略奪隊のスポーン",
    description: "略奪隊がスポーンするかどうか"
  },
  spawn_phantoms: {
    name: "ファントムのスポーン",
    description: "夜間にファントムがスポーンするかどうか。"
  },
  spawn_wandering_traders: {
    name: "行商人のスポーン",
    description: "行商人がスポーンするかどうか。"
  },
  spawn_wardens: {
    name: "ウォーデンのスポーン",
    description: "ウォーデンがスポーンするかどうか。"
  },
  spawner_blocks_work: {
    name: "スポナーの有効化",
    description: "モンスタースポナー類が有効かどうか。"
  },
  spectators_generate_chunks: {
    name: "スペクテイターモードのプレイヤーによる地形生成の許可",
    description: "スペクテイターモードのプレイヤーの周囲においてチャンク生成を行うかどうか。"
  },
  spread_vines: {
    name: "ツタの成長",
    description: "ツタが周囲のブロックへ伝播するかどうか。"
  },
  tnt_explodes: {
    name: "TNTの点火及び爆発の許可",
    description: "TNTが着火して爆発するかどうか。"
  },
  tnt_explosion_drop_decay: {
    name: "TNT爆発時にブロックがドロップアイテムを落とさない可能性",
    description: "ブロックが、TNTの爆発によって破壊されたときのドロップアイテムの消失の仕様を以下のうちから設定する。"
  },
  universal_anger: {
    name: "敵対の共有",
    description: "中立的Mobに攻撃を加えたプレイヤーだけでなく、近くの全てのプレイヤーを敵対の対象とするかどうか。forgiveDeadPlayersを無効にすると相性が良い。"
  },
  water_source_conversion: {
    name: "水流から水源への変化",
    description: "水源に挟まれた水流が水源に変換されるかどうか。"
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "プレイヤーから遠い炎のティック処理",
    description: "中心がプレイヤーから水平方向に128ブロック以遠のチャンクでも、炎と溶岩のティック処理が行われるかどうか。"
  },
  allowEnteringNetherUsingPortals: {
    name: "ネザーの許可",
    description: "ネザーポータルでオーバーワールドからネザーへ移動できるかどうか（逆は常に可）。"
  },
  announceAdvancements: {
    name: "進捗達成の通知",
    description: "進捗の達成をチャット欄で知らせるかどうか。"
  },
  blockExplosionDropDecay: {
    name: "ブロックの操作による爆発時にブロックがドロップアイテムを落とさない可能性",
    description: "ブロックが、他のブロック由来（ベッドなど。TNTを除く）の爆発によって破壊されたときのドロップアイテムの消失の仕様を以下のうちから設定する。"
  },
  commandBlockOutput: {
    name: "コマンドブロックの実行結果の通知",
    description: "コマンドブロックによるコマンドの実行ログを管理者に通知するかどうか。"
  },
  commandBlocksEnabled: {
    name: "コマンドブロックの有効化",
    description: "コマンドブロックが使えるかどうか。"
  },
  commandModificationBlockLimit: {
    name: "コマンドによるブロックの最大変更数",
    description: "/clone、/fill、/fillbiomeなどのコマンド一回の実行で編集できるブロックの最大数。"
  },
  disableElytraMovementCheck: {
    name: "エリトラの速度チェックの無効化",
    description: "エリトラを装備しているプレイヤーの速度チェックを無効にするかどうか。主に、マルチプレイにおけるラグによるプレイヤーの不規則な移動の誤検知解消に役立つ。"
  },
  disablePlayerMovementCheck: {
    name: "プレイヤーの速度チェックの無効化",
    description: "エリトラ装備時以外含め、常にプレイヤーの速度チェックを無効にするかどうか。主に、マルチプレイにおけるラグによるプレイヤーの不規則な移動の誤検知解消に役立つ。"
  },
  disableRaids: {
    name: "襲撃の無効化",
    description: "襲撃を無効にするかどうか。"
  },
  doDaylightCycle: {
    name: "ゲーム内時刻の進行",
    description: "昼夜サイクルと月の満ち欠けが進むかどうか。"
  },
  doEntityDrops: {
    name: "設備エンティティのドロップ",
    description: "ボートやトロッコなどの非Mobエンティティが破壊されたときにアイテムをドロップするかどうか。"
  },
  doFireTick: {
    name: "炎の更新",
    description: "炎が燃え広がり、自然に消火するかどうか。"
  },
  doImmediateRespawn: {
    name: "即時リスポーン",
    description: "死亡画面を表示せずに即座にリスポーンするかどうか。"
  },
  doInsomnia: {
    name: "ファントムのスポーン",
    description: "夜間にファントムがスポーンするかどうか。"
  },
  doLimitedCrafting: {
    name: "クラフトにレシピを要求",
    description: "プレイヤーが既にロックを解除したレシピのみを作成できるようにするかどうか。"
  },
  doMobLoot: {
    name: "Mobのアイテムのドロップ",
    description: "Mobが倒されたときにアイテムと経験値オーブをドロップするかどうか。"
  },
  doMobSpawning: {
    name: "Mobのスポーン",
    description: "Mobが自然にスポーンするかどうか。モンスタースポナーからのスポーンには影響しない。"
  },
  doPatrolSpawning: {
    name: "略奪隊のスポーン",
    description: "略奪隊がスポーンするかどうか"
  },
  doTileDrops: {
    name: "ブロックのドロップ",
    description: "ブロックが破壊されたときにアイテムをドロップするかどうか。"
  },
  doTraderSpawning: {
    name: "行商人のスポーン",
    description: "行商人がスポーンするかどうか。"
  },
  doVinesSpread: {
    name: "ツタの成長",
    description: "ツタが周囲のブロックへ伝播するかどうか。"
  },
  doWardenSpawning: {
    name: "ウォーデンのスポーン",
    description: "ウォーデンがスポーンするかどうか。"
  },
  doWeatherCycle: {
    name: "天気の更新",
    description: "天候が自然に変動するかどうか。/weatherコマンドによる変更は可能。"
  },
  drowningDamage: {
    name: "溺水ダメージ",
    description: "プレイヤーが溺れているときにダメージを受けるかどうか。"
  },
  enderPearlsVanishOnDeath: {
    name: "プレイヤー死亡時のエンダーパールの消滅",
    description: "プレイヤーの投げたエンダーパールが、投げたプレイヤーが死亡したときに消滅するかどうか。"
  },
  fallDamage: {
    name: "落下ダメージ",
    description: "プレイヤーが落下ダメージを受けるかどうか。"
  },
  fireDamage: {
    name: "火傷ダメージ",
    description: "プレイヤーが炎や溶岩の中、焚き火やマグマブロックの上で炎上ダメージを受けるかどうか。"
  },
  forgiveDeadPlayers: {
    name: "死亡したプレイヤーへの敵対を解除",
    description: "敵対した中立的Mobの近くで敵対対象のプレイヤーが死亡したとき、そのMobの敵対状態を解除するかどうか。"
  },
  freezeDamage: {
    name: "凍結ダメージ",
    description: "プレイヤーが粉雪に埋もれたときに凍結ダメージを受けるかどうか。"
  },
  globalSoundEvents: {
    name: "ワールド全体のサウンドイベント",
    description: "ボスの出現など、特定のサウンドイベントが発生したときのサウンドが場所に関わらず聞こえるかどうか。"
  },
  keepInventory: {
    name: "死亡時のインベントリの保持",
    description: "プレイヤーの死亡時にインベントリ内のアイテムや経験値を保持するかどうか。"
  },
  lavaSourceConversion: {
    name: "溶岩流から溶岩源への変化",
    description: "溶岩源同士に挟まれた溶岩流が、溶岩源に変化するかどうか。"
  },
  locatorBar: {
    name: "プレイヤーの方向バーの有効化",
    description: "方向バーを有効にするかどうか。"
  },
  logAdminCommands: {
    name: "管理者ログへのコマンドの実行結果の通知",
    description: "サーバーログに管理コマンドを記録するかどうか。"
  },
  maxCommandChainLength: {
    name: "コマンドの最大チェーン数",
    description: "1ティックの間に実行されるコマンド列の最大長。チェーンコマンドブロック・関数・チャット欄でのコマンドの実行に適用される。詳細はこちらも参照。"
  },
  maxCommandForkCount: {
    name: "コマンドの最大実行文脈数",
    description: "1ティックの間に実行されるコマンドの「実行文脈」数の上限。"
  },
  maxEntityCramming: {
    name: "エンティティの最大密集数",
    description: "プレイヤーやMobが重なり合えるエンティティの最大数。これを超えると0.5秒ごとに3の窒息ダメージを受ける。0にすることで無効化できる。このダメージはサバイバルモードもしくはアドベンチャーモードのプレイヤー、そしてコウモリ以外のすべてのMobが受ける。重なり合えるエンティティにはスペクテイターモードでないプレイヤー、コウモリ以外のMob、さらにボートとトロッコが含まれる。"
  },
  minecartMaxSpeed: {
    name: "トロッコの最高速度",
    description: "トロッコの最高速度を、8-1000ブロック/秒の間で設定する。"
  },
  mobExplosionDropDecay: {
    name: "Mob爆発時にブロックがドロップアイテムを落とさない可能性",
    description: "ブロックが、Mob由来（クリーパーなど）の爆発によって破壊されたときのドロップアイテムの消失の仕様を以下のうちから設定する。"
  },
  mobGriefing: {
    name: "Mobの破壊行動の許可",
    description: "クリーパー、ゾンビ、エンダーマン、ガスト、ウィザー、エンダードラゴン、ウサギ、ヒツジ、キツネ、村人、シルバーフィッシュ、スノウゴーレム、エンドクリスタルがブロックに変更を加えられるかどうか、およびMobがアイテムを拾えるかどうか。"
  },
  naturalRegeneration: {
    name: "体力の自動回復",
    description: "プレイヤーの満腹度が十分にあるとき、体力を自然回復するかどうか。金のリンゴや再生能力等のようなその他の回復には影響しない。"
  },
  playersNetherPortalCreativeDelay: {
    name: "クリエイティブモードでのネザーポータルの待機時間",
    description: "クリエイティブモードのプレイヤーがネザーポータルの中に立って何ティック経ったらディメンションを移動するか。"
  },
  playersNetherPortalDefaultDelay: {
    name: "非クリエイティブモードでのネザーポータルの待機時間",
    description: "クリエイティブモード以外のプレイヤーがネザーポータルの中に立って何ティック経ったらディメンションを移動するか。"
  },
  playersSleepingPercentage: {
    name: "就寝率",
    description: "マルチプレイヤー時に夜を過ごすために寝る必要があるプレイヤーの割合（%）。オーバーワールドにいないプレイヤーはいないものとして計算される。0以下だと誰か1人でも就寝すれば夜を過ごせるようになり、100より大きい数字だと全員寝ても夜を過ごせなくなる。"
  },
  projectilesCanBreakBlocks: {
    name: "飛び道具によるブロック破壊の可否",
    description: "飛び道具の着弾によって破壊されうるブロック（コーラスフラワーなど）が実際に破壊されるかどうかを設定できる。"
  },
  randomTickSpeed: {
    name: "ランダムティックの発生数",
    description: "植物の成長や葉の枯死などを引き起こすランダムティックが1ティックにつき1セクション（チャンクを更に垂直方向にも16ブロックずつ区切った立方体領域）あたり何回発生するか。ランダムティックは0以下の数値で無効化され、数値が大きいほどランダムティックが増加する。つまり、大きい数値を設定すれば、成長と枯死が早くなる。4096以上の値では成長と枯死が範囲内のすべてのブロックで毎ティック発生する。"
  },
  reducedDebugInfo: {
    name: "デバッグ情報の制限",
    description: "デバッグ画面ですべての情報を表示するか、一部の情報を表示するか。およびF3 + BやF3 + Gを入力したときの、エンティティの当たり判定やチャンク境界を表示するかどうか。"
  },
  sendCommandFeedback: {
    name: "コマンドの実行結果の表示",
    description: "プレイヤーが実行したコマンドのログをチャット欄に表示するかどうか。コマンドブロックがデフォルトでコマンドの出力を保存するかどうかにも影響する。"
  },
  showDeathMessages: {
    name: "死亡メッセージの表示",
    description: "プレイヤーの死亡時にチャット欄に死亡メッセージを表示するかどうか。ペットの死亡メッセージを飼い主に表示するかにも影響する。"
  },
  snowAccumulationHeight: {
    name: "積雪の深さ",
    description: "降雪時に雪が積もる層の最大数。0～8で指定。"
  },
  spawnChunkRadius: {
    name: "スポーンチャンクの半径",
    description: "スポーンチャンクの範囲を、ワールドのスポーン地点があるチャンクからのチャンク半径で指定する。範囲の大きさは指定値 × 2 + 1チャンク四方になる。"
  },
  spawnMonsters: {
    name: "モンスターのスポーン",
    description: "モンスターがスポーンするかどうか。"
  },
  spawnerBlocksEnabled: {
    name: "スポナーの有効化",
    description: "モンスタースポナー類が有効かどうか。"
  },
  spawnRadius: {
    name: "リスポーン地点の半径",
    description: "プレイヤーの初期スポーン地点の範囲を、ワールドのスポーン地点からのブロック半径で指定する。"
  },
  spectatorsGenerateChunks: {
    name: "スペクテイターモードのプレイヤーによる地形生成の許可",
    description: "スペクテイターモードのプレイヤーの周囲においてチャンク生成を行うかどうか。"
  },
  tntExplodes: {
    name: "TNTの点火及び爆発の許可",
    description: "TNTが着火して爆発するかどうか。"
  },
  tntExplosionDropDecay: {
    name: "TNT爆発時にブロックがドロップアイテムを落とさない可能性",
    description: "ブロックが、TNTの爆発によって破壊されたときのドロップアイテムの消失の仕様を以下のうちから設定する。"
  },
  universalAnger: {
    name: "敵対の共有",
    description: "中立的Mobに攻撃を加えたプレイヤーだけでなく、近くの全てのプレイヤーを敵対の対象とするかどうか。forgiveDeadPlayersを無効にすると相性が良い。"
  },
  waterSourceConversion: {
    name: "水流から水源への変化",
    description: "水源に挟まれた水流が水源に変換されるかどうか。"
  },
};

export default gamerulePresets;
