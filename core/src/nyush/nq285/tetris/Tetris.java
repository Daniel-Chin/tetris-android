package nyush.nq285.tetris;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;

import static com.badlogic.gdx.Gdx.audio;
import static java.lang.Thread.sleep;
import java.util.Arrays;
import java.util.Random;

public class Tetris implements ApplicationListener, GestureDetector.GestureListener {
    private float elapsed_time = 0;
    private ShapeRenderer shaper;

    private static final int  width = 10, height = 20;
    private Color[][] grid;
	private Falling falling;
	private float interval;
	private boolean gameover = false;
	private float fatty;
    private int level = 0;
    private float base_interval = .5f;
    private float falling_visual_x, falling_visual_y;
    private float acceleration = 1f;
    private boolean just_rotated = false;
    private boolean solidify_delayed = false;

    private Sound over, score, thump;
    private Music bgm;

	@Override
	public void create () {
        GestureDetector gd = new GestureDetector(this);
        Gdx.input.setInputProcessor(gd);
        shaper = new ShapeRenderer();
        grid = new Color[height][width];
        for (int y = 0; y < height; y++) {
            Arrays.fill(grid[y],Color.BLACK);
        }
        interval = base_interval;
        falling = new Falling();
        fatty = 1;
        over = audio.newSound(Gdx.files.internal("over.mp3"));
        thump = audio.newSound(Gdx.files.internal("thump.mp3"));
        score = audio.newSound(Gdx.files.internal("score.mp3"));
        bgm = audio.newMusic(Gdx.files.internal("BGM.mp3"));
        bgm.setLooping(true);
        bgm.setVolume(.3f);
        bgm.play();
	}

    @Override
    public void resize(int width, int height) {

    }

