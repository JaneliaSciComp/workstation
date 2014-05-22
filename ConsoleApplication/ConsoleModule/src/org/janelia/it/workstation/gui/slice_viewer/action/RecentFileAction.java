package org.janelia.it.workstation.gui.slice_viewer.action;

import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;

/**
 * Menu item to load a particular file from a URL
 * 
 * @author brunsc
 *
 */
public class RecentFileAction extends AbstractAction 
{
	private static final long serialVersionUID = 1L;
	// private static final Logger log = LoggerFactory.getLogger(RecentFileAction.class);
	
	private URL url;
	private org.janelia.it.workstation.signal.Signal1<URL> openFileRequestedSignal = new org.janelia.it.workstation.signal.Signal1<URL>();

	RecentFileAction(URL url) {
		setUrl(url);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RecentFileAction other = (RecentFileAction) obj;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	public URL getUrl() {
		return url;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	public void setUrl(URL url) {
		this.url = url;
		putValue(NAME, url.toString());
		putValue(SHORT_DESCRIPTION, "Load image from " + url.toString());
	}

	public org.janelia.it.workstation.signal.Signal1<URL> getOpenFileRequestedSignal() {
		return openFileRequestedSignal;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// log.info("requesting load of "+url);
		getOpenFileRequestedSignal().emit(url);
	}

}
