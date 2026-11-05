import type { GameruleTranslations } from ".";

/**
 * Text excerpts from Minecraft Wiki contributors, CC BY-NC-SA 3.0.
 * Wiki markup and references omitted; unavailable translations are empty.
 * @see https://fr.minecraft.wiki/w/Règle_de_jeu?oldid=391660
 * @see https://fr.minecraft.wiki/w/Règle_de_jeu?oldid=356970
 * @see https://creativecommons.org/licenses/by-nc-sa/3.0/
 */
export const wikiUrl = "https://fr.minecraft.wiki/w/Règle_de_jeu";

const gamerulePresets: GameruleTranslations = {
  advance_time: {
    name: "",
    description: "Si le cycle jour-nuit et les phases de la Lune progressent."
  },
  advance_weather: {
    name: "",
    description: "Si la météo peut changer naturellement. La commande /weather peut toujours changer la météo."
  },
  allow_entering_nether_using_portals: {
    name: "",
    description: "Si le Nether peut être atteint en utilisant les portails du Nether."
  },
  block_drops: {
    name: "",
    description: "Si les blocs doivent lâcher du butin."
  },
  block_explosion_drop_decay: {
    name: "",
    description: "Si le butin des blocs est lâché par tous les blocs (false) ou aléatoirement (true) en fonction de la distance du bloc par rapport au centre d'une explosion de bloc (par exemple, cliquer sur un lit dans des dimensions différentes de la Surface)."
  },
  command_block_output: {
    name: "",
    description: "Si les blocs de commande doivent notifier les administrateurs lorsqu'ils exécutent des commandes."
  },
  command_blocks_work: {
    name: "",
    description: "Si les blocs de commande doit être activés en jeu."
  },
  drowning_damage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts en se noyant."
  },
  elytra_movement_check: {
    name: "",
    description: "Si le serveur doit vérifier la vitesse du joueur lorsque le joueur porte des élytres. Permet généralement d'éviter les tremblements dus à de la latence en multijoueur."
  },
  ender_pearls_vanish_on_death: {
    name: "",
    description: "Contrôle si les perles de l'Ender lancées disparaissent lorsque le joueur meurt."
  },
  entity_drops: {
    name: "",
    description: "Si les entités qui ne sont pas des créatures doivent lâcher du butin."
  },
  fall_damage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts de chute."
  },
  fire_damage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts du feu, de la lave, des feux de camp et des blocs de magma."
  },
  fire_spread_radius_around_player: {
    name: "",
    description: "Contrôle le rayon maximal en blocs autour d'un joueur dans lequel le feu peut se propager."
  },
  forgive_dead_players: {
    name: "",
    description: "Empêche les créatures neutres agressives situées dans un rayon de 65 x 21 x 65 blocs autour du joueur ciblé de continuer d'être agressives lorsque celui-ci meurt."
  },
  freeze_damage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts lorsqu'il est dans de la neige poudreuse."
  },
  global_sound_events: {
    name: "",
    description: "Si le son d'apparition de Wither, celui de la mort d'Ender Dragon et celui de l'activation de portail de l'End sont entendus par tous les joueurs, peu importe leur emplacement."
  },
  immediate_respawn: {
    name: "",
    description: "Si les joueurs réapparaissent immédiatement sans afficher l'écran de mort."
  },
  keep_inventory: {
    name: "",
    description: "Si le joueur garde les objets dans son inventaire et son expérience lorsqu'il meurt."
  },
  lava_source_conversion: {
    name: "",
    description: "Si des nouvelles sources de lave peuvent se former."
  },
  limited_crafting: {
    name: "",
    description: "Si les joueurs ne peuvent fabriquer que les recettes qu'ils ont débloquées."
  },
  locator_bar: {
    name: "",
    description: "Si la barre de localisation du joueur est activée, indiquant l'emplacement des joueurs ou des entités."
  },
  log_admin_commands: {
    name: "",
    description: "Si les commandes des administrateurs sont affichées dans le journal du serveur."
  },
  max_block_modifications: {
    name: "",
    description: "Contrôle le nombre maximum de blocs modifiés en utilisant les commandes /clone, /fill et /fillbiome."
  },
  max_command_forks: {
    name: "",
    description: "Le nombre maximum d'embranchements (contextes) pouvant être créés pendant un tick. S'applique aux blocs de commande et aux fonctions."
  },
  max_command_sequence_length: {
    name: "",
    description: "La longueur maximum d'une chaîne de commandes pouvant être exécutée pendant un tick. S'applique aux blocs de commande et aux fonctions."
  },
  max_entity_cramming: {
    name: "",
    description: "Le nombre maximum d'entités qu'une créature ou qu'un joueur peut pousser, avant de prendre 6HP points de dégâts d'entassement des entités par demi-seconde. La définir à 0 ou moins désactive la règle. Les dégâts affectent les joueurs en mode Survie et Aventure, et toutes les créatures sauf les chauve-souris. Les entités pouvant être poussées incluent les joueurs n'étant pas en mode Spectateur, toutes les créatures sauf les chauve-souris, ainsi que les bateaux et les wagonnets."
  },
  max_minecart_speed: {
    name: "",
    description: "La vitesse maximum qu'un wagonnet peut atteindre."
  },
  max_snow_accumulation_height: {
    name: "",
    description: "Le nombre maximum de couches de neige pouvant être accumulées sur chaque bloc."
  },
  mob_drops: {
    name: "",
    description: "Si les créatures peuvent lâcher du butin et des orbes d'expérience."
  },
  mob_explosion_drop_decay: {
    name: "",
    description: "Si le butin des blocs est lâché par tous les blocs (false) ou aléatoirement (true) en fonction de la distance du bloc par rapport au centre d'une explosion de créature (par exemple, une explosion de Creeper)."
  },
  mob_griefing: {
    name: "",
    description: "Si les Creepers, les zombies, les Endermen, les Ghasts, les Withers, les Ender Dragons, les lapins, les moutons, les villageois, les poissons d'argent, les golems de neige et les cristaux de l'End sont capables de modifier des blocs, et si les créatures peuvent ramasser des objets. Lorsque la règle est désactivée, les Piglins ne ramassent pas les lingots d'or, mais un joueur peut toujours troquer avec eux en utilisant l'objet sur la créature. De manière similaire, les villageois ne ramassent pas les objets de nourriture mais peuvent toujours se reproduire jusqu'à ce qu'ils manquent de nourriture déjà présente dans leur inventaire. Cela affecte également la capacité des créatures de type zombie, comme les Piglins zombifiés et les noyés, à rechercher leur chemin vers des œufs de tortue."
  },
  natural_health_regeneration: {
    name: "",
    description: "Si le joueur peut régénérer de la vie naturellement si sa barre de faim est assez remplie (n'affecte pas les sources de guérison externes, comme les pommes dorées, l'effet Régénération, etc)."
  },
  player_movement_check: {
    name: "",
    description: "Si le serveur doit vérifier la vitesse du joueur."
  },
  players_nether_portal_creative_delay: {
    name: "",
    description: "Contrôle le temps pendant lequel un joueur en Créatif doit rester dans un portail du Nether avant de changer de dimension."
  },
  players_nether_portal_default_delay: {
    name: "",
    description: "Contrôle le temps pendant lequel un joueur n'étant pas en Créatif doit rester dans un portail du Nether avant de changer de dimension."
  },
  players_sleeping_percentage: {
    name: "",
    description: "Le pourcentage de joueurs dans la Surface devant dormir pour passer la nuit. Une valeur de pourcentage inférieure ou égale à 0 permet de passer la nuit avec seulement 1 joueur, et une valeur de pourcentage supérieure à 100 empêche les joueurs de passer la nuit."
  },
  projectiles_can_break_blocks: {
    name: "",
    description: "Si les projectiles d'impact détruisent les blocs destructibles par eux, c'est-à-dire les fleurs de chorus, les spéléothèmes pointus et les vases décorés."
  },
  pvp: {
    name: "",
    description: "Si le joueur peut se battre avec d'autres joueurs."
  },
  raids: {
    name: "",
    description: "Si les invasions sont activées."
  },
  random_tick_speed: {
    name: "",
    description: "La fréquence à laquelle un tick de bloc aléatoire survient (comme la croissance des plantes, la détérioration des feuilles, etc) par section de chunk et par tick de jeu. Une valeur inférieure ou égale à 0 désactive les ticks aléatoires, et une valeur supérieure augmente les ticks aléatoires. La définir à un entier élevé entraîne des vitesses élevées de détérioration et de croissance. Les nombres supérieurs à 4096 rendent la croissance des plantes et la détérioration des feuilles instantanées."
  },
  reduced_debug_info: {
    name: "",
    description: "Si l'écran de débogage affiche toutes les informations ou des informations réduites ; et si les effets de F3 + B (les boîtes de collision des entités) et F3 + G (les bordures des chunks) sont affichés."
  },
  respawn_radius: {
    name: "",
    description: "Le rayon en blocs à partir des coordonnées d'apparition du monde dans lequel un joueur apparaît la première fois qu'il rejoint un serveur ou en mourant sans point d'apparition personnel. N'a aucun effet sur les serveurs où le mode de jeu par défaut est Aventure."
  },
  send_command_feedback: {
    name: "",
    description: "Si le retour des commandes exécutées par un joueur s'affiche dans le tchat. Affecte également le comportement par défaut des blocs de commande stockant ou non le texte de sortie."
  },
  show_advancement_messages: {
    name: "",
    description: "Si les progrès doivent être annoncés dans le tchat."
  },
  show_death_messages: {
    name: "",
    description: "Si les messages de mort sont affichés dans le tchat lorsqu'un joueur meurt. Affecte également si un message est envoyé au propriétaire d'un animal de compagnie lorsque celui-ci meurt."
  },
  spawn_mobs: {
    name: "",
    description: "Si les créatures peuvent apparaître naturellement, ou via une logique d'apparition globale, comme pour les chats, les Phantoms, les patrouilles, les marchands ambulants ou les sièges de zombies. N'affecte pas les tentatives d'apparition spéciales, comme les générateurs de créatures, les invasions ou les golems de fer."
  },
  spawn_monsters: {
    name: "",
    description: "Si les monstres peuvent apparaître naturellement."
  },
  spawn_patrols: {
    name: "",
    description: "Si les patrouilles peuvent apparaître."
  },
  spawn_phantoms: {
    name: "",
    description: "Si les Phantoms peuvent apparaître pendant la nuit."
  },
  spawn_wandering_traders: {
    name: "",
    description: "Si les marchands ambulants peuvent apparaître."
  },
  spawn_wardens: {
    name: "",
    description: "Si les Wardens peuvent apparaître."
  },
  spawner_blocks_work: {
    name: "",
    description: "Active ou désactive les générateur de créatures."
  },
  spectators_generate_chunks: {
    name: "",
    description: "Si les joueurs en mode Spectateur peuvent générer des chunks."
  },
  spread_vines: {
    name: "",
    description: "Si les lianes peuvent se propager vers d'autres blocs. Les lianes des cavernes, les lianes pleureuses et les lianes tortueuses ne sont pas affectées."
  },
  tnt_explodes: {
    name: "",
    description: "Si le TNT explose après son activation."
  },
  tnt_explosion_drop_decay: {
    name: "",
    description: "Si le butin des blocs est lâché par tous les blocs (false) ou aléatoirement (true) en fonction de la distance du bloc par rapport au centre d'une explosion de TNT."
  },
  universal_anger: {
    name: "",
    description: "Si les créatures neutres énervées attaquent n'importe quel joueur à proximité, et non uniquement le joueur qui les a énervées. Fonctionne mieux si forgive_dead_players est désactivée."
  },
  water_source_conversion: {
    name: "",
    description: "Si des nouvelles sources d'eau peuvent se former."
  },

  // Before Java Edition 1.21.11.
  allowFireTicksAwayFromPlayer: {
    name: "",
    description: ""
  },
  allowEnteringNetherUsingPortals: {
    name: "",
    description: "Si le Nether peut être atteint en utilisant les portails du Nether."
  },
  announceAdvancements: {
    name: "",
    description: "Si les progrès doivent être annoncés dans le tchat."
  },
  blockExplosionDropDecay: {
    name: "",
    description: "Si le butin des blocs est lâché par tous les blocs (false) ou aléatoirement (true) en fonction de la distance du bloc par rapport au centre d'une explosion de bloc (par exemple, cliquer sur un lit dans des dimensions différentes de la Surface)."
  },
  commandBlockOutput: {
    name: "",
    description: "Si les blocs de commande doivent notifier les administrateurs lorsqu'ils exécutent des commandes."
  },
  commandBlocksEnabled: {
    name: "",
    description: "Si les blocs de commande doit être activés en jeu."
  },
  commandModificationBlockLimit: {
    name: "",
    description: "Contrôle le nombre maximum de blocs modifiés en utilisant les commandes /clone, /fill et /fillbiome."
  },
  disableElytraMovementCheck: {
    name: "",
    description: "Si le serveur doit ignorer la vérification de la vitesse du joueur lorsque le joueur porte des élytres. Permet généralement d'éviter les tremblements dus à de la latence en multijoueur."
  },
  disablePlayerMovementCheck: {
    name: "",
    description: "Si le serveur doit ignorer la vérification de la vitesse du joueur."
  },
  disableRaids: {
    name: "",
    description: "Si les invasions sont désactivées."
  },
  doDaylightCycle: {
    name: "",
    description: "Si le cycle jour-nuit et les phases de la Lune progressent."
  },
  doEntityDrops: {
    name: "",
    description: "Si les entités qui ne sont pas des créatures doivent lâcher du butin."
  },
  doFireTick: {
    name: "",
    description: "Si le feu se propage et s'éteint naturellement."
  },
  doImmediateRespawn: {
    name: "",
    description: "Si les joueurs réapparaissent immédiatement sans afficher l'écran de mort."
  },
  doInsomnia: {
    name: "",
    description: "Si les Phantoms peuvent apparaître pendant la nuit."
  },
  doLimitedCrafting: {
    name: "",
    description: "Si les joueurs ne peuvent fabriquer que les recettes qu'ils ont débloquées."
  },
  doMobLoot: {
    name: "",
    description: "Si les créatures peuvent lâcher du butin et des orbes d'expérience."
  },
  doMobSpawning: {
    name: "",
    description: "Si les créatures peuvent apparaître naturellement, ou via une logique d'apparition globale, comme pour les chats, les Phantoms, les patrouilles, les marchands ambulants ou les sièges de zombies. N'affecte pas les tentatives d'apparition spéciales, comme les générateurs de créatures, les invasions ou les golems de fer."
  },
  doPatrolSpawning: {
    name: "",
    description: "Si les patrouilles peuvent apparaître."
  },
  doTileDrops: {
    name: "",
    description: "Si les blocs doivent lâcher du butin."
  },
  doTraderSpawning: {
    name: "",
    description: "Si les marchands ambulants peuvent apparaître."
  },
  doVinesSpread: {
    name: "",
    description: "Si les lianes peuvent se propager vers d'autres blocs. Les lianes des cavernes, les lianes pleureuses et les lianes tortueuses ne sont pas affectées."
  },
  doWardenSpawning: {
    name: "",
    description: "Si les Wardens peuvent apparaître."
  },
  doWeatherCycle: {
    name: "",
    description: "Si la météo peut changer naturellement. La commande /weather peut toujours changer la météo."
  },
  drowningDamage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts en se noyant."
  },
  enderPearlsVanishOnDeath: {
    name: "",
    description: "Contrôle si les perles de l'Ender lancées disparaissent lorsque le joueur meurt."
  },
  fallDamage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts de chute."
  },
  fireDamage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts du feu, de la lave, des feux de camp et des blocs de magma."
  },
  forgiveDeadPlayers: {
    name: "",
    description: "Si les créatures neutres énervées arrêtent d'être énervées lorsque le joueur ciblé meurt à proximité."
  },
  freezeDamage: {
    name: "",
    description: "Si le joueur doit prendre des dégâts lorsqu'il est dans de la neige poudreuse."
  },
  globalSoundEvents: {
    name: "",
    description: "Si certains événements sonores sont entendus par tous les joueurs, peu importe leur emplacement."
  },
  keepInventory: {
    name: "",
    description: "Si le joueur garde les objets dans son inventaire et son expérience lorsqu'il meurt."
  },
  lavaSourceConversion: {
    name: "",
    description: "Si des nouvelles sources de lave peuvent se former."
  },
  locatorBar: {
    name: "",
    description: "Si la barre de localisation du joueur est activée, indiquant l'emplacement des joueurs ou des entités."
  },
  logAdminCommands: {
    name: "",
    description: "Si les commandes des administrateurs sont affichées dans le journal du serveur."
  },
  maxCommandChainLength: {
    name: "",
    description: "La longueur maximum d'une chaîne de commandes pouvant être exécutée pendant un tick. S'applique aux blocs de commande et aux fonctions."
  },
  maxCommandForkCount: {
    name: "",
    description: "Le nombre maximum d'embranchements (contextes) pouvant être créés pendant un tick. S'applique aux blocs de commande et aux fonctions."
  },
  maxEntityCramming: {
    name: "",
    description: "Le nombre maximum d'entités qu'une créature ou qu'un joueur peut pousser, avant de prendre 6HP points de dégâts d'entassement des entités par demi-seconde. La définir à 0 ou moins désactive la règle. Les dégâts affectent les joueurs en mode Survie et Aventure, et toutes les créatures sauf les chauve-souris. Les entités pouvant être poussées incluent les joueurs n'étant pas en mode Spectateur, toutes les créatures sauf les chauve-souris, ainsi que les bateaux et les wagonnets."
  },
  minecartMaxSpeed: {
    name: "",
    description: "La vitesse maximum qu'un wagonnet peut atteindre."
  },
  mobExplosionDropDecay: {
    name: "",
    description: "Si le butin des blocs est lâché par tous les blocs (false) ou aléatoirement (true) en fonction de la distance du bloc par rapport au centre d'une explosion de créature (par exemple, une explosion de Creeper)."
  },
  mobGriefing: {
    name: "",
    description: "Si les Creepers, les zombies, les Endermen, les Ghasts, les Withers, les Ender Dragons, les lapins, les moutons, les villageois, les poissons d'argent, les golems de neige et les cristaux de l'End sont capables de modifier des blocs, et si les créatures peuvent ramasser des objets. Lorsque la règle est désactivée, les Piglins ne ramassent pas les lingots d'or, mais un joueur peut toujours troquer avec eux en utilisant l'objet sur la créature. De manière similaire, les villageois ne ramassent pas les objets de nourriture mais peuvent toujours se reproduire jusqu'à ce qu'ils manquent de nourriture déjà présente dans leur inventaire. Cela affecte également la capacité des créatures de type zombie, comme les Piglins zombifiés et les noyés, à rechercher leur chemin vers des œufs de tortue."
  },
  naturalRegeneration: {
    name: "",
    description: "Si le joueur peut régénérer de la vie naturellement si sa barre de faim est assez remplie (n'affecte pas les sources de guérison externes, comme les pommes dorées, l'effet de régénération, etc)."
  },
  playersNetherPortalCreativeDelay: {
    name: "",
    description: "Contrôle le temps pendant lequel un joueur en Créatif doit rester dans un portail du Nether avant de changer de dimension."
  },
  playersNetherPortalDefaultDelay: {
    name: "",
    description: "Contrôle le temps pendant lequel un joueur n'étant pas en Créatif doit rester dans un portail du Nether avant de changer de dimension."
  },
  playersSleepingPercentage: {
    name: "",
    description: "Le pourcentage de joueurs à la Surface devant dormir pour passer la nuit. Une valeur de pourcentage inférieure ou égale à 0 permet de passer la nuit avec seulement 1 joueur, et une valeur de pourcentage supérieure à 100 empêche les joueurs de passer la nuit."
  },
  projectilesCanBreakBlocks: {
    name: "",
    description: "Si les projectiles d'impact détruisent les blocs destructibles par eux, c'est-à-dire les fleurs de chorus, les spéléothèmes pointus et les vases décorés."
  },
  randomTickSpeed: {
    name: "",
    description: "La fréquence à laquelle un tick de bloc aléatoire survient (comme la croissance des plantes, la détérioration des feuilles, etc) par section de chunk et par tick de jeu. Une valeur inférieure ou égale à 0 désactive les ticks aléatoires, et une valeur supérieure augmente les ticks aléatoires. La définir à un entier élevé entraîne des vitesses élevées de détérioration et de croissance. Les nombres supérieurs à 4096 rendent la croissance des plantes et la détérioration des feuilles instantanées."
  },
  reducedDebugInfo: {
    name: "",
    description: "Si l'écran de débogage affiche toutes les informations ou des informations réduites ; et si les effets de F3 + B (les boîtes de collision des entités) et F3 + G (les bordures des chunks) sont affichés."
  },
  sendCommandFeedback: {
    name: "",
    description: "Si le retour des commandes exécutées par un joueur s'affiche dans le tchat. Affecte également le comportement par défaut des blocs de commande stockant ou non le texte de sortie."
  },
  showDeathMessages: {
    name: "",
    description: "Si les messages de mort sont affichés dans le tchat lorsqu'un joueur meurt. Affecte également si un message est envoyé au propriétaire d'un animal de compagnie lorsque celui-ci meurt."
  },
  snowAccumulationHeight: {
    name: "",
    description: "Le nombre maximum de couches de neige pouvant être accumulées sur chaque bloc."
  },
  spawnChunkRadius: {
    name: "",
    description: "La taille des chunks d'apparition du monde."
  },
  spawnMonsters: {
    name: "",
    description: "Si les monstres peuvent apparaître naturellement."
  },
  spawnerBlocksEnabled: {
    name: "",
    description: "Active ou désactive les générateur de créatures."
  },
  spawnRadius: {
    name: "",
    description: "Le rayon en blocs à partir des coordonnées d'apparition du monde dans lequel un joueur apparaît la première fois qu'il rejoint un serveur ou en mourant sans point d'apparition personnel. N'a aucun effet sur les serveurs où le mode de jeu par défaut est Aventure."
  },
  spectatorsGenerateChunks: {
    name: "",
    description: "Si les joueurs en mode Spectateur peuvent générer des chunks."
  },
  tntExplodes: {
    name: "",
    description: "Si le TNT explose après son activation."
  },
  tntExplosionDropDecay: {
    name: "",
    description: "Si le butin des blocs est lâché par tous les blocs (false) ou aléatoirement (true) en fonction de la distance du bloc par rapport au centre d'une explosion de TNT."
  },
  universalAnger: {
    name: "",
    description: "Si les créatures neutres énervées attaquent n'importe quel joueur à proximité, et non uniquement le joueur qui les a énervées. Fonctionne mieux si forgiveDeadPlayers est désactivée."
  },
  waterSourceConversion: {
    name: "",
    description: "Si des nouvelles sources d'eau peuvent se former."
  },
};

export default gamerulePresets;
