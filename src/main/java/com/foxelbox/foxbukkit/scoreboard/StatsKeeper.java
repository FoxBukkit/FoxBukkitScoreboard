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

import com.webkonsept.minecraft.lagmeter.LagMeter;
import com.webkonsept.minecraft.lagmeter.exceptions.NoAvailableTPSException;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.UUID;

public class StatsKeeper {
    private static final String FBSTATS_SIDEBAR = "fbstats_sbr";
    private static final String FBSTATS_SIDEBAR_ALT = "fbstats_sbr2";
    private static final String FBSTATS_PINGINFO = "fbstats_ping";

    private HashMap<UUID, Objective> statsSidebar = new HashMap<>();
    private HashMap<UUID, Objective> statsBelowName = new HashMap<>();

    private final FoxBukkitScoreboard plugin;
    StatsKeeper(FoxBukkitScoreboard plugin) {
        this.plugin = plugin;
    }

    void refreshAllStats() {
        for(Player ply : plugin.getServer().getOnlinePlayers()) {
            refreshStats(ply);
        }
    }

    void onDisconnect(Player ply) {
        statsSidebar.remove(ply.getUniqueId());
        statsBelowName.remove(ply.getUniqueId());
    }

    void onDisable() {
        statsSidebar.clear();
        statsBelowName.clear();
    }

    private int getPlayerPing(final Player player) {
        return ((CraftPlayer)player).getHandle().ping;
    }

    void refreshStats(Player player) {
        Scoreboard scoreboard = plugin.playerStatsScoreboards.get(player.getUniqueId());

        final Objective oldSidebar = statsSidebar.get(player.getUniqueId());
        final String sidebarName = (oldSidebar != null && oldSidebar.getName().equals(FBSTATS_SIDEBAR)) ? FBSTATS_SIDEBAR_ALT : FBSTATS_SIDEBAR;
        Objective sidebar = scoreboard.registerNewObjective(sidebarName, sidebarName);
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebar.setDisplayName("\u00a7dFoxel\u00a75Box");
        statsSidebar.put(player.getUniqueId(), sidebar);

        Objective belowName = statsBelowName.get(player.getUniqueId());
        if(belowName == null) {
            belowName = scoreboard.registerNewObjective(FBSTATS_PINGINFO, FBSTATS_PINGINFO);
            belowName.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            belowName.setDisplayName("ms");
            statsBelowName.put(player.getUniqueId(), belowName);
        }

        String tps;
        try {
            final double tpsNumber = LagMeter.getInstance().getTPS();
            final char tpsColor;
            if(tpsNumber < 15D) {
                tpsColor = 'c';
            } else if(tpsNumber < 18D) {
                tpsColor = 'e';
            } else {
                tpsColor = 'a';
            }
            tps = String.format("\u00a7%c%.1f", tpsColor, tpsNumber);
        } catch (NoAvailableTPSException e) {
            tps = "N/A";
        }

        final String rank = plugin.permissionHandler.getGroup(player);
        final String rankTag = plugin.permissionHandler.getGroupTag(rank);

        sidebar.getScore("\u00a72\u00a7lTPS: \u00a7r" + tps).setScore(3);
        sidebar.getScore("\u00a74\u00a7lRank: \u00a7r" + rankTag + rank).setScore(2);
        sidebar.getScore("\u00a71\u00a7lPing: \u00a7r" + getPlayerPing(player) + "ms").setScore(1);

        for(Player ply : plugin.getServer().getOnlinePlayers()) {
            belowName.getScore(ply.getName()).setScore(getPlayerPing(ply));
        }

        if(oldSidebar != null) {
            oldSidebar.unregister();
        }
    }
}
