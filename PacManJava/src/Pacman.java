import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

public class Pacman extends JPanel implements ActionListener, KeyListener {

    private static final int HUD_HEIGHT = 120;

    class Block{
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; //U D L R
        int velocityX = 0;
        int velocityY = 0;  

        Block(Image image, int x, int y , int width, int height){
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char newDir){
            char prevDirection = this.direction; 
            this.direction = newDir;
            updateVelocity();
            int testX = x + velocityX;
            int testY = y + velocityY;
            for (Block wall : walls){
                if (testX < wall.x + wall.width && testX + width > wall.x && testY < wall.y + wall.height && testY + height > wall.y){
                    this.direction = prevDirection;
                    updateVelocity();
                    return;
                }
            }
        }

        void updateVelocity(){
            if (this.direction == 'U'){
                this.velocityX = 0;
                this.velocityY = -tileSize/4;
            }
            else if(this.direction == 'D'){
                this.velocityX = 0;
                this.velocityY = tileSize/4;
            }
            else if(this.direction == 'L'){
                this.velocityX = -tileSize/4;
                this.velocityY = 0; 
            }
            else if(this.direction == 'R'){
                this.velocityX = tileSize/4;
                this.velocityY = 0; 
            }
        }

        void reset(){
            this.x = this.startX;
            this.y = this.startY;
        }
    }
    
    private final int rowCount = 21;
    private final int columnCount = 19;
    private final int tileSize = 32;
    private final int boardWidth = columnCount*tileSize;
    private final int boardHeight = rowCount*tileSize + HUD_HEIGHT; // extra space for score display

    private Font titleFont; // Title font
    private Font hudFont; // HUD font
    private Font smallFont; // Small font

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;
    
    private Image cherryImage;
    private Image heartImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    private Image muteImage;
    private Image unmuteImage;

    private Clip backgroundClip;
    private float volume = 0.6f;

    private char nextDirection;

    private boolean isAlignedToGrid(Block b) {
        return b.x % tileSize == 0 && b.y % tileSize == 0;
    }

    private boolean muted = false;
    private boolean cherrySpawned = false;

    private enum GameState {
        PLAYING,
        GAME_OVER,
        LEVEL_TRANSITION,
        LIVES_UPDATE,
        WIN_STATUS,
        PAUSED
    }

    private GameState gameState = GameState.PLAYING;

    private int currentLevel = 0;
    Levels levels = new Levels();

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    Block cherry;
    Block pacman;

    Timer gameLoop;
    char[] directions = {'U','D','L','R'}; //up,down,left,right
    Random random = new Random();
    int totalScore = 0; //initial score 0 
    // NOTE: only when player completes the level, totalScore updates. If user dies before completing 
    // totalScore is 0 and does not get updated.

    int levelScore = 0;
    int lives = 3; //default 3 lives

    Pacman(){
        setPreferredSize(new Dimension(boardWidth,boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        //load images
        wallImage = new ImageIcon(getClass().getResource("./Images/wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./Images/blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./Images/orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./Images/pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./Images/redGhost.png")).getImage(); 
        pacmanUpImage = new ImageIcon(getClass().getResource("./Images/pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./Images/pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./Images/pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./Images/pacmanRight.png")).getImage();
        cherryImage = new ImageIcon(getClass().getResource("./Images/cherry.png")).getImage();
        muteImage = new ImageIcon(getClass().getResource("./Images/mute.png")).getImage();
        unmuteImage = new ImageIcon(getClass().getResource("./Images/unmute.png")).getImage();
        heartImage = new ImageIcon(getClass().getResource("./Images/heart.png")).getImage();

        titleFont = loadFont("/Fonts/PressStart2P-Regular.ttf", 28f);
        hudFont = loadFont("/Fonts/PressStart2P-Regular.ttf", 20f);
        smallFont = loadFont("/Fonts/PressStart2P-Regular.ttf", 14f);

        loadMap(currentLevel);

        for(Block ghost : ghosts){
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        //how long it takes to start timer, ms gone between frames
        gameLoop = new Timer(50, this); //20fps(1000/50)
        gameLoop.start();
    }

    private Font loadFont(String path, float size) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream(path));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(font);
            return font.deriveFont(size);
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return new Font("Arial", Font.PLAIN, 12);
        }
    }

