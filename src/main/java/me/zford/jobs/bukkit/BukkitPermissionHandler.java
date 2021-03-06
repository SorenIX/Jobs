/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.zford.jobs.bukkit;

import java.util.HashMap;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import me.zford.jobs.Jobs;
import me.zford.jobs.PermissionHandler;
import me.zford.jobs.container.Job;
import me.zford.jobs.container.JobPermission;
import me.zford.jobs.container.JobProgression;
import me.zford.jobs.container.JobsPlayer;

public class BukkitPermissionHandler implements PermissionHandler {
    private JobsPlugin plugin;
    public BukkitPermissionHandler(JobsPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public void recalculatePermissions(JobsPlayer jPlayer) {
        Player player = plugin.getServer().getPlayer(jPlayer.getName());
        if (player == null)
            return;
        
        boolean changed = false;
        
        // remove old permissions
        String negativePermName = "jobs.players."+player.getName()+".negative";
        String positivePermName = "jobs.players."+player.getName()+".positive";
        Permission negativePermission = plugin.getServer().getPluginManager().getPermission(negativePermName);
        if (negativePermission != null) {
            plugin.getServer().getPluginManager().removePermission(negativePermission);
            changed = true;
        }
        
        Permission positivePermission = plugin.getServer().getPluginManager().getPermission(positivePermName);
        if (positivePermission != null) {
            plugin.getServer().getPluginManager().removePermission(positivePermission);
            changed = true;
        }
        
        List<JobProgression> progression = jPlayer.getJobProgression();
        // calculate new permissions
        HashMap<String, Boolean> negative = new HashMap<String, Boolean>();
        HashMap<String, Boolean> positive = new HashMap<String, Boolean>();
        if (progression.size() == 0) {
            Job job = Jobs.getNoneJob();
            if (job != null) {
                for (JobPermission perm : job.getPermissions()) {
                    if (perm.getLevelRequirement() <= 0) {
                        if (perm.getValue()) {
                            positive.put(perm.getNode(), true);
                        } else {
                            negative.put(perm.getNode(), false);
                        }
                    }
                }
            }
        } else {
            for (JobProgression prog : progression) {
                for (JobPermission perm : prog.getJob().getPermissions()) {
                    if (prog.getLevel() >= perm.getLevelRequirement()) {
                        if (perm.getValue()) {
                            positive.put(perm.getNode(), true);
                        } else {
                            negative.put(perm.getNode(), false);
                        }
                    }
                }
            }
        }
        
        // add new permissions (if applicable)
        if (negative.size() > 0) {
            plugin.getServer().getPluginManager().addPermission(new Permission(negativePermName, PermissionDefault.FALSE, negative));
            changed = true;
        }
        
        if (positive.size() > 0) {
            plugin.getServer().getPluginManager().addPermission(new Permission(positivePermName, PermissionDefault.FALSE, positive));
            changed = true;
        }
        
        // If the permissions changed, recalculate them
        if (!changed)
            return;
        
        // find old attachment
        PermissionAttachment attachment = null;
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (pai.getAttachment() != null && pai.getAttachment().getPlugin() instanceof JobsPlugin) {
                attachment = pai.getAttachment();
            }
        }
        
        // create if attachment doesn't exist
        if (attachment == null) {
            attachment = player.addAttachment(plugin);
            attachment.setPermission(negativePermName, true);
            attachment.setPermission(positivePermName, true);
        }
        
        // recalculate!
        player.recalculatePermissions();
    }
    
    @Override
    public void registerPermissions() {
        PluginManager pm = plugin.getServer().getPluginManager();
        for (World world : plugin.getServer().getWorlds()) {
            if (pm.getPermission("jobs.world."+world.getName().toLowerCase()) == null)
                pm.addPermission(new Permission("jobs.world."+world.getName().toLowerCase(), PermissionDefault.TRUE));
        }
        for (Job job : Jobs.getJobs()) {
            if (pm.getPermission("jobs.join."+job.getName().toLowerCase()) == null)
                pm.addPermission(new Permission("jobs.join."+job.getName().toLowerCase(), PermissionDefault.TRUE));
        }
    }

}
