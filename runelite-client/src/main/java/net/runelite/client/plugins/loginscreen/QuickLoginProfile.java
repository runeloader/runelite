package net.runelite.client.plugins.loginscreen;

import lombok.Data;

@Data
public class QuickLoginProfile {
	
	private String display, username, password;
	private int world;
	
}
