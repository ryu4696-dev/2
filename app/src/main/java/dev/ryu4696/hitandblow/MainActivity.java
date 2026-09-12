package dev.ryu4696.hitandblow;

import android.app.Activity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.*;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(new GameView());
    }

    private class GameView extends View {
        final int[] COLORS = {0xffef476f,0xffffb703,0xff39d98a,0xff27a9e1,0xff835af1,0xffff7a36};
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final ArrayList<int[]> guesses = new ArrayList<>();
        final ArrayList<int[]> scores = new ArrayList<>();
        final ArrayList<RectF> colorButtons = new ArrayList<>();
        final int[] pick = {-1,-1,-1,-1};
        int[] answer = new int[4];
        RectF undo = new RectF(), submit = new RectF(), restart = new RectF(), duplicate = new RectF();
        boolean allowDuplicate = false, won = false, over = false;
        String message = "4つの色と順番を当てよう";
        float den;

        GameView() { super(MainActivity.this); den=getResources().getDisplayMetrics().density; setLayerType(View.LAYER_TYPE_SOFTWARE,null); newGame(); }
        void newGame() {
            guesses.clear(); scores.clear(); Arrays.fill(pick,-1); won=false; over=false;
            ArrayList<Integer> bag=new ArrayList<>(); for(int i=0;i<6;i++) bag.add(i);
            Collections.shuffle(bag);
            Random r=new Random();
            for(int i=0;i<4;i++) answer[i]=allowDuplicate?r.nextInt(6):bag.get(i);
            message="4つの色と順番を当てよう"; invalidate();
        }
        void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align) {
            p.setStyle(Paint.Style.FILL); p.setTypeface(Typeface.create("sans",Typeface.BOLD)); p.setTextAlign(align); p.setTextSize(size); p.setColor(color); c.drawText(s,x,y,p);
        }
        void round(Canvas c,RectF r,float radius,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(r,radius,radius,p);}
        void ball(Canvas c,float x,float y,float rad,int color){
            p.setShadowLayer(rad*.28f,0,rad*.16f,0x66000000);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,rad,p);p.clearShadowLayer();
            p.setColor(0x66ffffff);c.drawCircle(x-rad*.28f,y-rad*.32f,rad*.22f,p);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c); float w=getWidth(),h=getHeight();
            LinearGradient bg=new LinearGradient(0,0,w,h,0xff14172a,0xff282249,Shader.TileMode.CLAMP);p.setShader(bg);c.drawRect(0,0,w,h,p);p.setShader(null);
            float margin=Math.max(18*den,w*.045f), top=38*den;
            text(c,"HIT & BLOW",margin,top,25*den,Color.WHITE,Paint.Align.LEFT);
            text(c,"CPUの秘密のコード",margin,top+24*den,11*den,0xffaaaec7,Paint.Align.LEFT);
            restart.set(w-margin-94*den,top-27*den,w-margin,top+8*den);round(c,restart,18*den,0x22ffffff);text(c,"NEW GAME",restart.centerX(),restart.centerY()+4*den,10*den,Color.WHITE,Paint.Align.CENTER);

            float histTop=top+42*den, controlsH=205*den, histBottom=h-controlsH;
            int maxRows=Math.max(4,(int)((histBottom-histTop)/(34*den))); int start=Math.max(0,guesses.size()-maxRows);
            text(c,"TRY",margin,histTop,10*den,0xff858ba8,Paint.Align.LEFT); text(c,"RESULT",w-margin,histTop,10*den,0xff858ba8,Paint.Align.RIGHT);
            float rowY=histTop+23*den;
            for(int gi=start;gi<guesses.size();gi++){
                int[] g=guesses.get(gi),s=scores.get(gi); text(c,String.format(Locale.JAPAN,"%02d",gi+1),margin,rowY+5*den,10*den,0xff777e9c,Paint.Align.LEFT);
                float sx=margin+42*den; for(int j=0;j<4;j++) ball(c,sx+j*31*den,rowY,10*den,COLORS[g[j]]);
                text(c,s[0]+" HIT",w-margin-62*den,rowY+4*den,11*den,0xffff5878,Paint.Align.RIGHT); text(c,s[1]+" BLOW",w-margin,rowY+4*den,11*den,0xfff5f5f8,Paint.Align.RIGHT); rowY+=34*den;
            }
            float panelTop=h-controlsH+8*den; RectF panel=new RectF(margin,panelTop,w-margin,h-12*den);round(c,panel,24*den,0xfff5f3f8);
            text(c,message,w/2,panelTop+25*den,12*den,0xff363147,Paint.Align.CENTER);
            float slotsY=panelTop+58*den, gap=Math.min(58*den,(w-2*margin-70*den)/4), startX=w/2-gap*1.5f;
            for(int i=0;i<4;i++){ p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*den);p.setColor(0xffc8c4d2);c.drawCircle(startX+i*gap,slotsY,17*den,p);if(pick[i]>=0)ball(c,startX+i*gap,slotsY,15*den,COLORS[pick[i]]); }
            undo.set(w-margin-48*den,slotsY-19*den,w-margin,slotsY+19*den);round(c,undo,18*den,0xffddd9e6);text(c,"⌫",undo.centerX(),undo.centerY()+7*den,20*den,0xff393448,Paint.Align.CENTER);
            colorButtons.clear(); float cy=panelTop+105*den, cg=Math.min(48*den,(w-2*margin-28*den)/6), cs=w/2-cg*2.5f;
            for(int i=0;i<6;i++){RectF b=new RectF(cs+i*cg-19*den,cy-19*den,cs+i*cg+19*den,cy+19*den);colorButtons.add(b);ball(c,b.centerX(),b.centerY(),17*den,COLORS[i]);}
            duplicate.set(margin,panelTop+137*den,margin+122*den,panelTop+169*den);round(c,duplicate,16*den,allowDuplicate?0xff6550d8:0xffddd9e6);text(c,"同じ色 "+(allowDuplicate?"あり":"なし"),duplicate.centerX(),duplicate.centerY()+4*den,10*den,allowDuplicate?Color.WHITE:0xff4c475b,Paint.Align.CENTER);
            submit.set(w-margin-112*den,panelTop+134*den,w-margin,panelTop+172*den);round(c,submit,19*den,over?0xffaaa6b2:0xff5140c7);text(c,over?"もう一度":"判 定",submit.centerX(),submit.centerY()+5*den,12*den,Color.WHITE,Paint.Align.CENTER);
        }
        int filled(){int n=0;for(int v:pick)if(v>=0)n++;return n;}
        void addColor(int v){int n=filled();if(n>=4)return;if(!allowDuplicate)for(int x:pick)if(x==v)return;pick[n]=v;}
        void judge(){
            if(over){newGame();return;} if(filled()<4){message="色を4つ選んでね";return;}
            int hit=0,blow=0;boolean[] a=new boolean[4],g=new boolean[4];
            for(int i=0;i<4;i++)if(pick[i]==answer[i]){hit++;a[i]=g[i]=true;}
            for(int i=0;i<4;i++)if(!g[i])for(int j=0;j<4;j++)if(!a[j]&&pick[i]==answer[j]){blow++;a[j]=true;break;}
            guesses.add(pick.clone());scores.add(new int[]{hit,blow});Arrays.fill(pick,-1);
            if(hit==4){won=over=true;message="CLEAR!  "+guesses.size()+"回で正解";performHapticFeedback(HapticFeedbackConstants.CONFIRM);}
            else if(guesses.size()>=10){over=true;message="GAME OVER  正解を確認して再挑戦";}
            else message=hit+" HIT  /  "+blow+" BLOW";
        }
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();
            for(int i=0;i<colorButtons.size();i++)if(colorButtons.get(i).contains(x,y)){addColor(i);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);invalidate();return true;}
            if(undo.contains(x,y)){int n=filled();if(n>0)pick[n-1]=-1;invalidate();return true;}
            if(submit.contains(x,y)){judge();invalidate();return true;}
            if(restart.contains(x,y)){newGame();return true;}
            if(duplicate.contains(x,y)){allowDuplicate=!allowDuplicate;newGame();return true;}
            return true;
        }
    }
}
