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

import java.util.Iterator;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class Timer implements Runnable {
    private Main main;
    public Timer(Main plugin) { main = plugin; }

    public void run() {
        if (main.core.flying.isEmpty()) { return; }

        Iterator<UUID> iter = main.core.flying.keySet().iterator();
        while (iter.hasNext()) {
            UUID id = iter.next();
            Player player = Bukkit.getPlayer(id);

            // Should already be handled by onQuit, but don't let a stale
            // entry crash the loop if one somehow slips through
            if (player == null) { continue; }

            // If they aren't in the correct mode, don't adjust anything
            if (! main.correctMode(player)) { continue; }

            // They moved to a world where flight is disabled and don't have the anyworld permission
            if (main.core.isFlying(player) && main.core.disabledWorlds.contains(player.getWorld().getName()) && ! main.hasPermission(player, "anyworld")) {
                main.core.setFalling(player, true);
                main.core.setFlying(player, false);
                player.setAllowFlight(false);
                main.core.messagePlayer(player, "disabled", null);
            }

            // Check if they have "landed" to disable fall protection
            if (main.core.isFalling(player)) {
                if (isOnGround(player)) {
                    main.core.setFalling(player, false);
                }
            }

            // Check if the players "wings" have "healed"
            if (main.core.isBroken(player)) {
                if (main.core.getBrokenCount(player) > 1) {
                    main.core.decreaseBrokenCounter(player, 0.50);
                }
                else {
                    main.core.removeBroken(player);
                    player.setAllowFlight(true);
                    main.core.setFlying(player, true);
                    main.core.messagePlayer(player, "wHealed", null);
                }
            }

            // Check and update the players fuel counter
            if (main.core.isFlying(player) && ! main.hasPermission(player, "nofuel")) {
                if (! main.core.hasFuelCount(player)) {
                        FuelType fuel = main.core.findFuel(player, main.fuels);
                        if (fuel != null) {
                            // -1 because Timer adds a second back at 0
                            main.core.increaseFuelCount(player, fuel.seconds() - 1);
                            main.core.removeFuel(player, fuel.material());
                            if (main.core.findFuel(player, main.fuels) == null) { main.core.messagePlayer(player, "fLast", null); }
                        }
                        else {
                            main.core.messagePlayer(player, "fOut", null);
                            main.core.setFalling(player, true);
                            main.core.setFlying(player, false);
                            player.setAllowFlight(false);
                        }
                }
                // Update the players remaining time
                else {
                    Double fuelMultiplier = 1.0;
                    if (isOnGround(player)) {
                        fuelMultiplier = main.getConfig().getDouble("groundFuel");
                    }
                    else if (main.getConfig().getBoolean("speedFuel")) {
                        Float f = player.getFlySpeed();
                        fuelMultiplier = f.doubleValue() * 10;
                    }

                    main.core.decreaseFuelCount(player, (0.50 * fuelMultiplier));
                }
            }
        }
    }

    // Whether the block directly beneath the player is solid ground
    // (not just non-air - grass, flowers, signs etc. shouldn't count as landed)
    private boolean isOnGround(Player player) {
        Location loc = player.getLocation();
        Location block = new Location(player.getWorld(), loc.getBlockX(), Math.ceil(loc.getY()) - 1, loc.getBlockZ());
        return block.getBlock().getType().isSolid();
    }
}
