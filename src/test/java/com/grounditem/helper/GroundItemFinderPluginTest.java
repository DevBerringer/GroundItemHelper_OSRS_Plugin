package com.grounditem.helper;

import com.grounditem.helper.plugin.GroundItemFinderPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GroundItemFinderPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GroundItemFinderPlugin.class);
		RuneLite.main(args);
	}
}