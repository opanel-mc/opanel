use std::fmt::Write as _;

use pumpkin::entity::NBTStorage;
use pumpkin_data::item_stack::ItemStack as PumpkinItemStack;
use pumpkin_inventory::{
    Inventory,
    player::{ender_chest_inventory::EnderChestInventory, player_inventory::PlayerInventory},
};
use pumpkin_nbt::{compound::NbtCompound, tag::NbtTag};
use serde::{Deserialize, Serialize};
use thiserror::Error;

use super::{Player, PlayerError, data_directory};
use crate::utils::{item, player_data, snbt};

// Panel order is head, chest, legs, feet, offhand; Pumpkin uses native slots 36..40.
const EQUIPMENT: [(&str, usize, i8); 5] = [
    ("head", 39, 103),
    ("chest", 38, 102),
    ("legs", 37, 101),
    ("feet", 36, 100),
    ("offhand", 40, -106),
];

#[derive(Debug, Error)]
pub(crate) enum InventoryError {
    #[error(transparent)]
    Player(#[from] PlayerError),
    #[error("Invalid inventory item or slot.")]
    InvalidItem,
}

#[derive(Debug, Clone, Copy, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) enum InventoryType {
    Main,
    Equipments,
    EnderChest,
}

impl InventoryType {
    fn size(self) -> usize {
        match self {
            Self::Main => 36,
            Self::Equipments => 5,
            Self::EnderChest => 27,
        }
    }
}

#[derive(Debug, Clone, Deserialize, Serialize)]
pub(crate) struct ItemStack {
    pub slot: usize,
    pub id: Option<String>,
    pub count: i32,
    pub snbt: Option<String>,
}

impl ItemStack {
    fn empty(slot: usize) -> Self {
        Self {
            slot,
            id: Some("minecraft:air".into()),
            count: 0,
            snbt: None,
        }
    }

    fn from_nbt(slot: usize, item: &NbtCompound) -> Self {
        let Some(id) = item.get_string("id") else {
            return Self::empty(slot);
        };
        let count = item.get_int("count").unwrap_or(0);
        if count <= 0 || id == "minecraft:air" {
            return Self::empty(slot);
        }
        Self {
            slot,
            id: Some(id.to_owned()),
            count,
            snbt: item
                .get_compound("components")
                .filter(|components| !components.child_tags.is_empty())
                .map(snbt::compound_to_string),
        }
    }

