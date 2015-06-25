/**
 * This file is part of FoxBukkitLua.
 *
 * FoxBukkitLua is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoxBukkitLua is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FoxBukkitLua.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.foxelbox.foxbukkit.scoreboard;

import com.foxelbox.dependencies.config.Configuration;
import com.foxelbox.dependencies.redis.CacheMap;
import com.foxelbox.dependencies.redis.RedisManager;
import com.foxelbox.dependencies.threading.SimpleThreadCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class FoxBukkitScoreboard extends JavaPlugin {
    public Configuration configuration;
    public RedisManager redisManager;

    @Override
    public void onDisable() {
        redisManager.stop();
    }

    private Map<String,String> rankTags;
    private Map<String,String> playerRanks;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        configuration = new Configuration(getDataFolder());
        redisManager = new RedisManager(new SimpleThreadCreator(), configuration);

        playerRanks = redisManager.createCachedRedisMap("playergroups").addOnChangeHook(new CacheMap.OnChangeHook() {
            @Override
            public void onEntryChanged(String key, String value) {
                UUID uuid = UUID.fromString(key);
                Player ply = getServer().getPlayer(uuid);
                if(ply != null) {
                    setPlayerScoreboardTeam(ply, value);
                }
            }
        });

        rankTags = redisManager.createCachedRedisMap("ranktags");
    }

    private final ArrayList<Scoreboard> registeredScoreboards = new ArrayList<>();
    private boolean mainScoreboardRegistered = false;

    public void setPlayerScoreboardTeam(Player ply) {
        setPlayerScoreboardTeam(ply, playerRanks.get(ply.getUniqueId().toString()));
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
                team.setPrefix(rankTags.get(rank));
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
