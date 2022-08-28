import java.io.*;
import java.lang.*;
import java.lang.reflect.Array;
import java.util.*;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import support.PlayerWindow;
import support.Song;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Player {

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
    private String playList[][] = new String[0][];
    private int currentFrame = 0;
    private int songID = 0;
    private int songOrder = 0;
    private boolean isPlaying = false;

    private final Lock threadLock = new ReentrantLock();
    private final String TITULO_DA_JANELA = "Spotify wannabe";
    private final String LISTA_DE_REPRODUÇÃO[][] = new String[0][];

    private final ActionListener buttonListenerPlayNow = e -> playSong(toString());
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

    public Player( ) {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                TITULO_DA_JANELA,
                this.playList,
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

    private void playSong(String selectedSong) {

    }

    private void removeSong() {
        String songSelected = window.getSelectedSong();
        removeFromQueue(songSelected);

    }

    public void removeFromQueue(String filePath) {
        new Thread(() -> {
            try {
                threadLock.lock();
                int songId = findSongByID(filePath);
                System.out.println(songId);
                String[][] copyArray = new String[playList.length - 1][];
                for (int i = 0, j = 0; i < playList.length; i++) {
                    if (i != songId) {
                        copyArray[j++] = playList[i];
                    }
                }
                System.out.println(playList.length);
                System.out.println(copyArray.length);
                playList = copyArray;
                window.setQueueList(playList);

            } finally {
                threadLock.unlock();
            }
        }).start();
    }

    public int findSongByID(String id){
        System.out.println(playList[0][5]);
        for(int i = 0; i< playList.length;i++)
        {
            if (playList[i][5].equals(id)) {
                System.out.println("Found the profile containing information for " + id);
//                System.out.println(id.getFirstName()+id.getFirstName()+id.getLastName()+id.getDob());
                return i;
            } else
                System.out.println("Could not find a profile based on the ID you provided");
        }
        return -1;
    }

    private void addSong() {

        try{

            Song song = this.window.openFileChooser(this.songID);
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
