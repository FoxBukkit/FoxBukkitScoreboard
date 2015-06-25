/**
 * This file is part of FoxBukkitScoreboard.
 *
 * FoxBukkitScoreboard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitScoreboard is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitScoreboard.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.scoreboard;

import com.foxelbox.foxbukkit.permissions.FoxBukkitPermissionHandler;
import com.foxelbox.foxbukkit.permissions.FoxBukkitPermissions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.UUID;

public class FoxBukkitScoreboard extends JavaPlugin implements Listener {
    private FoxBukkitPermissions permissions;
    private FoxBukkitPermissionHandler permissionHandler;

    @Override
    public void onDisable() {
        super.onDisable();
        registeredScoreboards.clear();
    }

    @Override
    public void onEnable() {
        permissions = (FoxBukkitPermissions)getServer().getPluginManager().getPlugin("FoxBukkitPermissions");
        permissionHandler = permissions.getHandler();

        permissionHandler.addRankChangeHandler(new FoxBukkitPermissionHandler.OnRankChange() {
            @Override
            public void rankChanged(UUID uuid, String rank) {
                Player ply = getServer().getPlayer(uuid);
                if (ply != null) {
                    setPlayerScoreboardTeam(ply, rank);
                }
            }
        });

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        setPlayerScoreboardTeam(event.getPlayer());
    }

    private final ArrayList<Scoreboard> registeredScoreboards = new ArrayList<>();
    private boolean mainScoreboardRegistered = false;

    public void setPlayerScoreboardTeam(Player ply) {
        setPlayerScoreboardTeam(ply, permissionHandler.getGroup(ply.getUniqueId()));
    }

    public void setPlayerScoreboardTeam(Player ply, String rank) {
        if(!mainScoreboardRegistered) {
            registeredScoreboards.add(getServer().getScoreboardManager().getMainScoreboard());
            mainScoreboardRegistered = true;
        }
        String sbEntry = ply.getName();
        for(Scoreboard scoreboard : registeredScoreboards) {
            boolean playerAlreadyInTeam = false;
            for(Team oldTeam : scoreboard.getTeams()) {
                if(oldTeam.getName().equals(rank)) {
                    if (oldTeam.hasEntry(sbEntry)) {
                        playerAlreadyInTeam = true;
                        break;
                    }
                } else {
                    oldTeam.removeEntry(sbEntry);
                }
            }
            if(playerAlreadyInTeam)
                continue;
            Team team = scoreboard.getTeam(rank);
            if(team == null) {
                team = scoreboard.registerNewTeam(rank);
                team.setPrefix(permissionHandler.getGroupTag(rank));
                team.setSuffix("\u00a7r");
            }
            team.addPlayer(ply);
        }
    }

    private void refreshScoreboards() {
        for(Player ply : getServer().getOnlinePlayers()) {
            setPlayerScoreboardTeam(ply);
        }
    }

    public Scoreboard createScoreboard() {
        Scoreboard scoreboard = getServer().getScoreboardManager().getNewScoreboard();
        registeredScoreboards.add(scoreboard);
        refreshScoreboards();
        return scoreboard;
    }
}