    fn to_native(&self) -> Result<PumpkinItemStack, InventoryError> {
        let Some(id) = self.id.as_deref().filter(|id| *id != "minecraft:air") else {
            return Ok(PumpkinItemStack::EMPTY.clone());
        };
        if self.count <= 0 {
            return Ok(PumpkinItemStack::EMPTY.clone());
        }
        // Pumpkin stores counts as u8; reject overflow rather than silently wrapping.
        if self.count > i32::from(u8::MAX) {
            return Err(InventoryError::InvalidItem);
        }
        let mut data = NbtCompound::new();
        data.put_string("id", id.to_owned());
        data.put_int("count", self.count);
        if let Some(input) = self
            .snbt
            .as_deref()
            .filter(|input| !input.trim().is_empty())
        {
            let components = snbt::parse_compound(input).ok_or(InventoryError::InvalidItem)?;
            data.put_compound("components", components);
        }
        PumpkinItemStack::read_item_stack(&data).ok_or(InventoryError::InvalidItem)
    }
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct InventoryUpdate {
    pub inventory_type: InventoryType,
    pub item: ItemStack,
}

impl InventoryUpdate {
    fn to_native(&self) -> Result<PumpkinItemStack, InventoryError> {
        if self.item.slot >= self.inventory_type.size() {
            return Err(InventoryError::InvalidItem);
        }
        self.item.to_native()
    }
}

#[derive(Debug, Clone, Serialize)]
struct InventoryContents {
    size: usize,
    items: Vec<ItemStack>,
}

impl InventoryContents {
    fn empty(size: usize) -> Self {
        Self {
            size,
            items: (0..size).map(ItemStack::empty).collect(),
        }
    }
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct PlayerInventorySnapshot {
    pub hash: String,
    main: InventoryContents,
    equipments: InventoryContents,
    ender_chest: InventoryContents,
}

impl PlayerInventorySnapshot {
    pub(crate) fn from_nbt(data: &NbtCompound) -> Self {
        let mut result = Self {
            hash: String::new(),
            main: InventoryContents::empty(36),
            equipments: InventoryContents::empty(5),
            ender_chest: InventoryContents::empty(27),
        };
        for item in data.get_list("Inventory").into_iter().flatten() {
            let Some(item) = item.extract_compound() else {
                continue;
            };
            let Some(slot) = item.get_byte("Slot") else {
                continue;
            };
            if (0..36).contains(&slot) {
                result.main.items[slot as usize] = ItemStack::from_nbt(slot as usize, item);
            } else if let Some(index) = EQUIPMENT
                .iter()
                .position(|(_, native, saved)| slot == *saved || i32::from(slot) == *native as i32)
            {
                result.equipments.items[index] = ItemStack::from_nbt(index, item);
            }
        }
        // Modern equipment entries take precedence, just like Pumpkin's player loader.
        if let Some(equipment) = data.get_compound("equipment") {
            for (slot, (key, _, _)) in EQUIPMENT.iter().enumerate() {
                if let Some(item) = equipment.get_compound(key) {
                    result.equipments.items[slot] = ItemStack::from_nbt(slot, item);
                }
            }
        }
        for item in data.get_list("EnderItems").into_iter().flatten() {
            let Some(item) = item.extract_compound() else {
                continue;
            };
            if let Some(slot) = item.get_byte("Slot").filter(|slot| (0..27).contains(slot)) {
                result.ender_chest.items[slot as usize] = ItemStack::from_nbt(slot as usize, item);
            }
        }

        let mut contents = String::new();
        for (name, inventory) in [
            ("main", &result.main),
            ("equipments", &result.equipments),
            ("enderChest", &result.ender_chest),
        ] {
            let _ = write!(contents, "{name}{{");
            for item in &inventory.items {
                let _ = write!(
                    contents,
                    "{}|{}|{}|{};",
                    item.slot,
                    item.id.as_deref().unwrap_or_default(),
                    item.count,
                    item.snbt.as_deref().unwrap_or_default()
                );
            }
            contents.push('}');
        }
        result.hash = format!("{:x}", md5::compute(contents));
        result
    }
}

impl Player {
    /// May access a compressed offline player file; run in a blocking task.
    pub(crate) fn inventory(&self) -> Result<PlayerInventorySnapshot, InventoryError> {
        let data = if let Some(player) = &self.online {
            let mut data = NbtCompound::new();
            player.inventory.write_nbt(&mut data);
            player.ender_chest_inventory.write_nbt(&mut data);
            data
        } else {
            player_data::read(&data_directory(&self.server), self.uuid)
                .map_err(PlayerError::from)?
        };
        Ok(PlayerInventorySnapshot::from_nbt(&data))
    }

    pub(crate) fn set_inventory_item(
        &self,
        update: &InventoryUpdate,
    ) -> Result<(), InventoryError> {
        let stack = update.to_native()?;
        if let Some(player) = &self.online {
            // Slot 9 belongs to the main inventory. Keep the host-created trait object:
            // casting player.inventory here would create a DLL-local vtable instead.
            let inventory = player
                .player_screen_handler
                .lock()
                .unwrap_or_else(std::sync::PoisonError::into_inner)
                .get_slot(9)
                .get_inventory();
            let stack = item::copy_to_host(inventory.as_ref(), &stack)
                .ok_or(InventoryError::InvalidItem)?;
            set_online_item(
                &player.inventory,
                &player.ender_chest_inventory,
                update,
                stack,
            );
            // Screen handlers use the host's packet serializers and synchronize an open
            // container too. Serializing host components directly in the DLL is unsafe.
            player.sync_inventory_to_client();
        } else {
            let directory = data_directory(&self.server);
            let mut data = player_data::read(&directory, self.uuid).map_err(PlayerError::from)?;
            set_saved_item(&mut data, update, &stack);
            player_data::write(&directory, self.uuid, data).map_err(PlayerError::from)?;
        }
        Ok(())
    }
}

fn set_online_item(
    inventory: &PlayerInventory,
    ender_chest: &EnderChestInventory,
    update: &InventoryUpdate,
    stack: PumpkinItemStack,
) {
    match update.inventory_type {
        InventoryType::Main => inventory.set_slot(update.item.slot, stack),
        InventoryType::Equipments => inventory.set_slot(EQUIPMENT[update.item.slot].1, stack),
        InventoryType::EnderChest => ender_chest.set_stack(update.item.slot, stack),
    }
}

fn set_saved_item(data: &mut NbtCompound, update: &InventoryUpdate, stack: &PumpkinItemStack) {
    let mut item = NbtCompound::new();
    stack.write_item_stack(&mut item);
    let (list_key, saved_slot, alias) = match update.inventory_type {
        InventoryType::Main => ("Inventory", update.item.slot as i8, update.item.slot as i8),
        InventoryType::EnderChest => ("EnderItems", update.item.slot as i8, update.item.slot as i8),
        InventoryType::Equipments => {
            let (key, native, saved) = EQUIPMENT[update.item.slot];
            let mut equipment = data.get_compound("equipment").cloned().unwrap_or_default();
            if stack.is_empty() {
                equipment.child_tags.remove(key);
            } else {
                equipment.put_compound(key, item.clone());
            }
            data.put_compound("equipment", equipment);
            ("Inventory", saved, native as i8)
        }
    };
    let mut items = data.get_list(list_key).unwrap_or_default().to_vec();
    items.retain(|item| {
        item.extract_compound()
            .and_then(|item| item.get_byte("Slot"))
            .is_none_or(|slot| slot != saved_slot && slot != alias)
    });
    if !stack.is_empty() {
        item.put_byte("Slot", saved_slot);
        items.push(NbtTag::Compound(item));
    }
    data.put(list_key, NbtTag::List(items));
}

#[cfg(test)]
mod tests {
    use std::{
        fs,
        sync::{Arc, Mutex},
    };

