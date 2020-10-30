package com.zeldiablo.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.zeldiablo.controllers.KeyboardListener;
import com.zeldiablo.controllers.MouseListener;
import com.zeldiablo.models.GameWorld;
import com.zeldiablo.models.Player;

public class GameScreen extends ScreenAdapter {

    // Ces deux attributs permettent de déterminer la vitesse du jeu
    static final float STEP_TIME = 1f/45f;
    private float accumulator = 0;

    private SpriteBatch batch;              // Ensemble de sprites contenu par le jeu
    private Box2DDebugRenderer debug;       // Rendu de debug pour vérifier le placement des corps du jeu

    private GameWorld game;                 // Instance du jeu
    private KeyboardListener keyboard;      // Controleur du clavier
    private MouseListener mouse;            // Controleur de la souris

    private OrthographicCamera camera;      // La caméra du jeu
    private Viewport view;                  // Adapte le jeu a tous type d'écran

    public GameScreen() {
        this.batch = new SpriteBatch();
        this.debug = new Box2DDebugRenderer();

        // --- Instanciation des modèles --- //
        this.game = new GameWorld(this);
        this.keyboard = new KeyboardListener();
        this.mouse = new MouseListener();

        // --- Définition de la caméra du jeu --- //
        this.camera = new OrthographicCamera();
        this.view = new FitViewport(GameWorld.WIDTH, GameWorld.HEIGHT, this.camera);
        this.view.apply();
        this.camera.position.set(camera.viewportWidth/2, camera.viewportHeight/2, 0);   // Placement de la camera au centre sur un plan 2D
        this.camera.update();
        this.batch.setProjectionMatrix(camera.combined);

        // --- Ajout des controleurs au jeu --- //
        InputMultiplexer multi = new InputMultiplexer();    // Permet d'ajouter plusieurs écouteurs au jeu
        multi.addProcessor(this.keyboard);
        multi.addProcessor(this.mouse);
        Gdx.input.setInputProcessor(multi);                 // Ajout de la liste d'écouteurs
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        this.update();

        if (this.keyboard.isDebug()) {
            batch.begin();
            debug.render(this.game.getWorld(), camera.combined);
            batch.end();
        }
    }

    /**
     * Méthode privée. Permet de mettre à jour tous les modèles selon les actions des controleurs.
     */
    private void update() {

        // --- Gestion de la pause --- //
        if(this.kl.isPaused()){
            this.gameState.setState(State.PAUSED);
        } else {
            this.gameState.setState(State.IN_PROGRESS);
        }

        // --- Gestion du déplacement et de la rotation du personnage dans le jeu --- //
        Vector2 step = keyboard.getStep();
        Player p = this.game.getPlayer();

            // Récupération des coordonées du joueur
        float xP = p.getPosition().x;
        float yP = p.getPosition().y;

            // Récupération des coordonées de la souris
        float xM = this.mouse.getX();
        float yM = this.mouse.getY();

            // Calcule de la distance entre les deux
        float x = xM - xP;
        float y = yM - yP;

            // Un peu de magie (et de la trigo) et on obtient l'angle
        float angle = (float) Math.atan(y/x);
        if (x < 0)
            angle += Math.PI;

        p.move(step.x, step.y, angle);
        // --- Fin de la gestion --- //

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
        this.view.update(width, height);
        this.camera.update();
    }

    @Override
    public void dispose() {
        this.batch.dispose();
    }
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if(gameState.isPaused()){
            this.drawPause();
        }

        this.update();
    }


    private void update() {
        if(this.kl.isPaused()){
            this.gameState.setState(State.PAUSED);
        } else {
            this.gameState.setState(State.IN_PROGRESS);
        }
    }

    private void drawPause() {
        gameBatch.begin();
        gameBatch.draw(TextureFactory.INSTANCE.getPause(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        gameBatch.end();
    }


}
