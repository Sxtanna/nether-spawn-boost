package com.sxtanna.mc.nether;

import com.sxtanna.mc.nether.mods.SpawnBoostModule;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class NetherSpawnBoostPlugin extends JavaPlugin
{

	@NotNull
	private final SpawnBoostModule module = new SpawnBoostModule(this);


	@Override
	public void onLoad()
	{
		saveDefaultConfig();
	}

	@Override
	public void onEnable()
	{
		module.load();
	}

	@Override
	public void onDisable()
	{
		module.kill();
	}

}