    public void loadMap(int level){
        if (backgroundClip != null && backgroundClip.isRunning()){
            backgroundClip.stop();
            backgroundClip.close();
        }
        walls = new HashSet<Block >();
        foods = new HashSet<Block >();
        ghosts = new HashSet<Block >();

        String[] map = levels.tileMap[level];

        cherrySpawned = false;

        for (int r = 0; r < rowCount;r++){
            for (int c = 0; c < columnCount; c++){
                String row = map[r];
                char tileMapChar = row.charAt(c);

                int x = c * tileSize;
                int y = r *tileSize;

                if (tileMapChar == 'X'){ //block wall
                    Block wall = new Block(wallImage,x,y,tileSize,tileSize);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b'){ //blueghost
                    Block ghost = new Block (blueGhostImage,x,y,tileSize,tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o'){ //orange ghost
                    Block ghost = new Block (orangeGhostImage,x,y,tileSize,tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p'){ //pink ghost
                    Block ghost = new Block (pinkGhostImage,x,y,tileSize,tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r'){ //red ghost
                    Block ghost = new Block (redGhostImage,x,y,tileSize,tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'P'){ //pacman
                    pacman = new Block (pacmanRightImage,x,y,tileSize,tileSize);
                }
                else if (tileMapChar == ' '){
                    Block food = new Block (null,x+14,y+14,4,4);
                    foods.add(food);
                }
                else if (tileMapChar == 'O'){
                    continue;
                }
            }
        }
        playMusic();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(0, HUD_HEIGHT);
        draw(g2);
        g2.dispose();
    }

    private void drawDimBackground (Graphics g, float alpha) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, boardWidth, boardHeight);
        g2d.dispose();
    }

    public void draw(Graphics g) {
        //draw pacman
        g.drawImage(pacman.image,pacman.x, pacman.y, pacman.width, pacman.height,null); 

        //draw cherry
        if (cherrySpawned && cherry != null) {
            g.drawImage(cherryImage,cherry.x,cherry.y,cherry.width,cherry.height,null);
        }

        //draw ghosts
        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x,ghost.y,ghost.width,ghost.height,null);
        }

        //draw walls
        for (Block wall : walls) {
            g.drawImage(wall.image,wall.x,wall.y,wall.width,wall.height,null);
        }

        // draw food
        g.setColor(Color.WHITE);
        for(Block food : foods) {
            g.fillRect(food.x,food.y,food.width,food.height);
        }

        // draw title
        g.setFont(titleFont);
        g.setColor(Color.YELLOW);
        g.drawString("PAC-MAN", boardWidth/2 - 98, -75);  

        //score, lives, level
        g.setFont(smallFont);
        g.drawString("Score:" + levelScore, 20, -10);
        g.drawString("Lives:", 228, -10);
        int offset = 0;
        for (int i = lives; i > 0; i--) {
            g.drawImage(heartImage, 305 + offset, -38, 40, 40, null);
            offset += 30;
        }
        g.drawString("Level:" + (currentLevel + 1), 485, -10);

        // draw mute status
        if (muted) {
            g.drawImage(muteImage, 555, -80, 20, 20, null);
        } else {
            g.drawImage(unmuteImage, 555, -80, 20, 20, null);
        }

        // draw game state messages
        switch (gameState) {
            case GAME_OVER:
                drawDimBackground(g, 0.7f);
                drawCenteredText(g, "GAME OVER!", boardHeight/2 - 135, titleFont.getFontName());
                drawCenteredText(g, "Final Score: " + (totalScore + levelScore), boardHeight/2 - 95, hudFont.getFontName());
                drawCenteredText(g, "Press ENTER to Restart", boardHeight/2 - 55, smallFont.getFontName());
                return;
            case LIVES_UPDATE:
                drawDimBackground(g, 0.7f);
                drawCenteredText(g, "Lives Remaining: " + lives, boardHeight/2 - 125, hudFont.getFontName());
                drawCenteredText(g, "Press 'ENTER' to Continue", boardHeight/2 - 85, smallFont.getFontName());
                return;
            case LEVEL_TRANSITION:
                drawDimBackground(g, 0.7f);
                drawCenteredText(g, "LEVEL " + (currentLevel + 1), boardHeight/2 - 125, hudFont.getFontName());
                drawCenteredText(g, "Get Ready!", boardHeight/2 - 85, smallFont.getFontName());
                drawCenteredText(g, "Press 'ENTER' to Start", boardHeight/2 - 45, smallFont.getFontName());
                return;
            case WIN_STATUS:
                drawDimBackground(g, 0.7f);
                drawCenteredText(g, "CONGRATULATIONS! YOU WIN!", boardHeight/2 - 125, titleFont.getFontName());
                drawCenteredText(g, "Press ENTER to Restart", boardHeight/2 - 85, hudFont.getFontName());
                return;
            case PAUSED:   
                drawDimBackground(g, 0.7f);
                drawCenteredText(g, "GAME PAUSED", boardHeight/2 - 125, hudFont.getFontName());
                drawCenteredText(g, "Press 'P' to Resume", boardHeight/2 - 95, smallFont.getFontName());
                return;
            default:
                break;
        }
    }

