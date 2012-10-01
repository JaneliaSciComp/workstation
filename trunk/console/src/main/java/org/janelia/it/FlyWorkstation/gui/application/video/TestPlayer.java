/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2009, 2010, 2011, 2012 Caprica Software Limited.
 * 
 * /Volumes/jacsData/rokickiTest/mpeg/ConsolidatedSignal2.mp4
 * 
 * -Djna.library.path=/Applications/VLC.app/Contents/MacOS/lib
 * 
 */

package org.janelia.it.FlyWorkstation.gui.application.video;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.logger.Logger;
import uk.co.caprica.vlcj.player.*;
import uk.co.caprica.vlcj.player.embedded.DefaultFullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.windows.WindowsCanvas;

//import com.sun.awt.AWTUtilities;
import com.sun.jna.platform.WindowUtils;

/**
 * Simple test harness creates an AWT Window and plays a video.
 * <p>
 * This is <strong>very</strong> basic but should give you an idea of how to
 * build a media player.
 * <p>
 * In case you didn't realise, you can press F12 to toggle the visibility of the
 * player controls.
 * <p>
 * Java7 provides -Dsun.java2d.xrender=True or -Dsun.java2d.xrender=true, might
 * give some general performance improvements in graphics rendering.
 */
public class TestPlayer extends VlcjTest {

	private JFrame mainFrame;
	private Canvas videoSurface;
	private JPanel controlsPanel;
	private JPanel videoAdjustPanel;

	private MediaPlayerFactory mediaPlayerFactory;

	private EmbeddedMediaPlayer mediaPlayer;

