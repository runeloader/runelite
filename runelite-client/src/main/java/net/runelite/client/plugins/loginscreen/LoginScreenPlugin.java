/*
 * Copyright (c) 2017, Seth <Sethtroll3@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.loginscreen;

import com.google.common.base.Strings;
import com.google.inject.Provides;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.OSType;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldClient;
import net.runelite.http.api.worlds.WorldResult;

@PluginDescriptor(
	name = "Login Screen",
	description = "Provides various enhancements for login screen"
)
@Slf4j
public class LoginScreenPlugin extends Plugin implements KeyListener {
	private static final int MAX_USERNAME_LENGTH = 254;
	private static final int MAX_PASSWORD_LENGTH = 20;
	private static final int MAX_PIN_LENGTH = 6;
	
	private QuickLoginProfile[] profiles = new QuickLoginProfile[3];
	
	@Inject
	private Client client;
	
	@Inject
	private LoginScreenConfig config;
	
	@Inject
	private OverlayManager overlayManager;
	
	@Inject
	private QuickLoginOverlay quickLoginOverlay;
	
	@Inject
	private KeyManager keyManager;
	
	@Inject
	private MouseManager mouseManager;
	
	private QuickLoginMouseListener quickLoginMouseListener;
	
	private String usernameCache;
	
	private WorldResult worldResult;
	
	@Inject
	private Robot robot;
	
	@Override
	protected void startUp() throws Exception {
		applyUsername();
		overlayManager.add(quickLoginOverlay);
		keyManager.registerKeyListener(this);
		
		this.worldResult = new WorldClient().lookupWorlds();
		this.quickLoginMouseListener = new QuickLoginMouseListener(this, client, quickLoginOverlay);
		
		mouseManager.registerMouseListener(quickLoginMouseListener);
		
	}
	
	@Override
	protected void shutDown() throws Exception {
		if (config.syncUsername()) {
			client.getPreferences().setRememberedUsername(usernameCache);
		}
		
		keyManager.unregisterKeyListener(this);
		mouseManager.unregisterMouseListener(quickLoginMouseListener);
		overlayManager.remove(quickLoginOverlay);
	}
	
	@Provides
	LoginScreenConfig getConfig(ConfigManager configManager) {
		return configManager.getConfig(LoginScreenConfig.class);
	}
	
	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (!config.syncUsername()) {
			return;
		}
		
		if (event.getGameState() == GameState.LOGIN_SCREEN) {
			applyUsername();
		} else if (event.getGameState() == GameState.LOGGED_IN) {
			String username = "";
			
			if (client.getPreferences().getRememberedUsername() != null) {
				username = client.getUsername();
			}
			
			if (config.username().equals(username)) {
				return;
			}
			
			log.debug("Saving username: {}", username);
			config.username(username);
		}
	}
	
	@Subscribe
	public void onSessionOpen(SessionOpen event) {
		// configuation for the account is available now, so update the username
		applyUsername();
	}
	
	protected void setProfile(int index, String display, String username, String password, int world)
	{
		QuickLoginProfile profile = new QuickLoginProfile();
		profile.setDisplay(display);
		profile.setUsername(username);
		profile.setPassword(password);
		profile.setWorld(world);
		this.profiles[index] = profile;
	}
	
	protected void populateLoginFields(int index)
	{
		if(index < 0 || index > this.profiles.length)
		{
			return;
		}
		QuickLoginProfile profile = this.profiles[index];
		if(profile == null)
		{
			return;
		}
		client.setUsername(profile.getUsername());
		client.setPassword(profile.getPassword());
		for(World world : worldResult.getWorlds()) {
			log.info("{}", world);
		}
		
		World world = worldResult.findWorld(profile.getWorld());
		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));
		client.changeWorld(rsWorld);
		
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		
	}
	
	protected QuickLoginProfile getProfileAtSlot(int index)
	{
		if(index < 0 || index > this.profiles.length)
		{
			return null;
		}
		return this.profiles[index];
	}
	
	public int getProfileCount()
	{
		return this.profiles.length;
	}
	
	public int getActiveProfileCount()
	{
		for(int i = this.profiles.length - 1; i >= 0; i--)
		{
			if(this.profiles[i] != null)
			{
				return i + 1;
			}
		}
		return 0;
	}

	private void applyUsername()
	{
		if (!config.syncUsername())
		{
			return;
		}

		GameState gameState = client.getGameState();
		if (gameState == GameState.LOGIN_SCREEN)
		{
			String username = config.username();

			if (Strings.isNullOrEmpty(username))
			{
				return;
			}

			// Save it only once
			if (usernameCache == null)
			{
				usernameCache = client.getPreferences().getRememberedUsername();
			}

			client.getPreferences().setRememberedUsername(username);
		}
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!config.pasteEnabled() || (
			client.getGameState() != GameState.LOGIN_SCREEN &&
			client.getGameState() != GameState.LOGIN_SCREEN_AUTHENTICATOR))
		{
			return;
		}

		// enable pasting on macOS with the Command (meta) key
		boolean isModifierDown = OSType.getOSType() == OSType.MacOS ? e.isMetaDown() : e.isControlDown();

		if (e.getKeyCode() == KeyEvent.VK_V && isModifierDown)
		{
			try
			{
				final String data = Toolkit
					.getDefaultToolkit()
					.getSystemClipboard()
					.getData(DataFlavor.stringFlavor)
					.toString()
					.trim();

				switch (client.getLoginIndex())
				{
					// Username/password form
					case 2:
						if (client.getCurrentLoginField() == 0)
						{
							// Truncate data to maximum username length if necessary
							client.setUsername(data.substring(0, Math.min(data.length(), MAX_USERNAME_LENGTH)));
						}
						else
						{
							// Truncate data to maximum password length if necessary
							client.setPassword(data.substring(0, Math.min(data.length(), MAX_PASSWORD_LENGTH)));
						}

						break;
					// Authenticator form
					case 4:
						// Truncate data to maximum OTP code length if necessary
						client.setOtp(data.substring(0, Math.min(data.length(), MAX_PIN_LENGTH)));
						break;
				}
			}
			catch (UnsupportedFlavorException | IOException ex)
			{
				log.warn("failed to fetch clipboard data", ex);
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}
}