    private void drawCenteredText(Graphics g, String text, int centerY, String fontName) {
        Font selectedFont = titleFont;
        if (fontName.equals(hudFont.getFontName())) {
            selectedFont = hudFont;
        } else if (fontName.equals(smallFont.getFontName())) {
            selectedFont = smallFont;
        }
        g.setFont(selectedFont);
        g.setColor(Color.YELLOW);
        FontMetrics metrics = g.getFontMetrics(selectedFont);
        int textWidth = metrics.stringWidth(text);
        int ascent = metrics.getAscent();
        int x = (boardWidth - textWidth) / 2;
        int y = centerY + (ascent / 2);
        g.drawString(text, x, y);
    }

    public void move(){
        if (nextDirection != '\0' && isAlignedToGrid(pacman)) {
            char before = pacman.direction;
            pacman.updateDirection(nextDirection);

            if (pacman.direction != before) {
                nextDirection = '\0'; // consume input
            }
        }
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        switch(pacman.direction){
            case 'U': pacman.image = pacmanUpImage; break;
            case 'D': pacman.image = pacmanDownImage; break;
            case 'L': pacman.image = pacmanLeftImage; break;
            case 'R': pacman.image = pacmanRightImage; break;
        }

        //check for wall
        for(Block wall : walls){
            if (collision(pacman, wall)){
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        //check ghost collisions
        for (Block ghost : ghosts){
            if(collision(ghost, pacman)){
                lives--;
                pacman.velocityX = 0;
                pacman.velocityY = 0;
                nextDirection = '\0';
                if (backgroundClip != null) {
                    backgroundClip.stop();
                }
                playSoundEffect("/Music/Death.wav", lives <= 0 ? 1.0f : 0.6f);
                gameState = (lives <= 0) ? GameState.GAME_OVER : GameState.LIVES_UPDATE;
                return;
            }
            if (ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D'){
                ghost.updateDirection('U');
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;

            for (Block wall : walls){
                if(collision(ghost,wall) || ghost.x <0 || ghost.x + ghost.width >= boardWidth){
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }
        }

        //check food collision
        Block foodEaten = null;
        for (Block food : foods){
            if(collision(pacman, food)){
                foodEaten = food;
                levelScore += 10;
                playSoundEffect("/Music/Eating.wav", 0.6f);
            }
        }
        foods.remove(foodEaten);

        // spawn cherry ONCE
        if (!cherrySpawned && levelScore >= 100) {
            if (!foods.isEmpty()) {
                //int index = random.nextInt(foods.size());
                Block spawnLocation = foods.stream()
                           .skip(random.nextInt(foods.size()))
                           .findFirst()
                           .orElse(null);
                cherry = new Block(
                    cherryImage,
                    spawnLocation.x - 14,
                    spawnLocation.y - 14,
                    tileSize,
                    tileSize
                );
                cherrySpawned = true;
            }
        }

        //check cherry collision
        if (cherry != null && collision(pacman, cherry)) {
            cherry = null;
            levelScore += 100;
            playSoundEffect("/Music/Cherry.wav", 1.0f);
        }

        if (foods.isEmpty()){
            gameState = GameState.LEVEL_TRANSITION;
            advanceLevel();
        }
    }

    private void advanceLevel() {
        currentLevel += 1;
        totalScore += levelScore;
        levelScore = 0;
        if (currentLevel >= levels.tileMap.length){
            gameState = GameState.WIN_STATUS;
            return;
        }
        Timer t = new Timer(1200, e -> {
            loadMap(currentLevel);
            resetPositions();
            playMusic();
            gameState = GameState.PLAYING;
        });
        t.setRepeats(false);
        t.start();
    }

    public boolean collision(Block a, Block b){
        return a.x < b.x + b.width &&
               a.x + a.width > b.x &&
               a.y < b.y +b.height &&
               a.y + a.height >b.y; 
    }

    public void resetPositions(){
        if (backgroundClip!= null && backgroundClip.isRunning()){
            backgroundClip.stop();
        }
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        nextDirection = '\0';

        for(Block ghost : ghosts){
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection) ;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING)
            move();
        repaint();
}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            switch (gameState) {
                case LIVES_UPDATE:
                    resetPositions();
                    playMusic();
                    gameState = GameState.PLAYING;
                    return;
                case GAME_OVER:
                    currentLevel = 0;
                    lives = 3;
                    totalScore = 0;
                    levelScore = 0;
                    loadMap(currentLevel);
                    resetPositions();
                    playMusic();
                    gameState = GameState.PLAYING;
                    return;
                case LEVEL_TRANSITION:
                    levelScore = 0;
                    return;
                default:
                    break;
            }
        }
        // movement keys
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP)
            nextDirection = 'U';
        else if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN)
            nextDirection = 'D';
        else if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT)
            nextDirection = 'L';
        else if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT)
            nextDirection = 'R';
        else if (e.getKeyCode() == KeyEvent.VK_P) {
            if (gameState == GameState.PAUSED)
                gameState = GameState.PLAYING;
            else
                gameState = GameState.PAUSED;
        }
        else if (e.getKeyCode() == KeyEvent.VK_M) {
            muted = !muted;
            if (muted) {
                if (backgroundClip != null && backgroundClip.isRunning()) {
                    backgroundClip.stop();
                }
            } else {
                playMusic();
            }
        }
    }

    public void playMusic() {
        if (muted) return;
        if (backgroundClip != null && backgroundClip.isRunning()) return;
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource("/Music/PacmanMusic.wav"));
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioInputStream);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            setVolume(0.3f);
            backgroundClip.start();
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        if (backgroundClip == null) return;

        FloatControl gain =
            (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);

        float dB = (float) (Math.log10(volume == 0 ? 0.0001 : volume) * 20);
        gain.setValue(dB);
    }

    public void playSoundEffect(String soundFilePath, float soundVolume) {
        if (muted) return;
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(getClass().getResource(soundFilePath));
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
             FloatControl gain =
            (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            soundVolume = Math.max(0f, Math.min(1f, soundVolume));
            float dB = (float) (Math.log10(soundVolume == 0 ? 0.0001 : soundVolume) * 20);
            gain.setValue(dB);
            clip.start();
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace(); 
        }
    }
}