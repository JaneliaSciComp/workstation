package org.janelia.it.FlyWorkstation.gui.application.video;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import uk.co.caprica.vlcj.filter.swing.SwingFileFilterFactory;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class PlayerControlsPanel extends JPanel {

	private final EmbeddedMediaPlayer mediaPlayer;

	private JLabel timeLabel;
	private JSlider positionSlider;

	private JButton pauseButton;
	private JButton playButton;
	private JButton ejectButton;
	private JFileChooser fileChooser;

	public PlayerControlsPanel(EmbeddedMediaPlayer mediaPlayer) {
		this.mediaPlayer = mediaPlayer;
		createControls();
		layoutControls();
		registerListeners();
	}

	private void createControls() {
		timeLabel = new JLabel("hh:mm:ss");

		positionSlider = new JSlider();
		positionSlider.setMinimum(0);
		positionSlider.setMaximum(1000);
		positionSlider.setValue(0);
		positionSlider.setToolTipText("Position");

		pauseButton = new JButton();
		pauseButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource("images/control_pause_blue.png")));
		pauseButton.setToolTipText("Play/pause");

		playButton = new JButton();
		playButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource("images/control_play_blue.png")));
		playButton.setToolTipText("Play");

		ejectButton = new JButton();
		ejectButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource("images/control_eject_blue.png")));
		ejectButton.setToolTipText("Load/eject media");

		fileChooser = new JFileChooser();
		fileChooser.setApproveButtonText("Play");
		fileChooser.addChoosableFileFilter(SwingFileFilterFactory.newVideoFileFilter());
		fileChooser.addChoosableFileFilter(SwingFileFilterFactory.newAudioFileFilter());
		fileChooser.addChoosableFileFilter(SwingFileFilterFactory.newPlayListFileFilter());
		FileFilter defaultFilter = SwingFileFilterFactory.newMediaFileFilter();
		fileChooser.addChoosableFileFilter(defaultFilter);
		fileChooser.setFileFilter(defaultFilter);
	}

	private void layoutControls() {
		setBorder(new EmptyBorder(4, 4, 4, 4));
		setLayout(new BorderLayout());

		JPanel positionPanel = new JPanel();
		positionPanel.setLayout(new GridLayout(1, 1));
		positionPanel.add(positionSlider);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout(8, 0));
		topPanel.add(timeLabel, BorderLayout.WEST);
		topPanel.add(positionPanel, BorderLayout.CENTER);
		add(topPanel, BorderLayout.NORTH);

		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout());
		bottomPanel.add(pauseButton);
		bottomPanel.add(playButton);
		bottomPanel.add(ejectButton);
		add(bottomPanel, BorderLayout.SOUTH);
	}

	private void registerListeners() {
		mediaPlayer.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

			@Override
			public void positionChanged(MediaPlayer mp, float newPosition) {
				final int position = (int) (mediaPlayer.getPosition() * 1000.0f);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (mediaPlayer.isPlaying()) {
							positionSlider.setValue(position);
						}
					}
				});
			}
		});

		positionSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!mediaPlayer.isSeekable() || mediaPlayer.isPlaying()) {
					return;
				}
				float positionValue = (float) positionSlider.getValue() / 1000.0f;
				// Avoid end of file freeze-up
				if (positionValue > 0.99f) {
					positionValue = 0.99f;
				}

				mediaPlayer.setPosition(positionValue);
			}
		});

		positionSlider.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
				}
			}

		});

		pauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayer.pause();
			}
		});

		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayer.play();
			}
		});

		ejectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mediaPlayer.enableOverlay(false);
				if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(PlayerControlsPanel.this)) {
					mediaPlayer.setRate(0.5f);
					mediaPlayer.setRepeat(true);
					mediaPlayer.playMedia(fileChooser.getSelectedFile().getAbsolutePath());
				}
				mediaPlayer.enableOverlay(true);
			}
		});
	}
}
