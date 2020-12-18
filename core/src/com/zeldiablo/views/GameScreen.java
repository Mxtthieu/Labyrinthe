package com.zeldiablo.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.zeldiablo.controllers.CollisionListener;
import com.zeldiablo.controllers.KeyboardListener;
import com.zeldiablo.controllers.MouseListener;
import com.zeldiablo.factories.SoundFactory;
import com.zeldiablo.factories.TextureFactory;
import com.zeldiablo.models.GameState;
import com.zeldiablo.models.GameWorld;
import com.zeldiablo.models.Player;
import com.zeldiablo.models.enums.State;
import com.zeldiablo.models.traps.Trap;

public class GameScreen extends ScreenAdapter {

    // Ces deux attributs permettent de déterminer la vitesse du jeu
    static final float STEP_TIME = 1f/45f;
    private float accumulator = 0;
    private float angle;

    private SpriteBatch batch;              // Ensemble de sprites contenu par le jeu
    private SpriteBatch batchTexte;         // SpriteBatch pour les textes
    private Box2DDebugRenderer debug;       // Rendu de debug pour vérifier le placement des corps du jeu

    private GameWorld game;                 // Instance du jeu
    private GameState gameState;            // Instance de l'état du jeu
    private KeyboardListener keyboard;      // Controleur du clavier
    private MouseListener mouse;            // Controleur de la souris
    private CollisionListener collision;    // Controleur de la collision

    private OrthographicCamera camera;      // La caméra du jeu
    private OrthographicCamera cameraText;  // La caméra pour le texte

    private Music music;
    private boolean losePlayed = false;
    private boolean winPlayed = false;