    use pumpkin_inventory::{build_equipment_slots, entity_equipment::EntityEquipment};
    use serde_json::json;
    use uuid::Uuid;

    use super::*;

    fn update(
        kind: &str,
        slot: usize,
        id: &str,
        count: i32,
        components: Option<&str>,
    ) -> InventoryUpdate {
        serde_json::from_value(json!({
            "inventoryType": kind,
            "item": {"slot": slot, "id": id, "count": count, "snbt": components}
        }))
        .unwrap()
    }

    fn native_inventory() -> PlayerInventory {
        PlayerInventory::new(
            Arc::new(Mutex::new(EntityEquipment::new())),
            Arc::new(build_equipment_slots()),
        )
    }

    #[test]
    fn online_and_offline_slots_round_trip_through_pumpkins_player_loader() {
        let online = native_inventory();
        let ender = EnderChestInventory::new();
        let mut saved = NbtCompound::new();
        for (kind, size) in [("main", 36), ("equipments", 5), ("enderChest", 27)] {
            for slot in 0..size {
                let edit = update(kind, slot, "minecraft:stone", slot as i32 + 1, None);
                let stack = edit.to_native().unwrap();
                let host_stack = item::copy_to_host(&online, &stack).unwrap();
                set_online_item(&online, &ender, &edit, host_stack);
                set_saved_item(&mut saved, &edit, &stack);
            }
        }
        let mut live = NbtCompound::new();
        online.write_nbt(&mut live);
        ender.write_nbt(&mut live);
        let snapshot = PlayerInventorySnapshot::from_nbt(&saved);
        assert_eq!(snapshot.hash, PlayerInventorySnapshot::from_nbt(&live).hash);
        for (contents, size) in [
            (&snapshot.main, 36),
            (&snapshot.equipments, 5),
            (&snapshot.ender_chest, 27),
        ] {
            assert_eq!(contents.size, size);
            assert_eq!(contents.items.len(), size);
            for (slot, item) in contents.items.iter().enumerate() {
                assert_eq!(item.slot, slot);
                assert_eq!(item.count, slot as i32 + 1);
            }
        }
        // Check against native indices, not just the panel's own reverse mapping.
        for (native, count) in [(39, 1), (38, 2), (37, 3), (36, 4), (40, 5)] {
            assert_eq!(online.get_slot(native).item_count, count);
        }
        let reloaded = native_inventory();
        reloaded.read_nbt_non_mut(&saved);
        let reloaded_ender = EnderChestInventory::new();
        reloaded_ender.read_nbt_non_mut(&saved);
        let mut reloaded_data = NbtCompound::new();
        reloaded.write_nbt(&mut reloaded_data);
        reloaded_ender.write_nbt(&mut reloaded_data);
        assert_eq!(
            snapshot.hash,
            PlayerInventorySnapshot::from_nbt(&reloaded_data).hash
        );
    }

