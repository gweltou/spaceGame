package com.gwel.spacegame.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.gwel.spacegame.SpaceGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "SpaceGame 0.2";
		config.width = 800;
		config.height = 600;
		//config.fullscreen = true;
		new LwjglApplication(new SpaceGame(), config);
	}
}
