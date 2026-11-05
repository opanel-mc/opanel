import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://de.minecraft.wiki/w/Spielregel?oldid=742641
 * @see https://de.minecraft.wiki/w/Spielregel?oldid=707218
 * @see https://de.minecraft.wiki/w/Spielregel?oldid=702245
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 */
export const wikiUrl = "https://de.minecraft.wiki/w/Spielregel";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "",
    description: "Ob die Phasen Tag-Nacht-Rhythmus und Mondphasen fortschreiten."
  },
  advance_weather: {
    name: "",
    description: "Ob sich das Wetter sich natürlich ändern kann. Der Befehl Befehl /weather kann das Wetter immer noch ändern."
  },
  allow_entering_nether_using_portals: {
    name: "",
    description: "Ob Spieler und Kreaturen durch Netherportale reisen können."
  },
  block_drops: {
    name: "",
    description: "Ob Blöcke droppen sollen."
  },
  block_explosion_drop_decay: {
    name: "",
    description: "Ob Blockbeute von allen Blöcken (false) oder zufällig (true) fallen gelassen wird, abhängig davon, wie weit der Block vom Zentrum einer Blockexplosion entfernt ist (z. B. Klicken auf ein Bett in anderen Dimensionen). als die Oberwelt)."
  },
  command_block_output: {
    name: "",
    description: "Ob Befehlsblöcke Administratoren benachrichtigen sollen, wenn sie Befehle ausführen."
  },
  command_blocks_work: {
    name: "",
    description: "Ob Befehlsblöcke im Spiel aktiviert werden sollen."
  },
  drowning_damage: {
    name: "",
    description: "Ob der Spieler beim Ertrinken Schaden erleiden soll."
  },
  // The Wiki still describes the inverse legacy rule here.
  elytra_movement_check: {
    name: "",
    description: ""
  },
  ender_pearls_vanish_on_death: {
    name: "",
    description: "Steuert, ob geworfene Enderperlen verschwinden, wenn der Spieler stirbt."
  },
  entity_drops: {
    name: "",
    description: "Ob Objekte, die keine Kreaturen sind, Drops haben sollen."
  },
  fall_damage: {
    name: "",
    description: "Ob der Spieler Sturzschaden erleiden soll."
  },
  fire_damage: {
    name: "",
    description: "Ob der Spieler Schaden in Feuer, Lava, Lagerfeuern oder auf Magmablöcken erleiden soll."
  },
  fire_spread_radius_around_player: {
    name: "",
    description: "Steuert die maximale Reichweite in Blöcken, die Feuer um einen Spieler herum haben kann."
  },
  forgive_dead_players: {
    name: "",
    description: "Verärgerte neutrale Kreaturen hören auf, wütend zu sein, wenn der Zielspieler in der Nähe stirbt."
  },
  freeze_damage: {
    name: "",
    description: "Ob der Spieler Schaden erleiden soll, wenn er sich im Pulverschnee befindet."
  },
  global_sound_events: {
    name: "",
    description: "Ob bestimmte Geräuschereignisse von allen Spielern gehört werden, unabhängig vom Standort."
  },
  immediate_respawn: {
    name: "",
    description: "Spieler erscheinen sofort wieder, ohne dass der Todesbildschirm angezeigt wird."
  },
  keep_inventory: {
    name: "",
    description: "Ob der Spieler Gegenstände und Erfahrung nach dem Tod in seinem Inventar behalten soll."
  },
  lava_source_conversion: {
    name: "",
    description: "Ob sich neue Lavaquellen bilden dürfen."
  },
  limited_crafting: {
    name: "",
    description: "Ob Spieler nur die Rezepte herstellen können, die sie freigeschaltet haben."
  },
  locator_bar: {
    name: "",
    description: "Ob die Ortungsleiste des Spielers aktiviert ist und die Richtungen von Spielern oder Kreaturen anzeigt."
  },
  log_admin_commands: {
    name: "",
    description: "Ob Administratorbefehle im Serverprotokoll protokolliert werden sollen."
  },
  max_block_modifications: {
    name: "",
    description: "Steuert die maximale Anzahl der Blöcke, die bei der Verwendung von Befehl /clone, Befehl /fill oder Befehl /fillbiome geändert werden."
  },
  max_command_forks: {
    name: "",
    description: "Die maximale Anzahl an Abspaltungen (Kontexten), die während eines Ticks erstellt werden können. Gilt für Befehlsblöcke und Funktionen."
  },
  max_command_sequence_length: {
    name: "",
    description: "Die maximale Länge einer Befehlskette, die während eines Ticks ausgeführt werden kann. Gilt für Befehlsblöcke und Funktionen."
  },
  max_entity_cramming: {
    name: "",
    description: "Die maximale Anzahl schiebbarer Kreaturen, die eine Kreatur oder Spieler schieben kann, bevor er pro halbe Sekunde 6 () Erstickungsschaden erleidet. Bei einer Einstellung auf 0 oder niedriger wird die Regel deaktiviert. Der Schaden betrifft Spieler im Überlebens- oder Abenteuermodus und alle Kreaturen außer Fledermäusen. Zu den schiebbaren Kreaturen gehören Spieler, die nicht im Zuschauermodus sind, alle Kreaturen außer Fledermäusen sowie Boote und Loren."
  },
  max_minecart_speed: {
    name: "",
    description: ""
  },
  max_snow_accumulation_height: {
    name: "",
    description: "Die maximale Anzahl an Schneeschichten, die auf jedem Block angesammelt werden können."
  },
  mob_drops: {
    name: "",
    description: "Ob Kreaturen Drops und Erfahrungskugeln fallen lassen sollen."
  },
  mob_explosion_drop_decay: {
    name: "",
    description: "Ob Blockbeute von allen Blöcken (false) oder zufällig (true) fallen gelassen wird, abhängig davon, wie weit der Block vom Zentrum einer Kreatur-Explosion (z. B. Creeper-Explosion) entfernt ist."
  },
  mob_griefing: {
    name: "",
    description: "Ob Creeper, Zombies, Endermen, Ghasts, Withers, Enderdrachen, Kaninchen, Schafe, Dorfbewohner, Silberfischchen, Schneegolems und Endkristalle sollten dazu in der Lage sein Blöcke wechseln und ob Kreaturen Gegenstände aufnehmen können. Wenn MobGriefing deaktiviert ist, sammeln Piglins keine Goldbarren ein, aber ein Spieler kann trotzdem mit ihnen Handeln, indem er den Gegenstand auf die Kreatur verwendet. Ebenso sammeln Dorfbewohner keine Lebensmittel ein, können sich aber trotzdem fortpflanzen, bis ihnen die Lebensmittel in ihrem Inventar ausgehen. Dies wirkt sich auch auf die Fähigkeit zombieähnlicher Kreaturen wie zombifizierte Piglins und Ertrunkene aus, den Weg zu Schildkröteneiern zu finden."
  },
  natural_health_regeneration: {
    name: "",
    description: "Ob der Spieler seine Gesundheit auf natürliche Weise regenerieren kann, wenn sein Hunger ausreichend ist (hat keinen Einfluss auf externe Heilung, wie z. B. goldene Äpfel, den Regenerationseffekt usw.)."
  },
  // The Wiki still describes the inverse legacy rule here.
  player_movement_check: {
    name: "",
    description: ""
  },
  players_nether_portal_creative_delay: {
    name: "",
    description: "Steuert die Zeit, die ein Kreativmodus-Spieler benötigt, um in einem Netherportal zu stehen, bevor er die Dimension wechselt."
  },
  players_nether_portal_default_delay: {
    name: "",
    description: "Steuert die Zeit, die ein nicht-Kreativmodus-Spieler benötigt, um in einem Netherportal zu stehen, bevor er die Dimension wechselt."
  },
  players_sleeping_percentage: {
    name: "",
    description: "Wie viel Prozent der Spieler in der Oberwelt schlafen müssen, um die Nacht zu überspringen. Bei einem Prozentwert von 0 oder weniger kann die Nacht von nur einem Spieler übersprungen werden, und bei einem Prozentwert über 100 wird verhindert, dass Spieler die Nacht überhaupt überspringen."
  },
  projectiles_can_break_blocks: {
    name: "",
    description: "Ob Aufprallgeschosse Blöcke zerstören, die durch sie zerstörbar sind, d. h. Chorusblüten, spitze Tropfsteine und verzierte Krüge."
  },
  pvp: {
    name: "",
    description: "Ob der Spieler mit anderen Spielern kämpfen kann."
  },
  // The Wiki still describes the inverse legacy rule here.
  raids: {
    name: "",
    description: ""
  },
  random_tick_speed: {
    name: "",
    description: "Wie oft ein zufälliger Block-Tick pro Chunk-Abschnitt und Wild-Tick auftritt (z. B. Pflanzenwachstum, Blattverfall usw.). 0 und negative Werte deaktivieren Zufalls-Ticks, höhere Zahlen erhöhen Zufalls-Ticks. Die Einstellung auf eine hohe Ganzzahl führt zu hohen Zerfalls- und Wachstumsgeschwindigkeiten. Zahlen über 4096 bewirken, dass Pflanzenwachstum oder Blattverfall sofort erfolgen."
  },
  reduced_debug_info: {
    name: "",
    description: "Ob der Debug-Bildschirm alle oder reduzierte Informationen anzeigt; und ob die Auswirkungen von F3+B (Kreatur-Hitboxen) und F3+G (Chunk-Grenzen) angezeigt werden."
  },
  respawn_radius: {
    name: "",
    description: "Die Anzahl der Blöcke außerhalb der Spawn-Koordinaten der Welt, in denen ein Spieler spawnt, wenn er zum ersten Mal einem Server beitritt oder wenn er ohne persönlichen Spawnpunkt stirbt. Hat keine Auswirkungen auf Server, auf denen der Standardspielmodus Abenteuer ist."
  },
  send_command_feedback: {
    name: "",
    description: "Ob das Feedback zu den von einem Spieler ausgeführten Befehlen im Chat angezeigt werden soll. Beeinflusst auch das Standardverhalten, ob Befehlsblöcke ihren Ausgabetext speichern."
  },
  show_advancement_messages: {
    name: "",
    description: "Ob Fortschritte im Chat bekannt gegeben werden sollen."
  },
  show_death_messages: {
    name: "",
    description: "Ob Todesmeldungen in den Chat gestellt werden, wenn ein Spieler stirbt. Beeinflusst auch, ob eine Nachricht an den Besitzer des Haustiers gesendet wird, wenn das Haustier stirbt."
  },
  spawn_mobs: {
    name: "",
    description: "Ob Kreaturen natürlich spawnen sollen, oder über eine globale Spawning-Logik, wie zum Beispiel für Katzen, Phantome, Plünderer-Patrouillen, fahrende Händler oder Zombiebelagerungen. Beeinträchtigt nicht spezielle Spawnversuche wie Spawners, Überfälle oder Eisengolems."
  },
  spawn_monsters: {
    name: "",
    description: "Ob feindliche Kreaturen auf natürliche Weise spawnen können."
  },
  spawn_patrols: {
    name: "",
    description: "Ob Plünderer-Patrouillen spawnen können."
  },
  spawn_phantoms: {
    name: "",
    description: "Ob Phantome nachts spawnen können."
  },
  spawn_wandering_traders: {
    name: "",
    description: "Ob fahrende Händler spawnen können."
  },
  spawn_wardens: {
    name: "",
    description: "Ob Wärter spawnen können."
  },
  spawner_blocks_work: {
    name: "",
    description: "Aktiviert oder deaktiviert Spawner-Blöcke."
  },
  spectators_generate_chunks: {
    name: "",
    description: "Ob Spieler im Zuschauermodus Chunks generieren können."
  },
  spread_vines: {
    name: "",
    description: "Ob Ranken sich auf andere Blöcke ausbreiten können. Höhlenranken, Trauerranken und Zwirbelranken sind nicht betroffen."
  },
  tnt_explodes: {
    name: "",
    description: "Ob TNT nach der Aktivierung explodiert."
  },
  tnt_explosion_drop_decay: {
    name: "",
    description: "Ob Blockbeute von allen Blöcken (false) oder zufällig (true) fallen gelassen wird, abhängig davon, wie weit der Block vom Zentrum einer TNT-Explosion entfernt ist."
  },
  universal_anger: {
    name: "",
    description: "Lässt verärgerte neutrale Kreaturen jeden Spieler in der Nähe angreifen, nicht nur den Spieler, der sie verärgert hat. Funktioniert am besten, wenn forgiveDeadPlayers deaktiviert ist."
  },
  water_source_conversion: {
    name: "",
    description: "Ob sich neue Wasserquellen bilden dürfen."
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "",
    description: "Ob Feuer- und Lava-Ticks außerhalb einer 8-Chunk-Reichweite eines Spielers auftreten können."
  },
  allowEnteringNetherUsingPortals: {
    name: "",
    description: "Ob Spieler und Kreaturen durch Netherportale reisen können."
  },
  announceAdvancements: {
    name: "",
    description: "Ob Fortschritte im Chat bekannt gegeben werden sollen."
  },
  blockExplosionDropDecay: {
    name: "",
    description: "Ob Blockbeute von allen Blöcken (false) oder zufällig (true) fallen gelassen wird, abhängig davon, wie weit der Block vom Zentrum einer Blockexplosion entfernt ist (z. B. Klicken auf ein Bett in anderen Dimensionen). als die Oberwelt)."
  },
  commandBlockOutput: {
    name: "",
    description: "Ob Befehlsblöcke Administratoren benachrichtigen sollen, wenn sie Befehle ausführen."
  },
  commandBlocksEnabled: {
    name: "",
    description: "Ob Befehlsblöcke im Spiel aktiviert werden sollen."
  },
  commandModificationBlockLimit: {
    name: "",
    description: "Steuert die maximale Anzahl der Blöcke, die bei der Verwendung von Befehl /clone, Befehl /fill oder Befehl /fillbiome geändert werden."
  },
  disableElytraMovementCheck: {
    name: "",
    description: "Ob der Server die Überprüfung der Spielergeschwindigkeit überspringen soll, wenn der Spieler Elytren trägt. Hilft oft bei Glitches aufgrund von Verzögerungen im Mehrspielermodus."
  },
  disablePlayerMovementCheck: {
    name: "",
    description: ""
  },
  disableRaids: {
    name: "",
    description: "Ob Überfälle deaktiviert sind."
  },
  doDaylightCycle: {
    name: "",
    description: "Ob die Phasen Tag-Nacht-Rhythmus und Mondphasen fortschreiten."
  },
  doEntityDrops: {
    name: "",
    description: "Ob Objekte, die keine Kreaturen sind, Drops haben sollen."
  },
  doFireTick: {
    name: "",
    description: "Ob sich Feuer ausbreiten und auf natürliche Weise erlöschen soll."
  },
  doImmediateRespawn: {
    name: "",
    description: "Spieler erscheinen sofort wieder, ohne dass der Todesbildschirm angezeigt wird."
  },
  doInsomnia: {
    name: "",
    description: "Ob Phantome nachts spawnen können."
  },
  doLimitedCrafting: {
    name: "",
    description: "Ob Spieler nur die Rezepte herstellen können, die sie freigeschaltet haben."
  },
  doMobLoot: {
    name: "",
    description: "Ob Kreaturen Drops und Erfahrungskugeln fallen lassen sollen."
  },
  doMobSpawning: {
    name: "",
    description: "Ob Kreaturen natürlich spawnen sollen, oder über eine globale Spawning-Logik, wie zum Beispiel für Katzen, Phantome, Plünderer-Patrouillen, fahrende Händler oder Zombiebelagerungen. Beeinträchtigt nicht spezielle Spawnversuche wie Spawners, Überfälle oder Eisengolems."
  },
  doPatrolSpawning: {
    name: "",
    description: "Ob Plünderer-Patrouillen spawnen können."
  },
  doTileDrops: {
    name: "",
    description: "Ob Blöcke droppen sollen."
  },
  doTraderSpawning: {
    name: "",
    description: "Ob fahrende Händler spawnen können."
  },
  doVinesSpread: {
    name: "",
    description: "Ob Ranken sich auf andere Blöcke ausbreiten können. Höhlenranken, Trauerranken und Zwirbelranken sind nicht betroffen."
  },
  doWardenSpawning: {
    name: "",
    description: "Ob Wärter spawnen können."
  },
  doWeatherCycle: {
    name: "",
    description: "Ob sich das Wetter sich natürlich ändern kann. Der Befehl Befehl /weather kann das Wetter immer noch ändern."
  },
  drowningDamage: {
    name: "",
    description: "Ob der Spieler beim Ertrinken Schaden erleiden soll."
  },
  enderPearlsVanishOnDeath: {
    name: "",
    description: "Steuert, ob geworfene Enderperlen verschwinden, wenn der Spieler stirbt."
  },
  fallDamage: {
    name: "",
    description: "Ob der Spieler Sturzschaden erleiden soll."
  },
  fireDamage: {
    name: "",
    description: "Ob der Spieler Schaden in Feuer, Lava, Lagerfeuern oder auf Magmablöcken erleiden soll."
  },
  forgiveDeadPlayers: {
    name: "",
    description: "Verärgerte neutrale Kreaturen hören auf, wütend zu sein, wenn der Zielspieler in der Nähe stirbt."
  },
  freezeDamage: {
    name: "",
    description: "Ob der Spieler Schaden erleiden soll, wenn er sich im Pulverschnee befindet."
  },
  globalSoundEvents: {
    name: "",
    description: "Ob bestimmte Geräuschereignisse von allen Spielern gehört werden, unabhängig vom Standort."
  },
  keepInventory: {
    name: "",
    description: "Ob der Spieler Gegenstände und Erfahrung nach dem Tod in seinem Inventar behalten soll."
  },
  lavaSourceConversion: {
    name: "",
    description: "Ob sich neue Lavaquellen bilden dürfen."
  },
  locatorBar: {
    name: "",
    description: "Ob die Ortungsleiste des Spielers aktiviert ist und die Richtungen von Spielern oder Kreaturen anzeigt."
  },
  logAdminCommands: {
    name: "",
    description: "Ob Administratorbefehle im Serverprotokoll protokolliert werden sollen."
  },
  maxCommandChainLength: {
    name: "",
    description: "Die maximale Länge einer Befehlskette, die während eines Ticks ausgeführt werden kann. Gilt für Befehlsblöcke und Funktionen."
  },
  maxCommandForkCount: {
    name: "",
    description: "Die maximale Anzahl an Abspaltungen (Kontexten), die während eines Ticks erstellt werden können. Gilt für Befehlsblöcke und Funktionen."
  },
  maxEntityCramming: {
    name: "",
    description: "Die maximale Anzahl schiebbarer Kreaturen, die eine Kreatur oder Spieler schieben kann, bevor er pro halbe Sekunde 6 () Erstickungsschaden erleidet. Bei einer Einstellung auf 0 oder niedriger wird die Regel deaktiviert. Der Schaden betrifft Spieler im Überlebens- oder Abenteuermodus und alle Kreaturen außer Fledermäusen. Zu den schiebbaren Kreaturen gehören Spieler, die nicht im Zuschauermodus sind, alle Kreaturen außer Fledermäusen sowie Boote und Loren."
  },
  minecartMaxSpeed: {
    name: "",
    description: ""
  },
  mobExplosionDropDecay: {
    name: "",
    description: "Ob Blockbeute von allen Blöcken (false) oder zufällig (true) fallen gelassen wird, abhängig davon, wie weit der Block vom Zentrum einer Kreatur-Explosion (z. B. Creeper-Explosion) entfernt ist."
  },
  mobGriefing: {
    name: "",
    description: "Ob Creeper, Zombies, Endermen, Ghasts, Withers, Enderdrachen, Kaninchen, Schafe, Dorfbewohner, Silberfischchen, Schneegolems und Endkristalle sollten dazu in der Lage sein Blöcke wechseln und ob Kreaturen Gegenstände aufnehmen können. Wenn MobGriefing deaktiviert ist, sammeln Piglins keine Goldbarren ein, aber ein Spieler kann trotzdem mit ihnen Handeln, indem er den Gegenstand auf die Kreatur verwendet. Ebenso sammeln Dorfbewohner keine Lebensmittel ein, können sich aber trotzdem fortpflanzen, bis ihnen die Lebensmittel in ihrem Inventar ausgehen. Dies wirkt sich auch auf die Fähigkeit zombieähnlicher Kreaturen wie zombifizierte Piglins und Ertrunkene aus, den Weg zu Schildkröteneiern zu finden."
  },
  naturalRegeneration: {
    name: "",
    description: "Ob der Spieler seine Gesundheit auf natürliche Weise regenerieren kann, wenn sein Hunger ausreichend ist (hat keinen Einfluss auf externe Heilung, wie z. B. goldene Äpfel, den Regenerationseffekt usw.)."
  },
  playersNetherPortalCreativeDelay: {
    name: "",
    description: "Steuert die Zeit, die ein Kreativmodus-Spieler benötigt, um in einem Netherportal zu stehen, bevor er die Dimension wechselt."
  },
  playersNetherPortalDefaultDelay: {
    name: "",
    description: "Steuert die Zeit, die ein nicht-Kreativmodus-Spieler benötigt, um in einem Netherportal zu stehen, bevor er die Dimension wechselt."
  },
  playersSleepingPercentage: {
    name: "",
    description: "Wie viel Prozent der Spieler in der Oberwelt schlafen müssen, um die Nacht zu überspringen. Bei einem Prozentwert von 0 oder weniger kann die Nacht von nur einem Spieler übersprungen werden, und bei einem Prozentwert über 100 wird verhindert, dass Spieler die Nacht überhaupt überspringen."
  },
  projectilesCanBreakBlocks: {
    name: "",
    description: "Ob Aufprallgeschosse Blöcke zerstören, die durch sie zerstörbar sind, d. h. Chorusblüten, spitze Tropfsteine und verzierte Krüge."
  },
  randomTickSpeed: {
    name: "",
    description: "Wie oft ein zufälliger Block-Tick pro Chunk-Abschnitt und Wild-Tick auftritt (z. B. Pflanzenwachstum, Blattverfall usw.). 0 und negative Werte deaktivieren Zufalls-Ticks, höhere Zahlen erhöhen Zufalls-Ticks. Die Einstellung auf eine hohe Ganzzahl führt zu hohen Zerfalls- und Wachstumsgeschwindigkeiten. Zahlen über 4096 bewirken, dass Pflanzenwachstum oder Blattverfall sofort erfolgen."
  },
  reducedDebugInfo: {
    name: "",
    description: "Ob der Debug-Bildschirm alle oder reduzierte Informationen anzeigt; und ob die Auswirkungen von F3+B (Kreatur-Hitboxen) und F3+G (Chunk-Grenzen) angezeigt werden."
  },
  sendCommandFeedback: {
    name: "",
    description: "Ob das Feedback zu den von einem Spieler ausgeführten Befehlen im Chat angezeigt werden soll. Beeinflusst auch das Standardverhalten, ob Befehlsblöcke ihren Ausgabetext speichern."
  },
  showDeathMessages: {
    name: "",
    description: "Ob Todesmeldungen in den Chat gestellt werden, wenn ein Spieler stirbt. Beeinflusst auch, ob eine Nachricht an den Besitzer des Haustiers gesendet wird, wenn das Haustier stirbt."
  },
  snowAccumulationHeight: {
    name: "",
    description: "Die maximale Anzahl an Schneeschichten, die auf jedem Block angesammelt werden können."
  },
  spawnChunkRadius: {
    name: "",
    description: "Die Größe der Spawn-Chunks der Welt.."
  },
  spawnMonsters: {
    name: "",
    description: "Ob feindliche Kreaturen auf natürliche Weise spawnen können."
  },
  spawnerBlocksEnabled: {
    name: "",
    description: "Aktiviert oder deaktiviert Spawner-Blöcke."
  },
  spawnRadius: {
    name: "",
    description: "Die Anzahl der Blöcke außerhalb der Spawn-Koordinaten der Welt, in denen ein Spieler spawnt, wenn er zum ersten Mal einem Server beitritt oder wenn er ohne persönlichen Spawnpunkt stirbt. Hat keine Auswirkungen auf Server, auf denen der Standardspielmodus Abenteuer ist."
  },
  spectatorsGenerateChunks: {
    name: "",
    description: "Ob Spieler im Zuschauermodus Chunks generieren können."
  },
  tntExplodes: {
    name: "",
    description: "Ob TNT nach der Aktivierung explodiert."
  },
  tntExplosionDropDecay: {
    name: "",
    description: "Ob Blockbeute von allen Blöcken (false) oder zufällig (true) fallen gelassen wird, abhängig davon, wie weit der Block vom Zentrum einer TNT-Explosion entfernt ist."
  },
  universalAnger: {
    name: "",
    description: "Lässt verärgerte neutrale Kreaturen jeden Spieler in der Nähe angreifen, nicht nur den Spieler, der sie verärgert hat. Funktioniert am besten, wenn forgiveDeadPlayers deaktiviert ist."
  },
  waterSourceConversion: {
    name: "",
    description: "Ob sich neue Wasserquellen bilden dürfen."
  },
};

export default gamerulePresets;