    #[test]
    fn offline_edits_preserve_unrelated_data_and_clearing_removes_all_equipment_copies() {
        let mut data = snbt::parse_compound(
            r#"{XpLevel:42,Inventory:[{Slot:0b,id:"minecraft:stone",count:4,extra:"keep"},{Slot:103b,id:"minecraft:diamond_helmet",count:1},{Slot:39b,id:"minecraft:iron_helmet",count:1}],equipment:{head:{id:"minecraft:golden_helmet",count:1},body:{id:"minecraft:leather_horse_armor",count:1}},EnderItems:[{Slot:26b,id:"minecraft:diamond",count:3}]}"#,
        ).unwrap();
        assert_eq!(
            PlayerInventorySnapshot::from_nbt(&data).equipments.items[0]
                .id
                .as_deref(),
            Some("minecraft:golden_helmet")
        );
        let original = data.clone();
        let clear = update("equipments", 0, "minecraft:air", 0, None);
        set_saved_item(&mut data, &clear, &clear.to_native().unwrap());
        let root =
            crate::utils::file::random_temporary_path(&std::env::temp_dir(), "inventory").unwrap();
        fs::create_dir(&root).unwrap();
        let uuid = Uuid::from_u128(1);
        player_data::write(&root, uuid, data).unwrap();
        let data = player_data::read(&root, uuid).unwrap();
        assert_eq!(data.get_int("XpLevel"), Some(42));
        assert_eq!(data.get_list("EnderItems"), original.get_list("EnderItems"));
        assert_eq!(
            data.get_list("Inventory").unwrap(),
            &original.get_list("Inventory").unwrap()[..1]
        );
        assert_eq!(
            data.get_compound("equipment").unwrap().get_compound("body"),
            original
                .get_compound("equipment")
                .unwrap()
                .get_compound("body")
        );
        assert!(
            data.get_compound("equipment")
                .unwrap()
                .get_compound("head")
                .is_none()
        );
        let native = native_inventory();
        native.read_nbt_non_mut(&data);
        assert!(native.get_slot(39).is_empty());
        assert_eq!(
            PlayerInventorySnapshot::from_nbt(&data).equipments.items[0].count,
            0
        );
        fs::remove_file(root.join(format!("{uuid}.dat"))).unwrap();
        fs::remove_dir(root).unwrap();
    }

    #[test]
    fn item_components_survive_edits_and_hashes_ignore_compound_order() {
        let components =
            r#"{"minecraft:custom_data":{text:"line\n\"quoted\"",a:1b},"minecraft:damage":3}"#;
        let edit = update("main", 35, "minecraft:diamond_sword", 1, Some(components));
        let stack = edit.to_native().unwrap();
        let stack = item::copy_to_host(&native_inventory(), &stack).unwrap();
        let mut data = NbtCompound::new();
        set_saved_item(&mut data, &edit, &stack);
        let snapshot = PlayerInventorySnapshot::from_nbt(&data);
        let snbt = snapshot.main.items[35].snbt.as_deref().unwrap();
        assert_eq!(snbt::parse_compound(snbt), snbt::parse_compound(components));
        let reordered = update(
            "main",
            35,
            "minecraft:diamond_sword",
            1,
            Some(
                r#"{"minecraft:damage":3,"minecraft:custom_data":{a:1b,text:"line\n\"quoted\""}}"#,
            ),
        );
        set_saved_item(&mut data, &reordered, &reordered.to_native().unwrap());
        assert_eq!(snapshot.hash, PlayerInventorySnapshot::from_nbt(&data).hash);
        let damaged = update(
            "main",
            35,
            "minecraft:diamond_sword",
            1,
            Some(r#"{"minecraft:damage":4}"#),
        );
        set_saved_item(&mut data, &damaged, &damaged.to_native().unwrap());
        assert_ne!(snapshot.hash, PlayerInventorySnapshot::from_nbt(&data).hash);
    }

    #[test]
    fn rejects_invalid_slots_items_counts_and_components_before_mutation() {
        for edit in [
            update("main", 36, "minecraft:stone", 1, None),
            update("equipments", 5, "minecraft:stone", 1, None),
            update("enderChest", 27, "minecraft:stone", 1, None),
            update("main", 0, "minecraft:nonexistent_item", 1, None),
            update("main", 0, "minecraft:stone", 256, None),
            update("main", 0, "minecraft:stone", 1, Some("{broken")),
            update("main", 0, "minecraft:stone", 1, Some("[]")),
            update("main", 0, "minecraft:stone", 1, Some("{} trailing")),
            update(
                "main",
                0,
                "minecraft:stone",
                1,
                Some(r#"{"minecraft:unknown_component":1}"#),
            ),
            update(
                "main",
                0,
                "minecraft:stone",
                1,
                Some(r#"{"minecraft:damage":"invalid"}"#),
            ),
        ] {
            assert!(
                matches!(edit.to_native(), Err(InventoryError::InvalidItem)),
                "{edit:?}"
            );
        }
        assert!(
            update("main", 0, "minecraft:stone", 0, None)
                .to_native()
                .unwrap()
                .is_empty()
        );
        assert_eq!(
            update("main", 0, "minecraft:stone", 255, None)
                .to_native()
                .unwrap()
                .item_count,
            255
        );
    }
}
