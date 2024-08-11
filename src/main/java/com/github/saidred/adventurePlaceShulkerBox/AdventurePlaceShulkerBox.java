package com.github.saidred.adventurePlaceShulkerBox;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftMetaBlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class AdventurePlaceShulkerBox extends JavaPlugin {
  public AdventurePlaceShulkerBox(){
    this.plugin = this;
  }

  private final JavaPlugin plugin;
  private fileConfig config;
  private final Map<Player,BukkitTask> targetPlayerMap = new HashMap<>();
  private final Map<Location,BukkitTask> targetLocationMap = new HashMap<>();

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(new shulkerBoxPlaceEvent(),this);
    getConfig().options().copyDefaults(true);
    saveConfig();
    config = new fileConfig(getConfig());
  }

  @Override
  public void onDisable() {
    getServer().getPluginManager().disablePlugin(this);
  }

  public class shulkerBoxPlaceEvent implements Listener {
    @EventHandler
    public void onInteractEvent(PlayerInteractEvent event){
      Player player = event.getPlayer();
      if(!player.getGameMode().equals(GameMode.ADVENTURE)) return;

      if(!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

      EquipmentSlot hand = event.getHand();
      if(hand == null) return;

      ItemStack useItem;
      if(hand.equals(EquipmentSlot.HAND)){
        useItem = player.getInventory().getItemInMainHand();
      }else if(hand.equals(EquipmentSlot.OFF_HAND)){
        useItem = player.getInventory().getItemInOffHand();
      }else{
        return;
      }

      if(!useItem.getType().toString().endsWith("SHULKER_BOX")) return;

      ItemStack def = useItem.clone();

      ItemMeta meta = useItem.getItemMeta();
      CraftMetaBlockState cet = (CraftMetaBlockState) meta;
      BlockState bscet = cet.getBlockState();

      ItemStack canPlaceOn = Bukkit.getItemFactory().createItemStack(
              useItem.getType().getKey() + "[minecraft:can_place_on={predicates: [{}]}]");
      ItemMeta canMeta = canPlaceOn.getItemMeta();
      CraftMetaBlockState canMetaBlockState = (CraftMetaBlockState) canMeta;
      canMetaBlockState.setBlockState(bscet);

      useItem.setItemMeta(canMetaBlockState);

      targetPlayerMap.put(player, new shulkerBoxPlaceTask(player, def, useItem).runTask(plugin));
    }

    @EventHandler
    public void onPlaceEvent(BlockPlaceEvent event){
      Player player = event.getPlayer();
      if(!player.getGameMode().equals(GameMode.ADVENTURE)) return;
      if(!targetPlayerMap.containsKey(player))return;
      Block block = event.getBlockPlaced();
      targetPlayerMap.get(player).cancel();
      targetPlayerMap.remove(player);
      if(config.isPlaceMessage()) player.sendMessage(config.getPlaceMessage());
      //player.sendMessage(ChatColor.BLUE + "シュルカーボックス設置を確認");
      targetLocationMap.put(block.getLocation(), new shulkerBoxBreakTask(block,player).runTaskLater(plugin, config.getBreakTick()));
    }

    public static class shulkerBoxPlaceTask extends BukkitRunnable {
      private final Player player;
      private final ItemStack before;
      private final ItemStack after;

      shulkerBoxPlaceTask(Player player, ItemStack before, ItemStack after) {
        this.player = player;
        this.before = before;
        this.after = after;
      }

      @Override
      public void run() {
        Inventory inventory = player.getInventory();
        if(inventory.contains(after)) inventory.setItem(inventory.first(after),before);
      }
    }

    public class shulkerBoxBreakTask extends BukkitRunnable {
      private final Block block;
      private final Player player;
      shulkerBoxBreakTask(Block block, Player player){
        this.block = block;
        this.player = player;
      }

      @Override
      public void run() {
        breakShulkerBox(block, player);
      }
    }

    @EventHandler
    public void shulkerBoxOpenEvent(InventoryOpenEvent event){
      if(event.getInventory().getHolder() instanceof ShulkerBox box){
        Location location = box.getBlock().getLocation();
        if(!targetLocationMap.containsKey(location)) return;
        targetLocationMap.get(location).cancel();
      }
    }

    @EventHandler
    public void shulkerBoxCloseEvent(InventoryCloseEvent event){
      if(event.getInventory().getHolder() instanceof ShulkerBox box){
        breakShulkerBox(box.getBlock(), (Player) event.getPlayer());
      }
    }

    protected void breakShulkerBox(Block shulkerBox,Player player){
      if(!targetLocationMap.containsKey(shulkerBox.getLocation())) return;
      targetLocationMap.remove(shulkerBox.getLocation());
      shulkerBox.breakNaturally();
      if(config.isBreakMessage()) player.sendMessage(config.getBreakMassage());
      //player.sendMessage(ChatColor.GREEN + "シュルカーボックスをアイテム化しました");
    }
  }


  private static class fileConfig{
    private final FileConfiguration file;
    fileConfig(FileConfiguration file){
      this.file = file;
    }
    /*
    public int getBreakTick(){
      return file.getInt("shulkerBreakTick");
    }

    private String getMassage(String massage, String color){
      return (color.isEmpty() ? "" : ChatColor.valueOf(color)) + massage;
    }

    public boolean isPlaceMessage(){
      return file.getBoolean("shulkerPlace.information");
    }

    public String getPlaceMessage(){
      String color = file.getString("shulkerPlace.color");
      return getMassage(file.getString("shulkerPlace.massage"),color);
    }

    public boolean isBreakMessage(){
      return file.getBoolean("shulkerBreak.information");
    }

    public String getBreakMassage(){
      String color = file.getString("shulkerBreak.color");
      return getMassage(file.getString("shulkerBreak.massage"),color);
    }//*/

    public int getBreakTick(){
      return file.getInt("shulkerBreakTick");
    }

    public boolean isPlaceMessage(){
      return yamlString.PLACE.getInformation(file);
    }

    public String getPlaceMessage(){
      return yamlString.PLACE.getMassage(file);
    }

    public boolean isBreakMessage(){
      return yamlString.BREAK.getInformation(file);
    }

    public String getBreakMassage() {
      return yamlString.BREAK.getMassage(file);
    }

    private enum yamlString{
      PLACE("shulkerPlace"),
      BREAK("shulkerBreak");

      private enum valueName{
        INFO("information"),
        COLOR("color"),
        MSG("massage");

        private final String name;

        valueName(String name){
          this.name = name;
        }

        @Override
        public String toString() {
          return name;
        }
      }

      private final String sectionName;

      yamlString(String name){
        sectionName = name;
      }

      public String getMassage(FileConfiguration file) {
        String color = file.getString(sectionName + "." + valueName.COLOR);
        return (color.isEmpty() ? "" : ChatColor.valueOf(color)) + file.getString(sectionName + "." + valueName.MSG);
      }

      public boolean getInformation(FileConfiguration file){
        return file.getBoolean(sectionName + "." + valueName.INFO);
      }
    }
  }
}
