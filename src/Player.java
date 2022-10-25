import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
    private boolean repeat = false;
    private boolean shuffle = false;
    boolean repeat_active = false;
    boolean shuffle_active = false;
    public int shuffleIndex;
    public int[] shuffleQueue;
    private final Lock threadLock = new ReentrantLock();

    private int currentFrame = 0;
    private String currentSongPlayingName;
    private int repeatIndex;

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

    private boolean isPlaying = false;

    public StartSong getCurrentSongPlaying() {
        return currentSongPlaying;
    }

    public void setCurrentSongPlaying(StartSong currentSongPlaying) {
        this.currentSongPlaying = currentSongPlaying;
    }

    private StartSong currentSongPlaying;

    private PlayerWindow window;
    private String playList[][] = new String[0][];
    private int qtde, indexCurrent;
    private String[][] newArray;
    private String[] songInfoDisplay;

    private int songID = 0;
    boolean doublePlay;
    private final String TITULO_DA_JANELA = "Spotify wannabe";
    private final String LISTA_DE_REPRODUÇÃO[][] = new String[0][];

    private final ActionListener buttonListenerPlayNow = e -> beginSong();
    private final ActionListener buttonListenerRemove = e -> removeSong();
    private final ActionListener buttonListenerAddSong = e -> addSong();
    private final ActionListener buttonListenerPlayPause = e -> playPauseSong();
    private final ActionListener buttonListenerStop = e -> stopSong();
    private final ActionListener buttonListenerNext = e -> nextSong();
    private final ActionListener buttonListenerPrevious = e -> previousSong();
    private final ActionListener buttonListenerShuffle = e -> shuffle();
    private final ActionListener buttonListenerLoop = e -> repeatOnOff();

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

    private PlayerWindow playerWindow;

    public Player() {
        String windowTitle = "Spotify wannabe";

        ActionListener playNowListener = event -> beginSong();
        ActionListener removeListener =  event -> removeSong();
        ActionListener addSongListener =  event -> addSong();
        ActionListener playPauseListener =  event -> playPauseSong();
        ActionListener stopListener =  event -> stopSong();
        ActionListener nextListener =  event -> nextSong();
        ActionListener previousListener =  event -> previousSong();
        ActionListener shuffleListener =  event -> shuffle();
        ActionListener repeatListener =  event -> repeatOnOff();

        MouseListener scrubber = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent event) {}

            @Override
            public void mouseReleased(MouseEvent event) {
                mouseRelease();
            }

            @Override
            public void mousePressed(MouseEvent event) {
                mouseClick();
            }

            @Override
            public void mouseEntered(MouseEvent event) {}

            @Override
            public void mouseExited(MouseEvent event) {}
        };

        MouseMotionListener scrubberMotion = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent event) {
                mouseDrag();
            }

            @Override
            public void mouseMoved(MouseEvent event) {}

        };

        this.playerWindow = new PlayerWindow(
                windowTitle,
                this.playList,
                playNowListener,
                removeListener,
                addSongListener,
                shuffleListener,
                previousListener,
                playPauseListener,
                stopListener,
                nextListener,
                repeatListener,
                scrubber,
                scrubberMotion
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

    private void beginSong(){

        String windowSong = playerWindow.getSelectedSong();

        setCurrentSongPlayingName(windowSong);

        int songIndex = findSongByID(windowSong);
        playSong(windowSong, songIndex);

    }

    private void playSong(String selectedSong, int songNumb) {

        if(isPlaying) {
            currentSongPlaying.suspend();
        }


        currentSongPlaying = new StartSong(playerWindow, this, this.playList, selectedSong);
        isPlaying = true;
        currentSongPlaying.start();
        playerWindow.toggleMusicControlButtons(true);
        playerWindow.setPlayingSongInfo(playList[songNumb][0], playList[songNumb][1], playList[songNumb][2]);


    }

    private void removeSong() {
        String songSelected = playerWindow.getSelectedSong();
        removeFromQueue(songSelected);

    }

    public void removeFromQueue(String filePath) {
        if(currentSongPlaying.selectedSong == filePath && isPlaying) {
            currentSongPlaying.suspend();
        }
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
                playerWindow.setQueueList(playList);
                if (this.shuffle_active) {
                    this.createShuffleQueue();
                }

                if(playList.length <= 1){
                    playerWindow.setEnabledShuffleButton(false);
                };

                if(playList.length < 1){
                    playerWindow.setEnabledLoopButton(false);
                };

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

            Song song = playerWindow.openFileChooser(this.songID);
            if (this.shuffle_active){
                this.shuffle();
                this.shuffleIndex = 0;
            }
            addSongToPlaylist(song);


        }catch(Exception e){

            System.out.println(e);
        }
    }

    public void addSongToPlaylist(Song song){
        songInfoDisplay = song.getDisplayInfo();
        new Thread(() -> {
            try {
                threadLock.lock();
                addSongInfoToPlaylist(songInfoDisplay);

                    playerWindow.setQueueList(playList);


                playerWindow.setEnabledScrubber(true);

                playerWindow.setEnabledLoopButton(true);
                if(playList.length > 1){
                    playerWindow.setEnabledShuffleButton(true);
                };



            } catch (Exception e) {

                System.out.println(e);
            } finally {

                threadLock.unlock();
            }
        }).start();
    }


    private void addSongInfoToPlaylist(String[] songInfoDisplay) {

        List<String[]> currentPlaylist = new ArrayList<>(Arrays.asList(playList));

        String[][] updatedPlaylist = {songInfoDisplay};

        currentPlaylist.add(updatedPlaylist[0]);

        playList = currentPlaylist.toArray(updatedPlaylist);
    }

    private void playPauseSong() {

        try{
            if(currentSongPlaying == null || !isPlaying){

                beginSong();

            } else {

                currentSongPlaying.suspend();
                setPlaying(false);
            }

            playerWindow.updatePlayPauseButtonIcon(!isPlaying);

        } catch(Exception e) {

            System.out.println(e);
        }

    }

    private void stopSong() {

        if(isPlaying) {
            currentSongPlaying.suspend();
        }

        playerWindow.resetMiniPlayer();
    }

