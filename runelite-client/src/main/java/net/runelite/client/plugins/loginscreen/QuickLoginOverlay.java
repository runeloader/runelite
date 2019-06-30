package net.runelite.client.plugins.loginscreen;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
public class QuickLoginOverlay extends Overlay {
	
	private final Client client;
	private final SpriteManager spriteManager;
	
	@Getter
	private List<ProfileStoneOverlay> stoneOverlays = Lists.newArrayList();
	
	private final LoginScreenPlugin plugin;
	private TextComponent quickLoginText = new TextComponent();
	
	@Inject
	QuickLoginOverlay(LoginScreenPlugin plugin, Injector injector, Client client, SpriteManager spriteManager)
	{
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.LOGIN_SCREEN);
		this.client = client;
		this.spriteManager = spriteManager;
		for(int i = 0; i < plugin.getProfileCount(); i++)
		{
			ProfileStoneOverlay overlay = new ProfileStoneOverlay(i);
			injector.injectMembers(overlay);
			stoneOverlays.add(overlay);
		}
		this.quickLoginText.setColor(Color.YELLOW);
		this.quickLoginText.setText("Quick Logins");
	}
	
	@Override
	public Dimension render(Graphics2D graphics) {
		if(client.getLoginIndex() < 2)
			return null;
		
		BufferedImage stoneSprite = spriteManager.getSprite(SpriteID.LOGIN_SCREEN_BUTTON_BACKGROUND, 0);
		if(stoneSprite == null)
			return null;
		
		int padding = 20;
		int activeCount = plugin.getActiveProfileCount();
		
		int midX =  client.getCanvasWidth() / 2;
		int y = 400;
		
		int totalWidth = (stoneSprite.getWidth(null) * activeCount);
		if(activeCount > 1)
		{
			totalWidth += (activeCount - 1) * padding;
		}
		
		
		int startX = midX - (totalWidth / 2);
		
		for(int index = 0; index < plugin.getProfileCount(); index++)
		{
			ProfileStoneOverlay stoneOverlay = this.stoneOverlays.get(index);
			stoneOverlay.setPosition(new Point(startX, y));
			stoneOverlay.render(graphics);
			
			startX += stoneSprite.getWidth(null);
			startX += padding;
			
		}
		this.quickLoginText.setText("Quick Logins");
		int displayWidth = (int) graphics.getFontMetrics().getStringBounds(quickLoginText.getText(), graphics).getWidth();
		quickLoginText.setPosition(new Point(midX - (displayWidth / 2), y - 5));
		quickLoginText.render(graphics);
		
		return null;
	}
}
