package com.barbu.paul.gheorghe.radialpong;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = GameView.class.getSimpleName();
	
	protected SurfaceHolder surfaceHolder;
	protected GameThread gameThread;
	
	protected CircleArena arena;
	protected Ball ball;
    private Point displaySize =  new Point();

	public GameView(Context context){
		super(context);		
		this.surfaceHolder = getHolder();
		//TODO: pad no larger than 90 and no smaller than ...
		//TODO: handle activity lifetime, and interruptions like calls
		//TODO: http://stackoverflow.com/questions/1016896/how-to-get-screen-dimensions
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		
		if(display != null){
			display.getSize(displaySize);
		}

        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		
		ball = new Ball.Builder(displaySize)
                .color(0xFF0000FF)
                .speed(64)
                .build();
        arena = new CircleArena.Builder(displaySize)
                .ballRadius(ball.getRadius())
                .color(0xC8000000)
                .bgColorIn(Color.WHITE)
                .bgColorOut(Color.RED)
                .vibrator(v)
                .vibrateDuration(500)
                .build();
		
		surfaceHolder.addCallback(this);
		
		setFocusable(true);
	}

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder){
        this.gameThread = new GameThread(this);
		this.gameThread.setRunning(true);
		this.gameThread.start();
		
		this.render();
		
		Log.d(TAG, "Surface created, thread started");
	}
	
	public void render(){
		Canvas c = this.surfaceHolder.lockCanvas();
		
		if(c == null){
			return;
		}
		
		try{
			arena.draw(c);
			ball.draw(c);
		}
		finally{
			this.surfaceHolder.unlockCanvasAndPost(c);
		}
	}

	public void update(){
        if(!arena.isTouched())
        {
            return;
        }

        PointF p = ball.getPosition();
        float offset = ball.getRadius();

        if(p.x + offset >= displaySize.x || p.x - offset <= 0)
        {
            ball.setVelocityX(-1*ball.getVelocityX());
        }

        if(p.y + offset >= displaySize.y || p.y - offset <= 0)
        {
            ball.setVelocityY(-1*ball.getVelocityY());
        }

        ball.update();
        arena.update(ball);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		boolean retval = this.arena.handleTouchEvent(event);
		
		if(this.gameThread.isPaused() && this.arena.isTouched()){
			this.gameThread.setPaused(false);
		}
		
		return retval;
	}
	 
	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height){	
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder){
		boolean retry = true;
		this.gameThread.setRunning(false);
		
		while(retry){
			try{
				this.gameThread.join();
				retry = false;
			}
			catch(InterruptedException e){
				//try again
			}
		}
		
		Log.d(TAG, "Surface destroyed");
	}	
}