////////////////////////////////////////////////////////////////////////////
// This file is part of BlazeFly.                                         //
//                                                                        //
// BlazeFly is free software: you can redistribute it and/or modify       //
// it under the terms of the GNU General Public License as published by   //
// the Free Software Foundation, either version 3 of the License, or      //
// (at your option) any later version.                                    //
//                                                                        //
// BlazeFly is distributed in the hope that it will be useful,            //
// but WITHOUT ANY WARRANTY; without even the implied warranty of         //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the           //
// GNU General Public License for more details.                           //
//                                                                        //
// You should have received a copy of the GNU General Public License      //
// along with BlazeFly. If not, see <http://www.gnu.org/licenses/>.       //
////////////////////////////////////////////////////////////////////////////

package com.bradleyjh.blazefly;

import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class Core {
    // All keyed by player UUID rather than the live Player object, since a
    // relog hands out a brand new Player instance and would otherwise orphan
    // whatever was stored against the old one.
    public ConcurrentHashMap<UUID, Boolean> flying = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, Double> fuel = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, Double> broken = new ConcurrentHashMap<>();
    private ConcurrentHashMap<UUID, Boolean> falling = new ConcurrentHashMap<>();
    public List<String> disabledWorlds;
    public File playersFile;
    public FileConfiguration players;
    public File stringsFile;
    public FileConfiguration strings;

    // Completely remove a player from the in-memory state
    public void clearPlayer(Player player) {
        UUID id = player.getUniqueId();
        flying.remove(id);
        fuel.remove(id);
        broken.remove(id);
        falling.remove(id);
    }

    // Persist one player's state to players.yml (called on quit/shutdown)
    public void storePlayer(Player player) {
        UUID id = player.getUniqueId();
        if (! flying.containsKey(id) && ! fuel.containsKey(id) && ! broken.containsKey(id) && ! falling.containsKey(id)) {
            return;
        }

        String path = id.toString();
        players.set(path + ".flying", flying.getOrDefault(id, false));
        players.set(path + ".fuel", fuel.getOrDefault(id, 0.0));
        players.set(path + ".broken", broken.getOrDefault(id, 0.0));
        players.set(path + ".falling", falling.getOrDefault(id, false));
        try { players.save(playersFile); } catch (IOException e) { return; }
    }

    // Restore a player's state from players.yml (for players joining)
    public void retrievePlayer(Player player) {
        String path = player.getUniqueId().toString();
        if (! players.isConfigurationSection(path)) { return; }
        ConfigurationSection section = players.getConfigurationSection(path);

        setFlying(player, section.getBoolean("flying"));
        increaseFuelCount(player, section.getDouble("fuel"));
        if (section.getDouble("broken") > 0.0) { setBrokenCounter(player, section.getDouble("broken")); }
        setFalling(player, section.getBoolean("falling"));
        if (isBroken(player)) { messagePlayer(player, "wResumed", null); }

        if (isFlying(player)) {
            player.setAllowFlight(true);
            player.setFlying(true);
            messagePlayer(player, "fResumed", null);
        }

        players.set(path, null);
        try { players.save(playersFile); } catch (IOException e) { return; }
    }

    // Store all tracked players in players.yml (for onDisable)
    public void storeAll() {
        Set<UUID> ids = new HashSet<>();
        ids.addAll(flying.keySet());
        ids.addAll(fuel.keySet());
        ids.addAll(broken.keySet());
        ids.addAll(falling.keySet());
        if (ids.isEmpty()) { return; }

        for (UUID id : ids) {
            String path = id.toString();
            players.set(path + ".flying", flying.getOrDefault(id, false));
            players.set(path + ".fuel", fuel.getOrDefault(id, 0.0));
            players.set(path + ".broken", broken.getOrDefault(id, 0.0));
            players.set(path + ".falling", falling.getOrDefault(id, false));
        }
        try { players.save(playersFile); } catch (IOException e) { return; }
    }

    // Restore all online players from players.yml (for onEnable, e.g. after a /reload)
    public void retrieveAll() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            retrievePlayer(player);
        }
    }

    // Send a configurable message to a command sender
    public void messagePlayer (CommandSender sender, String type, HashMap<String, String> keywords) {
        if (strings.contains(type)) {

            // Get the header and the string
            String message = strings.getString("header") + strings.getString(type);

            // Replace keywords if they were provided
            if (keywords != null) {
                Iterator<String> iter = keywords.keySet().iterator();
                while (iter.hasNext()) {
                    String keyword = iter.next();
                    String replacement = keywords.get(keyword);
                    message = message.replace(keyword, replacement);
                }
            }

            // Apply formatting
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    // Flying stuff
    public void setFlying(Player player, Boolean val) {
        flying.put(player.getUniqueId(), val);
    }
    public Boolean isFlying(Player player) {
        return flying.getOrDefault(player.getUniqueId(), false);
    }

    // Fuel counter stuff
    public void increaseFuelCount(Player player, Double val) {
        fuel.merge(player.getUniqueId(), val, Double::sum);
    }
    public void decreaseFuelCount(Player player, Double val) {
        fuel.merge(player.getUniqueId(), -val, Double::sum);
    }
    public Double getFuelCount(Player player) {
        return fuel.getOrDefault(player.getUniqueId(), 0.0);
    }
    public Boolean hasFuelCount(Player player) {
        return fuel.getOrDefault(player.getUniqueId(), 0.0) > 0;
    }

    // Falling stuff (Some hacky stuff to prevent occasional damage happening)
    public void setFalling(Player player, Boolean val) {
        falling.put(player.getUniqueId(), val);
    }
    public Boolean isFalling(Player player) {
        return falling.getOrDefault(player.getUniqueId(), false);
    }

    // Broken wings stuff
    public void setBrokenCounter(Player player, Double val) {
        broken.put(player.getUniqueId(), val);
    }
    public void decreaseBrokenCounter(Player player, Double val) {
        broken.merge(player.getUniqueId(), -val, Double::sum);
    }
    public Double getBrokenCount(Player player) {
        return broken.getOrDefault(player.getUniqueId(), 0.0);
    }
    public Boolean isBroken(Player player) {
        return broken.containsKey(player.getUniqueId());
    }
    public void removeBroken(Player player) {
        broken.remove(player.getUniqueId());
    }

    // Check if the player is carrying fuel, including the off-hand slot
    // (which PlayerInventory#getContents() does not include)
    public Boolean hasFuel(Player player, Material material) {
        PlayerInventory inv = player.getInventory();
        if (inv.getItemInOffHand().getType() == material) { return true; }

        for (ItemStack stack : inv.getContents()) {
            if (stack != null && stack.getType() == material) { return true; }
        }
        return false;
    }

    // Remove one fuel item from the player, checking off-hand first
    public void removeFuel(Player player, Material material) {
        PlayerInventory inv = player.getInventory();

        ItemStack offhand = inv.getItemInOffHand();
        if (offhand.getType() == material) {
            offhand.setAmount(offhand.getAmount() - 1);
            inv.setItemInOffHand(offhand.getAmount() > 0 ? offhand : null);
            return;
        }

        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) { continue; }

            stack.setAmount(stack.getAmount() - 1);
            inv.setItem(i, stack.getAmount() > 0 ? stack : null);
            return;
        }
    }
}
