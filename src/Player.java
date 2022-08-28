import javazoom.jl.decoder.*;
import javazoom.jl.player.*;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player extends Component {

    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice where audio samples are written to.
     */
    private AudioDevice device;

    private PlayerWindow window;

    private final Lock threadLock = new ReentrantLock();

    private int currentFrame = 0;

    public List<Song> getSongs() {
        return this.songs;
    }

    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }

    private List<Song> songs = new ArrayList();

    public int getSongOrder() {
        return songOrder;
    }

    public void setSongOrder(int songOrder) {
        this.songOrder = songOrder;
    }

    public void increaseSongOrder(){
        this.songOrder++;
    }

    private int songOrder = 1;

    private final String TITULO_DA_JANELA = "Spotify wannabe";
    private String playList[][] = new String[0][];

    private final ActionListener buttonListenerPlayNow = e -> playSong();
    private final ActionListener buttonListenerRemove = e -> removeSong();
    private final ActionListener buttonListenerAddSong = e -> addSong();
    private final ActionListener buttonListenerPlayPause = e -> playPauseSong();
    private final ActionListener buttonListenerStop = e -> stopSong();
    private final ActionListener buttonListenerNext = e -> nextSong();
    private final ActionListener buttonListenerPrevious = e -> previousSong();
    private final ActionListener buttonListenerShuffle = e -> shufflePlaylist();
    private final ActionListener buttonListenerLoop = e -> loopPlaylist();
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            System.out.println("Teste");
        }

        @Override
        public void mousePressed(MouseEvent e) {
            System.out.println("Teste");
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            System.out.println("Teste");
        }
    };

    public Player() {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                TITULO_DA_JANELA,
                playList,
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter)
        );
    }

    //<editor-fold desc="Essential">

    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        // TODO Is this thread safe?
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException Generic Bitstream exception.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > currentFrame) {
            int framesToSkip = newFrame - currentFrame;
            boolean condition = true;
            while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
        }
    }

    private void playSong() {

        Player mp3 = new Player();
        System.out.println("Teste playsong");
    }

    private void removeSong() {
        System.out.println("Teste removeSong");

    }

    private void addSong() {

        try{

            Song song = this.window.openFileChooser();
            addSongToPlaylist(song);
        }catch(Exception e){

            System.out.println(e);
        }
    }

    public void addSongToPlaylist(Song song){
        String[] songInfoDisplay = song.getDisplayInfo();
        new Thread(() -> {
            try {
                threadLock.lock();

                List<String[]> currentPlaylist = new ArrayList<>(Arrays.asList(playList));

                String[][] updatedPlaylist = {songInfoDisplay};

                currentPlaylist.add(updatedPlaylist[0]);

                playList = currentPlaylist.toArray(updatedPlaylist);

                this.window.setQueueList(playList);

            } catch (Exception e) {

                System.out.println(e);
            } finally {

                threadLock.unlock();
            }
        }).start();
    }

    private void playPauseSong() {
        System.out.println("Teste playPauseSong");
    }

    private void stopSong() {
        System.out.println("Teste stopSong");
    }

    private void nextSong() {
        System.out.println("Teste nextSong");
    }

    private void previousSong() {
        System.out.println("Teste previousSong");
    }

    private void shufflePlaylist() {
        System.out.println("Teste shufflePlaylist");
    }

    private void loopPlaylist() {
        System.out.println("Teste loopPlaylist");
    }



    //</editor-fold>
}
