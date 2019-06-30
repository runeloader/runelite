package net.runelite.client.plugins.loginscreen;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.input.MouseAdapter;

import javax.swing.SwingUtilities;
import java.awt.event.MouseEvent;

@Slf4j
public class QuickLoginMouseListener extends MouseAdapter {
	
	private final LoginScreenPlugin plugin;
	private final Client client;
	private final QuickLoginOverlay overlay;
	
	protected QuickLoginMouseListener(LoginScreenPlugin plugin, Client client, QuickLoginOverlay overlay)
	{
		this.plugin = plugin;
		this.client = client;
		this.overlay = overlay;
	}
	
	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (!SwingUtilities.isLeftMouseButton(event))
		{
			return event;
		}
		
		if(plugin.getProfileCount() == 0)
		{
			return event;
		}
		
		if(client.getGameState() != GameState.LOGIN_SCREEN)
		{
			return event;
		}
		
		for(ProfileStoneOverlay stone : overlay.getStoneOverlays())
		{
			if(plugin.getProfileAtSlot(stone.getProfileIndex()) == null)
			{
				continue;
			}
			if(stone.getBounds() == null)
			{
				continue;
			}
			if(stone.getBounds().contains(event.getPoint()))
			{
				log.info("Consuming event and quick logging in {}", stone.getProfileIndex());
				plugin.populateLoginFields(stone.getProfileIndex());
				event.consume();
				return event;
			}
		}
		
		return event;
	}
	
	
}
