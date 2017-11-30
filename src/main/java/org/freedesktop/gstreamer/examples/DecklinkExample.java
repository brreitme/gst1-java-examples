package org.freedesktop.gstreamer.examples;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Clock;
import org.freedesktop.gstreamer.Format;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.SeekFlags;
import org.freedesktop.gstreamer.SeekType;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.PlayBin;

/**
 *
 * An example of playing back a video in an appSink and a decklinkSink
 * 
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DecklinkExample {

	/**
	 * @param args
	 *            the command line arguments
	 */
	private static PlayBin playbin;
	
	private static final String URI = "https://storage.googleapis.com/lao-permanent_content/lipsink/Manifest.mpd";

	public static void main(String[] args) throws URISyntaxException {

		URI uri = new URI(URI);

		Gst.init();
		EventQueue.invokeLater(() -> {
			Bin videoBin = Bin.launch(
					"tee name=t t. ! queue max-size-buffers=400 max-size-bytes=20485760 ! videoscale method=0 ! video/x-raw,width=640,height=480 ! videoconvert ! videorate ! video/x-raw,framerate=30000/1001 ! appsink name=appsink"
							+" t. ! queue max-size-buffers=400 max-size-bytes=20485760 ! videoconvert ! videorate ! video/x-raw,framerate=30000/1001 ! decklinkvideosink device-number=0 mode=9"
							//+ " t. ! queue max-size-buffers=400 max-size-bytes=20485760 ! videoconvert ! videorate ! video/x-raw,framerate=30000/1001 ! decklinkvideosink device-number=1 mode=9"
					,true);

			SimpleVideoComponent vc = new SimpleVideoComponent((AppSink) videoBin.getElementByName("appsink"));

			Bin audioBin = Bin.launch(
					"level interval=100000000 ! tee name=t2"
							+ " t2. ! queue max-size-buffers=400 max-size-bytes=20485760 ! audioconvert ! audioresample ! decklinkaudiosink device-number=0"
							//+ " t2. ! queue max-size-buffers=400 max-size-bytes=20485760 ! audioconvert ! audioresample ! decklinkaudiosink device-number=1"
					,true);

			playbin = new PlayBin("playbin");
			playbin.setVideoSink(videoBin);
			playbin.setAudioSink(audioBin);

			// set PlayBin flags to not use internal video scaling otherwise don't get native resolution output
			// no mapping for GstPlayFlags in bindings yet!
			int flags = (int) playbin.get("flags");
			flags |= (1 << 6);
			playbin.set("flags", flags);

			JFrame window = new JFrame("Decklink Example");
			window.add(vc);
			vc.setPreferredSize(new Dimension(640, 480));
			Box buttons = Box.createHorizontalBox();

			AudioBars audioBars = new AudioBars();
			buttons.add(audioBars);

			JButton seekButton = new JButton("Seek to 1 second");
			JButton playPauseButton = new JButton("Play/Pause");

			buttons.add(seekButton);
			buttons.add(playPauseButton);
			window.add(buttons, BorderLayout.SOUTH);
			window.pack();
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			playbin.setURI(uri);
			// Start outputting
			playbin.play();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
			// Go ahead and pause so it can buffer a little more
			playbin.pause();

			Clock clock = playbin.getClock();
			System.out.println("Using playbin clock " + clock.getName());
			playbin.useClock(clock);

			playPauseButton.addActionListener(e -> {
				if (playbin == null) {
					return;
				}

				if (playbin.isPlaying()) {
					System.out.println("Pausing");
					playbin.pause();
				} else {
					System.out.println("Playing");
					playbin.play();
				}
			});
			
			seekButton.addActionListener(e -> {
				if (playbin == null) {
					return;
				}
				boolean isPlaying = playbin.isPlaying();
				playbin.pause();
				
				playbin.seek(1.0, Format.TIME, SeekFlags.FLUSH, SeekType.SET,
						TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS), SeekType.NONE, -1);

				if (isPlaying) {
					playbin.play();
				}
			});

			window.setVisible(true);
		});
	}

}