	public static void main(final String[] args) throws Exception {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new TestPlayer(args);
			}
		});
	}

	public TestPlayer(String[] args) {
		if (RuntimeUtil.isWindows()) {
			// If running on Windows and you want the mouse/keyboard event
			// hack...
			videoSurface = new WindowsCanvas();
		} else {
			videoSurface = new Canvas();
		}

		Logger.debug("videoSurface={}", videoSurface);

		videoSurface.setBackground(Color.black);
		videoSurface.setSize(800, 600); // Only for initial layout

		// Since we're mixing lightweight Swing components and heavyweight AWT
		// components this is probably a good idea
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);

		List<String> vlcArgs = new ArrayList<String>();
		vlcArgs.add("--no-plugins-cache");
		vlcArgs.add("--no-video-title-show");
		vlcArgs.add("--no-snapshot-preview");
		vlcArgs.add("--quiet");
		vlcArgs.add("--quiet-synchro");
		vlcArgs.add("--intf");
		vlcArgs.add("dummy");
		Logger.debug("vlcArgs={}", vlcArgs);

		mainFrame = new JFrame("VLCJ Test Player");
		mainFrame.setIconImage(new ImageIcon(getClass().getResource("/images/fly.png")).getImage());

		FullScreenStrategy fullScreenStrategy = new DefaultFullScreenStrategy(mainFrame);

		mediaPlayerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));
		mediaPlayerFactory.setUserAgent("vlcj test player");

		List<AudioOutput> audioOutputs = mediaPlayerFactory.getAudioOutputs();
		Logger.debug("audioOutputs={}", audioOutputs);

		mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer(fullScreenStrategy);
		mediaPlayer.setVideoSurface(mediaPlayerFactory.newVideoSurface(videoSurface));
		mediaPlayer.setPlaySubItems(true);

		mediaPlayer.setEnableKeyInputHandling(false);
		mediaPlayer.setEnableMouseInputHandling(false);

		controlsPanel = new PlayerControlsPanel(mediaPlayer);
		videoAdjustPanel = new PlayerVideoAdjustPanel(mediaPlayer);

		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(videoSurface, BorderLayout.CENTER);
		mainFrame.add(controlsPanel, BorderLayout.SOUTH);
		mainFrame.add(videoAdjustPanel, BorderLayout.EAST);
		mainFrame.setJMenuBar(buildMenuBar());
		mainFrame.pack();
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				Logger.debug("windowClosing(evt={})", evt);

				if (videoSurface instanceof WindowsCanvas) {
					((WindowsCanvas) videoSurface).release();
				}

				if (mediaPlayer != null) {
					mediaPlayer.release();
					mediaPlayer = null;
				}

				if (mediaPlayerFactory != null) {
					mediaPlayerFactory.release();
					mediaPlayerFactory = null;
				}
			}
		});

		mainFrame.setVisible(true);

		mediaPlayer.addMediaPlayerEventListener(new TestPlayerMediaPlayerEventListener());

		// Won't work with OpenJDK or JDK1.7, requires a Sun/Oracle JVM
		// (currently)
		boolean transparentWindowsSupport = true;
		try {
			Class.forName("com.sun.awt.AWTUtilities");
		} catch (Exception e) {
			transparentWindowsSupport = false;
		}

		Logger.debug("transparentWindowsSupport={}", transparentWindowsSupport);

		if (transparentWindowsSupport) {
			final Window test = new Window(null, WindowUtils.getAlphaCompatibleGraphicsConfiguration()) {
				private static final long serialVersionUID = 1L;

				public void paint(Graphics g) {
					Graphics2D g2 = (Graphics2D) g;

					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
							RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

					g.setColor(Color.white);
					g.fillRoundRect(100, 150, 100, 100, 32, 32);

					g.setFont(new Font("Sans", Font.BOLD, 32));
					g.drawString("Heavyweight overlay test", 100, 300);
				}
			};

//			AWTUtilities.setWindowOpaque(test, false); // Doesn't work in
														// full-screen exclusive
														// mode, you would have
														// to use 'simulated'
														// full-screen -
														// requires Sun/Oracle
														// JDK
			test.setBackground(new Color(0, 0, 0, 0)); // This is what you do in
														// JDK7
		}
	}

	private JMenuBar buildMenuBar() {
		// Menus are just added as an example of overlapping the video - they
		// are
		// non-functional in this demo player

		JMenuBar menuBar = new JMenuBar();

		JMenu mediaMenu = new JMenu("Media");
		mediaMenu.setMnemonic('m');

		JMenuItem mediaPlayFileMenuItem = new JMenuItem("Play File...");
		mediaPlayFileMenuItem.setMnemonic('f');
		mediaMenu.add(mediaPlayFileMenuItem);

		JMenuItem mediaPlayStreamMenuItem = new JMenuItem("Play Stream...");
		mediaPlayFileMenuItem.setMnemonic('s');
		mediaMenu.add(mediaPlayStreamMenuItem);

		mediaMenu.add(new JSeparator());

		JMenuItem mediaExitMenuItem = new JMenuItem("Exit");
		mediaExitMenuItem.setMnemonic('x');
		mediaMenu.add(mediaExitMenuItem);

		menuBar.add(mediaMenu);

		JMenu playbackMenu = new JMenu("Playback");
		playbackMenu.setMnemonic('p');

		JMenu playbackChapterMenu = new JMenu("Chapter");
		playbackChapterMenu.setMnemonic('c');
		for (int i = 1; i <= 25; i++) {
			JMenuItem chapterMenuItem = new JMenuItem("Chapter " + i);
			playbackChapterMenu.add(chapterMenuItem);
		}
		playbackMenu.add(playbackChapterMenu);

		JMenu subtitlesMenu = new JMenu("Subtitles");
		playbackChapterMenu.setMnemonic('s');
		String[] subs = { "01 English (en)", "02 English Commentary (en)", "03 French (fr)", "04 Spanish (es)",
				"05 German (de)", "06 Italian (it)" };
		for (int i = 0; i < subs.length; i++) {
			JMenuItem subtitlesMenuItem = new JMenuItem(subs[i]);
			subtitlesMenu.add(subtitlesMenuItem);
		}
		playbackMenu.add(subtitlesMenu);

		menuBar.add(playbackMenu);

		JMenu toolsMenu = new JMenu("Tools");
		toolsMenu.setMnemonic('t');

		JMenuItem toolsPreferencesMenuItem = new JMenuItem("Preferences...");
		toolsPreferencesMenuItem.setMnemonic('p');
		toolsMenu.add(toolsPreferencesMenuItem);

		menuBar.add(toolsMenu);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('h');

		JMenuItem helpAboutMenuItem = new JMenuItem("About...");
		helpAboutMenuItem.setMnemonic('a');
		helpMenu.add(helpAboutMenuItem);

		menuBar.add(helpMenu);

		return menuBar;
	}

	private final class TestPlayerMediaPlayerEventListener extends MediaPlayerEventAdapter {
		@Override
		public void mediaChanged(MediaPlayer mediaPlayer) {
			Logger.debug("mediaChanged(mediaPlayer={})", mediaPlayer);
		}

		@Override
		public void finished(MediaPlayer mediaPlayer) {
			Logger.debug("finished(mediaPlayer={})", mediaPlayer);
		}

		@Override
		public void paused(MediaPlayer mediaPlayer) {
			Logger.debug("paused(mediaPlayer={})", mediaPlayer);
		}

		@Override
		public void playing(MediaPlayer mediaPlayer) {
			Logger.debug("playing(mediaPlayer={})", mediaPlayer);
			MediaDetails mediaDetails = mediaPlayer.getMediaDetails();
			Logger.info("mediaDetails={}", mediaDetails);
		}

		@Override
		public void stopped(MediaPlayer mediaPlayer) {
			Logger.debug("stopped(mediaPlayer={})", mediaPlayer);
		}

		@Override
		public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
			Logger.debug("videoOutput(mediaPlayer={},newCount={})", mediaPlayer, newCount);
			if (newCount == 0) {
				return;
			}

			MediaDetails mediaDetails = mediaPlayer.getMediaDetails();
			Logger.info("mediaDetails={}", mediaDetails);

			MediaMeta mediaMeta = mediaPlayer.getMediaMeta();
			Logger.info("mediaMeta={}", mediaMeta);

			final Dimension dimension = mediaPlayer.getVideoDimension();
			Logger.debug("dimension={}", dimension);
			if (dimension != null) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						videoSurface.setSize(dimension);
						mainFrame.pack();
					}
				});
			}

			// You can set a logo like this if you like...
			// File logoFile = new File("./etc/vlcj-logo.png");
			// if(logoFile.exists()) {
			// mediaPlayer.setLogoFile(logoFile.getAbsolutePath());
			// mediaPlayer.setLogoOpacity(0.5f);
			// mediaPlayer.setLogoLocation(10, 10);
			// mediaPlayer.enableLogo(true);
			// }

			// Demo the marquee
			// mediaPlayer.setMarqueeText("vlcj java bindings for vlc");
			// mediaPlayer.setMarqueeSize(40);
			// mediaPlayer.setMarqueeOpacity(95);
			// mediaPlayer.setMarqueeColour(Color.white);
			// mediaPlayer.setMarqueeTimeout(5000);
			// mediaPlayer.setMarqueeLocation(50, 120);
			// mediaPlayer.enableMarquee(true);

			// Not quite sure how crop geometry is supposed to work...
			//
			// Assertions in libvlc code:
			//
			// top + height must be less than visible height
			// left + width must be less than visible width
			//
			// With DVD source material:
			//
			// Reported size is 1024x576 - this is what libvlc reports when you
			// call
			// get video size
			//
			// mpeg size is 720x576 - this is what is reported in the native log
			//
			// The crop geometry relates to the mpeg size, not the size reported
			// through the API
			//
			// For 720x576, attempting to set geometry to anything bigger than
			// 719x575 results in the assertion failures above (seems like it
			// should
			// allow 720x576) to me

			// mediaPlayer.setCropGeometry("4:3");
		}

		@Override
		public void error(MediaPlayer mediaPlayer) {
			Logger.debug("error(mediaPlayer={})", mediaPlayer);
		}

		@Override
		public void mediaSubItemAdded(MediaPlayer mediaPlayer, libvlc_media_t subItem) {
			Logger.debug("mediaSubItemAdded(mediaPlayer={},subItem={})", mediaPlayer, subItem);
		}

		@Override
		public void mediaDurationChanged(MediaPlayer mediaPlayer, long newDuration) {
			Logger.debug("mediaDurationChanged(mediaPlayer={},newDuration={})", mediaPlayer, newDuration);
		}

		@Override
		public void mediaParsedChanged(MediaPlayer mediaPlayer, int newStatus) {
			Logger.debug("mediaParsedChanged(mediaPlayer={},newStatus={})", mediaPlayer, newStatus);
		}

		@Override
		public void mediaFreed(MediaPlayer mediaPlayer) {
			Logger.debug("mediaFreed(mediaPlayer={})", mediaPlayer);
		}

		@Override
		public void mediaStateChanged(MediaPlayer mediaPlayer, int newState) {
			Logger.debug("mediaStateChanged(mediaPlayer={},newState={})", mediaPlayer, newState);
		}

		@Override
		public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
			Logger.debug("mediaMetaChanged(mediaPlayer={},metaType={})", mediaPlayer, metaType);
		}
	}

	/**
	 * 
	 * 
	 * @param enable
	 */
	@SuppressWarnings("unused")
	private void enableMousePointer(boolean enable) {
		Logger.debug("enableMousePointer(enable={})", enable);
		if (enable) {
			videoSurface.setCursor(null);
		} else {
			Image blankImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			videoSurface.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(blankImage, new Point(0, 0), ""));
		}
	}

	// /**
	// *
	// */
	// private final class TestPlayerMouseListener extends MouseAdapter {
	// @Override
	// public void mouseMoved(MouseEvent e) {
	// Logger.trace("mouseMoved(e={})", e);
	// }
	//
	// @Override
	// public void mousePressed(MouseEvent e) {
	// Logger.debug("mousePressed(e={})", e);
	// }
	//
	// @Override
	// public void mouseReleased(MouseEvent e) {
	// Logger.debug("mouseReleased(e={})", e);
	// }
	//
	// @Override
	// public void mouseWheelMoved(MouseWheelEvent e) {
	// Logger.debug("mouseWheelMoved(e={})", e);
	// }
	//
	// @Override
	// public void mouseEntered(MouseEvent e) {
	// Logger.debug("mouseEntered(e={})", e);
	// }
	//
	// @Override
	// public void mouseExited(MouseEvent e) {
	// Logger.debug("mouseExited(e={})", e);
	// }
	// }
	//
	// /**
	// *
	// */
	// private final class TestPlayerKeyListener extends KeyAdapter {
	//
	// @Override
	// public void keyPressed(KeyEvent e) {
	// Logger.debug("keyPressed(e={})", e);
	// }
	//
	// @Override
	// public void keyReleased(KeyEvent e) {
	// Logger.debug("keyReleased(e={})", e);
	// }
	//
	// @Override
	// public void keyTyped(KeyEvent e) {
	// Logger.debug("keyTyped(e={})", e);
	// }
	// }
}
