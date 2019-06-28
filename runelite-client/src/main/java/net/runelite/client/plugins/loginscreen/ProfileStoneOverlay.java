package net.runelite.client.plugins.loginscreen;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class ProfileStoneOverlay extends Overlay {
	
	@Setter
	private Point position = null;
	
	@Inject
	private LoginScreenPlugin plugin;
	
	@Inject
	private Client client;
	
	@Inject
	private SpriteManager spriteManager;
	
	@Getter
	private final int profileIndex;
	
	private TextComponent displayNameComponent = new TextComponent();
	private TextComponent worldComponent = new TextComponent();
	
	@Getter
	private Rectangle bounds;
	
	protected ProfileStoneOverlay(int profileIndex)
	{
		this.profileIndex = profileIndex;
	}
	
	@Override
	public Dimension render(Graphics2D graphics) {
		if(plugin.getProfileAtSlot(profileIndex) == null)
		{
			return null;
		}
		QuickLoginProfile profile = plugin.getProfileAtSlot(profileIndex);
		BufferedImage stoneSprite = spriteManager.getSprite(SpriteID.LOGIN_SCREEN_BUTTON_BACKGROUND, 0);
		if(stoneSprite == null)
			return null;
		graphics.drawImage(stoneSprite, position.x, position.y, null);
		String worldText = "World "+profile.getWorld();
		int displayWidth = (int) graphics.getFontMetrics().getStringBounds(profile.getDisplay(), graphics).getWidth();
		int worldWidth = (int) graphics.getFontMetrics().getStringBounds(worldText, graphics).getWidth();
		
		int stoneMidHeight = stoneSprite.getHeight() / 2;
		
		int centerX = stoneSprite.getWidth() / 2;
		centerX += position.x;
		
		
		displayNameComponent.setPosition(new Point(centerX - (displayWidth / 2), position.y + stoneMidHeight));
		worldComponent.setPosition(new Point(centerX - (worldWidth / 2), position.y + stoneMidHeight + 13));
		
		displayNameComponent.setText(profile.getDisplay());
		worldComponent.setText(worldText);
		
		displayNameComponent.render(graphics);
		worldComponent.render(graphics);
		
		this.bounds = new Rectangle(position.x, position.y, stoneSprite.getWidth(), stoneSprite.getHeight());
		
		return null;
	}
	
}
