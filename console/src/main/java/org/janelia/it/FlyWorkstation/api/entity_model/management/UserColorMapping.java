package org.janelia.it.FlyWorkstation.api.entity_model.management;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;

/**
 * A cycling color map which assigns distinct colors to users. The current user is always mapped to white.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UserColorMapping {

	private static final String[] colorCycle = { 
			"CBBEF7", // blue
			"BEF7DB", // mint
			"F7DBBE", // orange
			"EDC7D1", // pink
			"F7F5BE", // yellow
			"F3BEF7", // purple
			"E9F7BE", // green yellow
			"BED7F7", // aqua
			"C4F7BE", // green
			"BEF1F7" // cyan
	};

	private int currColor = 0;

	protected Color nextColor() {
		if (currColor >= colorCycle.length) {
			currColor = 0;
		}
		return Color.decode("#"+colorCycle[currColor++]);
	}
	
	protected Map<String, Color> userColors = new HashMap<String, Color>();
	
	/**
	 * Get a distinct color for the given username. The color will persist while this class is in memory, but it may 
	 * be different on the next run of the application. This is similar to how MS Word assigns colors to users in its
	 * Track Changes mode. 
	 * @param username
	 * @return
	 */
    public Color getColor(String username) {
    	if (username.equals(SessionMgr.getUsername())) {
    		return Color.white;
    	}
    	if (!userColors.containsKey(username)) {
    		userColors.put(username, nextColor());
    	}
    	return userColors.get(username);
    }
}