//    void nextSong() {
//        int songIndex = findSongByID(currentSongPlayingName);
//        if(playList.length == songIndex + 1 && repeat_active){
//            nextPrev(+1);
//        }
//        else if(playList.length != songIndex + 1 ) {
//            nextPrev(+1);
//        }
//    }

    public void nextSong(){
        nextPrev(+1);
    }

//    private void previousSong() {
//        stopSong();
//        int songIndex = findSongByID(currentSongPlayingName);
//        if(songIndex != 0) {
//        nextPrev(-1);
//        }
//    }

    public void previousSong() {
        nextPrev(-1);
    }



    public void nextPrev(int nextPrevIndex) {
        if (this.repeat) {
            repeat();
        } else {
            if (this.shuffle) {
                shuffle();
            }
                int songIndex = findSongByID(currentSongPlayingName);
                int nextMusicIndex = Math.floorMod(songIndex + nextPrevIndex, this.playList.length);
                String selectedSong = this.playList[nextMusicIndex][5];
                currentSongPlayingName = selectedSong;
                playSong(selectedSong, nextMusicIndex);


        };
    }

    public void repeat() {
        int songIndex = findSongByID(currentSongPlayingName);
        playSong(currentSongPlayingName, songIndex);
    }


    public void repeatOnOff() {
        this.repeatIndex = 0;
        if (this.repeat_active){
            this.repeat_active = false;
            System.out.println("shuffle disabled");
        }
        else {
            this.repeat_active = true;
            System.out.println("shuffle enabled");


        }
    }

    private void createShuffleQueue(){

            newArray = new String[playList.length][];
            System.arraycopy(playList, 0, newArray, 0, playList.length);
            String[] first = newArray[0];
            newArray[0] = newArray[indexCurrent];
            newArray[indexCurrent] = first;
            indexCurrent = 0;

            Random rd = new Random();
            for(int i = playList.length -1; i > 0; i--){
                int j = rd.nextInt(0, i+1);

                String[] temp = newArray[i];
                newArray[i] = newArray[j];
                newArray[j] = temp;
            }
            playerWindow.setQueueList(newArray);

    }

    public void shuffle() {
        int songIndex = findSongByID(currentSongPlayingName);
        this.shuffleIndex = 0;
        if (this.shuffle_active){
            this.shuffle_active = false;
            System.out.println("shuffle disabled");
            playerWindow.setQueueList(playList);
        }
        else {
            this.shuffle_active = true;
            System.out.println("shuffle enabled");
            Thread tCreateShuffle = new Thread(() -> { this.createShuffleQueue();});
            tCreateShuffle.start();

        }
    }


    private void loopPlaylist() {
        System.out.println("Teste loopPlaylist");
    }

    public void setCurrentSongPlayingName(String currentSongPlayingName) {
        this.currentSongPlayingName = currentSongPlayingName;
    }

    public String getCurrentSongPlayingName() {
        return currentSongPlayingName;
    }

    private void mouseRelease() { System.out.println("Soltou");}

    private void mouseClick() { System.out.println("Clicou"); }

    private void mouseDrag() { System.out.println("Arrastou"); }


    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
    //</editor-fold>
}
