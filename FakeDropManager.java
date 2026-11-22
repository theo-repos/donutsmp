package net.donutsim.entity;

import net.donutsim.util.ChatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakeDropManager {
    private static final List<FakeDropEntry> ACTIVE_DROPS = new ArrayList<>();
    private static final AtomicInteger NEXT_FAKE_ENTITY_ID = new AtomicInteger(-900_000);
    private static final Random RANDOM = new Random();

    private record FakeDropEntry(ItemEntity entity, int spawnTick) {}

    private FakeDropManager() {}

    public static boolean spawnItem(ResourceLocation itemId, int count) {
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        if (item == Items.AIR) {
            return false;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, Math.min(count, 64)));
        spawnStackInFront(stack, 0.0);
        return true;
    }

    public static void spawnKit(String kitName) {
        switch (kitName.toLowerCase()) {
            case "spawner" -> {
                spawnStackInFront(namedStack(Items.SPAWNER, 4, "Skeleton Spawner", ChatFormatting.AQUA), 0.14);
                spawnStackInFront(namedStack(Items.SPAWNER, 2, "Blaze Spawner", ChatFormatting.GOLD), 0.16);
                spawnStackInFront(namedStack(Items.AMETHYST_SHARD, 64, "Donut Shard", ChatFormatting.LIGHT_PURPLE), 0.18);
                spawnStackInFront(namedStack(Items.AMETHYST_SHARD, 64, "Donut Shard", ChatFormatting.LIGHT_PURPLE), 0.18);
            }
            case "pvp" -> {
                spawnStackInFront(new ItemStack(Items.TOTEM_OF_UNDYING, 1), 0.15);
                spawnStackInFront(new ItemStack(Items.TOTEM_OF_UNDYING, 1), 0.15);
                spawnStackInFront(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 16), 0.15);
                spawnStackInFront(enchantedGlint(Items.NETHERITE_SWORD, 1), 0.16);
                spawnStackInFront(enchantedGlint(Items.NETHERITE_HELMET, 1), 0.16);
                spawnStackInFront(enchantedGlint(Items.NETHERITE_CHESTPLATE, 1), 0.16);
                spawnStackInFront(enchantedGlint(Items.NETHERITE_LEGGINGS, 1), 0.16);
                spawnStackInFront(enchantedGlint(Items.NETHERITE_BOOTS, 1), 0.16);
            }
            case "keyall" -> {
                spawnStackInFront(namedStack(Items.TRIPWIRE_HOOK, 3, "Donut Key", ChatFormatting.AQUA), 0.15);
                spawnStackInFront(namedStack(Items.TRIPWIRE_HOOK, 2, "Spawner Key", ChatFormatting.LIGHT_PURPLE), 0.16);
                spawnStackInFront(namedStack(Items.TRIPWIRE_HOOK, 1, "Amethyst Key", ChatFormatting.GOLD), 0.17);
            }
        }
    }

    private static ItemStack namedStack(Item item, int count, String customName, ChatFormatting color) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(customName).withStyle(color, ChatFormatting.BOLD));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private static ItemStack enchantedGlint(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    public static void spawnStackInFront(ItemStack stack, double spread) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-4) {
            horizontalLook = new Vec3(0.0, 0.0, 1.0);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        double spawnX = player.getX() + horizontalLook.x * 1.4;
        double spawnY = player.getEyeY() - 0.3;
        double spawnZ = player.getZ() + horizontalLook.z * 1.4;

        ItemEntity itemEntity = new ItemEntity(level, spawnX, spawnY, spawnZ, stack.copy());
        int fakeId = NEXT_FAKE_ENTITY_ID.decrementAndGet();
        itemEntity.setId(fakeId);
        itemEntity.setGlowingTag(true);
        itemEntity.setNoPickUpDelay();

        double jitterX = (RANDOM.nextDouble() - 0.5) * spread;
        double jitterZ = (RANDOM.nextDouble() - 0.5) * spread;
        itemEntity.setDeltaMovement(
                horizontalLook.x * 0.24 + jitterX,
                0.22 + RANDOM.nextDouble() * 0.05,
                horizontalLook.z * 0.24 + jitterZ
        );

        level.addEntity(itemEntity);
        ACTIVE_DROPS.add(new FakeDropEntry(itemEntity, player.tickCount));
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            ACTIVE_DROPS.clear();
            return;
        }

        AABB pickupBox = player.getBoundingBox().inflate(1.15, 0.75, 1.15);
        Iterator<FakeDropEntry> it = ACTIVE_DROPS.iterator();
        while (it.hasNext()) {
            FakeDropEntry entry = it.next();
            ItemEntity entity = entry.entity();

            if (entity.isRemoved() || level.getEntity(entity.getId()) == null) {
                it.remove();
                continue;
            }

            // Despawn after 5 minutes (6000 ticks)
            int ageTicks = player.tickCount - entry.spawnTick();
            if (ageTicks > 6000) {
                level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
                it.remove();
                continue;
            }

            // Allow 18 ticks (~0.9s) throw grace period before picking up
            if (ageTicks >= 18 && entity.getBoundingBox().intersects(pickupBox)) {
                ItemStack picked = entity.getItem();
                Component itemName = picked.getHoverName();
                int count = picked.getCount();

                level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
                it.remove();

                ChatHelper.playPickupSound();
                ChatHelper.sendActionBar(
                        Component.literal("+ " + count + "x ").withStyle(ChatFormatting.GREEN)
                                .append(itemName.copy())
                );
            }
        }
    }

    public static void clearAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (FakeDropEntry entry : ACTIVE_DROPS) {
                mc.level.removeEntity(entry.entity().getId(), Entity.RemovalReason.DISCARDED);
            }
        }
        ACTIVE_DROPS.clear();
    }
}