    @Override
	public void render () {
	    float x_scale = Gdx.graphics.getWidth()/width;
        float y_scale = Gdx.graphics.getHeight()/height;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		shaper.begin(ShapeRenderer.ShapeType.Filled);
		for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                shaper.setColor(grid[y][x]);
                shaper.rect(x*x_scale,y*y_scale,x_scale*fatty,y_scale*fatty);
            }
        }
        shaper.setColor(falling.color);
		if (! gameover) {
            falling_visual_x = (falling_visual_x + falling.x) / 2;
            falling_visual_y = (falling_visual_y + falling.y) / 2;
            for (GridPoint2 member : falling.members) {
                shaper.rect((falling_visual_x + member.x) * x_scale, (falling_visual_y + member.y) * y_scale, x_scale * fatty, y_scale * fatty);
            }
        }
		shaper.end();
		if (! gameover) {
            elapsed_time += Gdx.graphics.getDeltaTime() * acceleration;
        }
        if (elapsed_time >= interval) {
            elapsed_time -= interval;
            falling.y--;
            if (falling.ifCollide()) {
                falling.y++;
                if (! just_rotated) {
                    if (solidify_delayed) {
                        solidify_delayed = false;
                        falling.solidify();
                        clearLineAndAccelerate();
                        thump.play();
                        if (!gameover) {
                            falling = new Falling();
                            acceleration = 1f;
                        }
                    } else {
                        solidify_delayed = true;
                    }
                }
            }
            just_rotated = false;
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
	public void dispose () {
		shaper.dispose();
		bgm.dispose();
		score.dispose();
		thump.dispose();
		over.dispose();
	}

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
	    if (Math.abs(velocityX) > Math.abs(velocityY)) {
            if (velocityX > 0) {
                acceleration = 1f;
                falling.x ++;
                if (falling.ifCollide()) {
                    falling.x --;
                }
            } else {
                acceleration = 1f;
                falling.x --;
                if (falling.ifCollide()) {
                    falling.x ++;
                }
            }
        } else {
            if (velocityY > 0) {
                acceleration = 4f;
            } else {
                acceleration = 1f;
                int original_x = falling.x;
                falling.rotate(true);
                just_rotated = true;
                if (falling.ifCollide()) {
                    if (falling.ifOutWall()) {
                        if (falling.x < width/2) {
                            while (falling.ifOutWall()) {
                                falling.x ++;
                            }
                        } else {
                            while (falling.ifOutWall()) {
                                falling.x --;
                            }
                        }
                    }
                    if (falling.hp > 0) {
                        while (falling.ifCollide()) {
                            falling.y ++;
                            falling.hp --;
                        }
                    } else {
                        falling.rotate(false);
                        falling.x = original_x;
                        just_rotated = false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

    private class Falling {
	    Color color;
	    int x, y;
	    ArrayList<GridPoint2> members = new ArrayList<GridPoint2>();
	    int hp;

        Falling() {
            hp = 3;
	        x = width/2 - 1;
	        y = height + 2;
            falling_visual_x = x;
            falling_visual_y = y;
            add(0,0);
            Random rand = new Random();
            switch (rand.nextInt(7)) {
                case 0: // |
                    color = Color.CYAN;
                    add(0,-1);
                    add(0,1);
                    add(0,2);
                    return;
                case 1: // T
                    color = Color.PURPLE;
                    add(-1,0);
                    add(1,0);
                    add(0,-1);
                    return;
                case 2: // L
                    color = Color.ORANGE;
                    add(1,0);
                    add(0,-1);
                    add(0,-2);
                    return;
                case 3: // L'
                    color = new Color(0,.4f,1,1);
                    add(-1,0);
                    add(0,-1);
                    add(0,-2);
                    return;
                case 4: // O
                    color = Color.YELLOW;
                    add(1,0);
                    add(0,1);
                    add(1,1);
                    return;
                case 5: // 4
                    color = Color.GREEN;
                    add(0,-1);
                    add(1,0);
                    add(1,1);
                    return;
                case 6: // 4'
                    color = Color.RED;
                    add(1,0);
                    add(1,-1);
                    add(0,1);
            }
        }

        private void add(int x, int y) {
            int absolute_x = this.x + x;
            if (0 <= absolute_x && absolute_x < width) {
                members.add(new GridPoint2(x, y));
            } else {
                fatalError("Out of grid boundary");
            }
        }

        boolean ifCollide() {
            for (GridPoint2 member : members) {
                int absolute_x = member.x + x;
                int absolute_y = member.y + y;
                if (absolute_x < 0) {return true;}
                if (absolute_x >= width) {return true;}
                if (absolute_y < 0) {return true;}
                if (absolute_y >= height) {continue;}
                if (grid[y+member.y][x+member.x] != Color.BLACK) {
                    return true;
                }
            }
            return false;
        }

        boolean ifOutWall() {
            for (GridPoint2 member : members) {
                int absolute_x = member.x + x;
                if (absolute_x < 0) {return true;}
                if (absolute_x >= width) {return true;}
            }
            return false;
        }

        void rotate(boolean clockwise) {
            for (GridPoint2 member : members) {
                int temp = member.x;
                member.x = clockwise ? member.y : -member.y;
                member.y = clockwise ? -temp : temp;
            }
        }

        void solidify() {
            for (GridPoint2 member : members) {
                if (y+member.y >= height) {
                    over.play();
                    bgm.stop();
                    gameover = true;
                    return;
                }
                grid[y+member.y][x+member.x] = color;
            }
        }
    }

    private void fatalError(@SuppressWarnings("SameParameterValue") String msg) {
	    System.err.println(msg);
        try {sleep(10000);} catch (InterruptedException e) {e.printStackTrace();}
    }

    private void clearLineAndAccelerate() {
	    boolean yes;
        for (int y = height-1; y >= 0; y--) {
            yes = true;
            for (int x = 0; x < width; x++) {
                if (grid[y][x] == Color.BLACK) {
                    yes = false;
                }
            }
            if (yes) {
                score.play();
                for (int yy = y; yy < height-1; yy++) {
                    System.arraycopy(grid[yy+1],0,grid[yy],0,width);
                }
                Arrays.fill(grid[height-1],Color.BLACK);
                level ++;
                interval = (float) (base_interval / Math.pow(1.0594630943592952645618252949463, level));
            }
        }
    }
}
