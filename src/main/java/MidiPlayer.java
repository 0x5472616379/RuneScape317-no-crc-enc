import javax.sound.midi.*;
import java.io.File;
import java.io.FileInputStream;

/**
 * MIDI player that runs in a background thread and monitors Signlink.midi
 * for playback commands.
 */
public class MidiPlayer implements Runnable {

    private static Sequencer sequencer;
    private static Synthesizer synthesizer;
    private static Thread playerThread;
    private static boolean running = false;
    private static String lastMidi = null;
    private static int lastVolume = 0;

    /**
     * Starts the MIDI player thread.
     */
    public static void start() {
        if (running) {
            return;
        }

        try {
            // Get MIDI devices
            sequencer = MidiSystem.getSequencer(false);
            synthesizer = MidiSystem.getSynthesizer();

            sequencer.open();
            synthesizer.open();

            // Connect sequencer to synthesizer
            Transmitter transmitter = sequencer.getTransmitter();
            Receiver receiver = synthesizer.getReceiver();
            transmitter.setReceiver(receiver);

            System.out.println("MIDI player initialized");
            System.out.println("Sequencer: " + sequencer.getDeviceInfo().getName());
            System.out.println("Synthesizer: " + synthesizer.getDeviceInfo().getName());

            running = true;
            playerThread = new Thread(new MidiPlayer());
            playerThread.setDaemon(true);
            playerThread.setPriority(Thread.MIN_PRIORITY);
            playerThread.start();

        } catch (MidiUnavailableException e) {
            System.err.println("MIDI unavailable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops the MIDI player and releases resources.
     */
    public static void stop() {
        running = false;

        if (sequencer != null) {
            if (sequencer.isRunning()) {
                sequencer.stop();
            }
            sequencer.close();
        }

        if (synthesizer != null) {
            synthesizer.close();
        }

        System.out.println("MIDI player stopped");
    }

    /**
     * Sets the master volume for MIDI playback.
     * @param volume Volume in decibels (0 = normal, negative = quieter)
     */
    private static void setVolume(int volume) {
        if (synthesizer == null || !synthesizer.isOpen()) {
            return;
        }

        try {
            // Convert decibels to gain (0.0 to 1.0)
            // volume is in range like -1200 to 0
            float gain = (float) Math.pow(10.0, volume / 2000.0);
            gain = Math.max(0.0f, Math.min(1.0f, gain));

            // Apply to all channels
            MidiChannel[] channels = synthesizer.getChannels();
            for (MidiChannel channel : channels) {
                if (channel != null) {
                    channel.controlChange(7, (int) (gain * 127)); // Volume control
                }
            }

            lastVolume = volume;
        } catch (Exception e) {
            System.err.println("Error setting MIDI volume: " + e.getMessage());
        }
    }

    /**
     * Plays a MIDI file.
     * @param filepath Path to the MIDI file
     * @param loop Whether to loop continuously
     */
    private static void playMidi(String filepath, boolean loop) {
        try {
            // Stop current playback
            if (sequencer.isRunning()) {
                sequencer.stop();
            }

            // Load the MIDI file
            File midiFile = new File(filepath);
            if (!midiFile.exists()) {
                System.err.println("MIDI file not found: " + filepath);
                return;
            }

            FileInputStream fis = new FileInputStream(midiFile);
            Sequence sequence = MidiSystem.getSequence(fis);
            fis.close();

            // Set the sequence
            sequencer.setSequence(sequence);

            // Set looping
            if (loop) {
                sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            } else {
                sequencer.setLoopCount(0);
            }

            // Apply volume
            setVolume(lastVolume);

            // Start playback
            sequencer.start();

            System.out.println("Playing MIDI: " + filepath + (loop ? " (looping)" : ""));

        } catch (Exception e) {
            System.err.println("Error playing MIDI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stops MIDI playback.
     */
    private static void stopMidi() {
        if (sequencer != null && sequencer.isRunning()) {
            sequencer.stop();
            System.out.println("MIDI playback stopped");
        }
    }

    @Override
    public void run() {
        System.out.println("MIDI player thread started");

        while (running) {
            try {
                // Check if there's a new MIDI command
                String currentMidi = Signlink.midi;

                if (currentMidi != null) {
                    if (currentMidi.equals("voladjust")) {
                        // Volume adjustment - always process, even if same as last
                        setVolume(Signlink.midivol);
                        System.out.println("MIDI volume adjusted to: " + Signlink.midivol);
                        Signlink.midi = null;
                    } else if (!currentMidi.equals(lastMidi)) {
                        // Process other commands only if different from last
                        lastMidi = currentMidi;

                        if (currentMidi.equals("stop")) {
                            // Stop playback
                            stopMidi();
                        } else {
                            // Play new MIDI file
                            playMidi(currentMidi, true); // Loop background music
                            lastVolume = Signlink.midivol;
                        }

                        // Clear the command
                        Signlink.midi = null;
                    }
                }

                // Sleep to avoid busy waiting
                Thread.sleep(100);

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("MIDI player error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("MIDI player thread stopped");
    }
}