use pumpkin_data::item_stack::ItemStack;
use pumpkin_inventory::Inventory;
use pumpkin_nbt::{compound::NbtCompound, tag::NbtTag};

/// Recreate a validated stack through an Inventory trait object supplied by the host.
/// DLL-local items/components have different Any TypeIds and must never be stored in
/// a host inventory. Its read_data vtable calls Pumpkin's own item/component factory.
pub(crate) fn copy_to_host(inventory: &dyn Inventory, stack: &ItemStack) -> Option<ItemStack> {
    let mut item = NbtCompound::new();
    stack.write_item_stack(&mut item);
    item.put_byte("Slot", 0);
    let mut data = NbtCompound::new();
    data.put("Items", NbtTag::List(vec![NbtTag::Compound(item)]));
    let mut items = [ItemStack::EMPTY.clone()];
    inventory.read_data(&data, &mut items);
    let [host_stack] = items;
    if stack.is_empty() || (!host_stack.is_empty() && host_stack.item.id == stack.item.id) {
        Some(host_stack)
    } else {
        None
    }
}