    public GameScreen() {
        this.batch = new SpriteBatch();
        this.batchTexte = new SpriteBatch();
        this.debug = new Box2DDebugRenderer();

        // --- Instanciation des modèles --- //
        this.gameState = new GameState();
        this.game = new GameWorld(this, this.gameState);
        this.keyboard = new KeyboardListener();
        this.mouse = new MouseListener();
        this.collision = new CollisionListener(this.game);

        // --- Ajout des controleurs au jeu --- //
        InputMultiplexer multi = new InputMultiplexer();        // Permet d'ajouter plusieurs écouteurs au jeu
        multi.addProcessor(this.keyboard);
        multi.addProcessor(this.mouse);
        Gdx.input.setInputProcessor(multi);                      // Ajout de la liste d'écouteurs
        this.game.getWorld().setContactListener(this.collision); // Ajout des effects de collision au monde

        this.music = SoundFactory.getInstance().misc_game;
        this.music.play();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (this.keyboard.isReset()) {
            this.gameState.setState(State.RESET);
            this.keyboard.setReset(false);
        }


        if(this.gameState.isReset()){
            this.reset();
            this.gameState.setState(State.IN_PROGRESS);
            this.music.stop();
            this.losePlayed = false;
        }

        if (this.keyboard.isAttack()){
            this.game.atk();
        }

        if (this.keyboard.isDebug()) {
            batch.begin();
            debug.render(this.game.getWorld(), camera.combined);
            batch.end();
        } else {
            if(!this.gameState.isLoading()){
                this.game.draw(this.batch, this.batchTexte);
            }
        }

        // --- Gestion de la pause --- //
        Trap t;
        if(this.keyboard.isPaused()){
            this.gameState.setState(State.PAUSED);
            this.drawPause();
            this.game.stopTimer();
            this.music.pause();
            for(Body b :this.game.getBodies()){
                if(b.getUserData() instanceof Trap){
                    t = (Trap)b.getUserData();
                    t.pause();
                }
            }
        } else if (!this.gameState.isLost() && !this.gameState.isWinned()){
            this.gameState.setState(State.IN_PROGRESS);
            this.game.startTimer();
            for(Body b :this.game.getBodies()){
                if(b.getUserData() instanceof Trap){
                    t = (Trap)b.getUserData();
                    t.play();
                }
            }
        }

        // --- Gesion de la mort --- //
        if(this.gameState.isLost()){
            Gdx.gl.glClearColor(1, 1, 1, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            this.drawGameOver();
            if (!this.losePlayed) {
                SoundFactory.getInstance().lose.play();
                this.losePlayed = true;
            }
        }

        // --- Gestion de la victoire --- //
        if(this.gameState.isWinned()){
            Gdx.gl.glClearColor(1, 1, 1, 0);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            this.music.stop();
            this.drawWin();
            if(!this.winPlayed) {
                //SoundFactory.getInstance().win.play();
                this.winPlayed = true;
            }
        }

        if(!this.gameState.isPaused()){
            this.update();
            if (!this.music.isPlaying()) {
                this.music.play();
            }
        }

    }



    private void reset() {
        this.losePlayed = false;
        this.winPlayed = false;
        this.game.reset();
        this.game.getWorld().setContactListener(this.collision);
    }

    /**
     * Méthode privée. Permet de mettre à jour tous les modèles selon les actions des controleurs.
     */
    private void update() {

        // --- Fin de Gestion --- //
        if(game.getScore() >= 50){
            this.gameState.setState(State.WINNED);
        }

        // --- Gestion du déplacement et de la rotation du personnage dans le jeu --- //
        Vector2 step = keyboard.getStep();
        Player p = this.game.getPlayer();

            // Récupération des coordonées du joueur
        float xP = p.getPosition().x;
        float yP = p.getPosition().y;

            // Récupération des coordonées de la souris
        float xM = GameWorld.WIDTH * this.mouse.getX() / Gdx.graphics.getWidth();
        float yM = GameWorld.HEIGHT * this.mouse.getY() / Gdx.graphics.getHeight();

            // Calcule de la distance entre les deux
        float x = xM - xP;
        float y = yM - yP;

        // Un peu de magie (et de la trigo) et on obtient l'angle
        this.angle = (float) Math.atan2(y, x);
        p.move(step.x, step.y, angle);

        // --- Fin de la gestion --- //

        // --- Gestion de la téléportation ---//
        if(this.collision.isTp()){
            game.teleport(p,this.collision.getPortal());
            this.collision.setTp(false);
        }

        // --- Destruction des bodies --- //
        this.game.deleteBodies();

        // --- Fin de Gestion ---//
        this.stepWorld();

    }

    /**
     * Méthode privée. Permet le calcule et l'application d'un temps d'attente entre deux frames. Cela permet au
     * jeu de garder une certaine fréquence d'image.
     */
    private void stepWorld() {
        float delta = Gdx.graphics.getDeltaTime();
        accumulator += Math.min(delta, 0.25f);

        while (accumulator >= STEP_TIME) {
            accumulator -= STEP_TIME;
            this.game.getWorld().step(STEP_TIME, 6, 2);
        }
    }

    /**
     * Appelé lorsque la fenêtre du jeu est redimensionnée
     * @param width nouvelle largeur
     * @param height nouvelle hauteur
     */
    @Override
    public void resize(int width, int height) {
        // --- Définition de la caméra du jeu --- //
        this.camera = new OrthographicCamera((float)GameWorld.WIDTH,GameWorld.HEIGHT);
        this.camera.position.set((float)GameWorld.WIDTH / 2, (float) GameWorld.HEIGHT / 2, 0);
        this.camera.update();
        this.batch.setProjectionMatrix(camera.combined);


        // --- Définition de la caméra pour les textes --- //
        this.cameraText = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cameraText.position.set((float)Gdx.graphics.getWidth(),(float)Gdx.graphics.getHeight(), 0);
        cameraText.update();

    }

    @Override
    public void dispose() {
        this.batch.dispose();
    }


    private void drawPause() {
            batch.begin();
            batch.draw(TextureFactory.INSTANCE.getPause(), 0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
            batch.end();
    }

    private void drawGameOver() {
        batch.begin();
        batch.draw(TextureFactory.INSTANCE.getGameover(), 0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
        batch.end();
    }

    private void drawWin() {
        batch.begin();
        batch.draw(TextureFactory.INSTANCE.getWin(), 0, 0, GameWorld.WIDTH, GameWorld.HEIGHT);
        batch.end();
    }


    public float getAngle() {
        return angle;
    }

    public MouseListener getMouse() {
        return mouse;
    }
}
