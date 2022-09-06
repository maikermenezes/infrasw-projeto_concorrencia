import com.mpatric.mp3agic.Mp3File;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class StartSong extends Thread {

    private final PlayerWindow playerWindow;
    private final Player player;
    int currentSeconds;
    int songID;
    String[][] playlist;
    String selectedSong;
    private int maxFrames;
    private Bitstream bitstream;
    private Decoder decoder;
    private AudioDevice device;

    private boolean paused = false;


    public StartSong(
            PlayerWindow playerWindow,
            Player player,
            String[][] songsQueue,
            String selectedSong){

        this.playerWindow = playerWindow;
        this.player = player;
        this.playlist = songsQueue;
        this.selectedSong = selectedSong;
    }

    @Override
    public void run() {
        try {
            playerWindow.updatePlayPauseButtonIcon(false);
            File file = new File(selectedSong);
            Mp3File mp3 = new Mp3File(file);
            maxFrames = mp3.getFrameCount();
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());
            long length = mp3.getLengthInMilliseconds();
            bitstream = new Bitstream(new BufferedInputStream(new FileInputStream(file)));
            start(length);
        } catch (Exception exception) {

            System.out.println(exception);
        }
    }

    public void start(long length) {
        if (device != null) {
            try {
                Header header;
                int currentFrame = 0;
                do {
                    header = bitstream.readFrame();
                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                    device.write(output.getBuffer(), 0, output.getBufferLength());
                    bitstream.closeFrame();
                    playerWindow.setTime(currentFrame * 24, (int) length);
                    currentFrame++;
                } while ( header != null);
            } catch (Exception exception) {

                System.out.println(exception);
            }
        }
    }

    public void pauseResume(){
        paused = !paused;

        synchronized(this){
            this.notify();
        }

    }

    private void allowPause() {
        synchronized(this) {
            while(paused) {
                try {
                    this.wait();
                } catch(InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
    }
}
