package dev.ryu4696.hitandblow;

import android.app.Activity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.*;
import android.animation.ValueAnimator;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import java.util.*;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(0xff292929); setContentView(new BoardView());
    }
    private class BoardView extends View {
        final int[] COLORS={0xff1596d2,0xffe94b45,0xff65ad4e,0xffffcc35,0xffe85b9e,0xffecebe6};
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        final ArrayList<int[]> guesses=new ArrayList<>(), results=new ArrayList<>(), paletteColors=new ArrayList<>();
        final ArrayList<RectF> palette=new ArrayList<>();
        final RectF ok=new RectF(), back=new RectF(), reset=new RectF(), duplicate=new RectF();
        final int[] current={-1,-1,-1,-1}; int[] answer=new int[4];
        int selected=-1; boolean answerDuplicates=false, ended=false;
        String message="色の玉を4つ並べよう"; float d;
        float placeT=1f, pinT=1f, revealT=0f, celebrateT=0f;
        float slotStartX, currentY, slotGap, paletteY;
        int movingColor=-1, movingSlot=-1, animatedRow=-1;
        boolean victory=false;
        final ToneGenerator tones=new ToneGenerator(AudioManager.STREAM_MUSIC,38);

        BoardView(){super(MainActivity.this);d=getResources().getDisplayMetrics().density;setLayerType(View.LAYER_TYPE_SOFTWARE,null);newGame();}
        void newGame(){guesses.clear();results.clear();Arrays.fill(current,-1);selected=-1;ended=false;victory=false;placeT=pinT=1f;revealT=celebrateT=0f;ArrayList<Integer> bag=new ArrayList<>();for(int i=0;i<6;i++)bag.add(i);Collections.shuffle(bag);Random r=new Random();for(int i=0;i<4;i++)answer[i]=answerDuplicates?r.nextInt(6):bag.get(i);message="色の玉を4つ並べよう";invalidate();}
        void txt(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(align);p.setTextSize(size);p.setColor(color);c.drawText(s,x,y,p);}
        void rr(Canvas c,RectF r,float rad,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(r,rad,rad,p);}
        void hole(Canvas c,float x,float y,float r){p.setShadowLayer(r*.25f,0,r*.15f,0x66000000);p.setColor(0xffb9b7b0);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setColor(0x55ffffff);c.drawCircle(x-r*.22f,y-r*.28f,r*.22f,p);}
        void ball(Canvas c,float x,float y,float r,int color){
            p.setShadowLayer(r*.34f,0,r*.24f,0x88000000);p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(x-r*.32f,y-r*.38f,r*1.28f,new int[]{mix(color,Color.WHITE,.48f),color,mix(color,Color.BLACK,.42f)},new float[]{0,.48f,1},Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setShader(null);
            p.setColor(0xaaffffff);c.drawOval(new RectF(x-r*.47f,y-r*.55f,x-r*.08f,y-r*.18f),p);p.setColor(0x33000000);c.drawArc(new RectF(x-r*.74f,y-r*.74f,x+r*.74f,y+r*.74f),18,105,false,p);
        }
        int mix(int a,int b,float t){return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));}
        void animate(ValueAnimator a){a.addUpdateListener(v->invalidate());a.start();}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();p.setShader(new LinearGradient(0,0,w,h,0xffeeeeea,0xffc8c6bf,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            float side=Math.max(12*d,w*.025f),top=13*d;
            p.setColor(0x22000000);for(int i=0;i<18;i++){float yy=(i*53*d)%(h+40*d)-20*d;c.drawLine(0,yy,w,yy+10*d,p);}
            txt(c,"HIT & BLOW",side,top+18*d,19*d,0xff323232,Paint.Align.LEFT);
            txt(c,"● ヒット：色と場所が正解",side,top+40*d,10*d,0xff764b32,Paint.Align.LEFT);txt(c,"○ ブロー：色だけ正解",side+151*d,top+40*d,10*d,0xff555555,Paint.Align.LEFT);
            reset.set(w-side-86*d,top,w-side,top+32*d);rr(c,reset,16*d,0xff555555);txt(c,"やり直す",reset.centerX(),reset.centerY()+4*d,10*d,Color.WHITE,Paint.Align.CENTER);
            duplicate.set(w-side-207*d,top,w-side-95*d,top+32*d);rr(c,duplicate,16*d,answerDuplicates?0xffd45f45:0xff77736e);txt(c,"答えの同色 "+(answerDuplicates?"あり":"なし"),duplicate.centerX(),duplicate.centerY()+4*d,9*d,Color.WHITE,Paint.Align.CENTER);
            float boardTop=top+54*d,boardBottom=h-79*d,rowH=(boardBottom-boardTop)/8f,boardLeft=side,boardRight=w-side;rr(c,new RectF(boardLeft,boardTop,boardRight,boardBottom),10*d,0xffdedcd5);
            float numberX=boardLeft+22*d,ballsStart=boardLeft+68*d,ballGap=Math.min(42*d,(w*.43f)/4f),br=Math.min(14*d,rowH*.29f),feedbackX=ballsStart+4*ballGap+35*d;
            slotStartX=ballsStart;slotGap=ballGap;currentY=boardTop+rowH*(guesses.size()+.5f);
            for(int row=0;row<8;row++){
                float y=boardTop+rowH*(row+.5f);if(row==guesses.size()&&!ended)rr(c,new RectF(boardLeft+5*d,boardTop+row*rowH+3*d,boardRight-5*d,boardTop+(row+1)*rowH-3*d),7*d,0x66ffffff);
                txt(c,String.valueOf(row+1),numberX,y+4*d,10*d,0xff77736d,Paint.Align.CENTER);int[] vals=row<guesses.size()?guesses.get(row):(row==guesses.size()?current:null);
                for(int j=0;j<4;j++){float x=ballsStart+j*ballGap;hole(c,x,y,br);if(vals!=null&&vals[j]>=0&&!(row==guesses.size()&&j==movingSlot&&placeT<1f))ball(c,x,y,br*.9f,COLORS[vals[j]]);}
                for(int k=0;k<4;k++){float fx=feedbackX+(k%2)*15*d,fy=y+(k/2-.5f)*15*d;hole(c,fx,fy,4.7f*d);}
                if(row<results.size()){int hit=results.get(row)[0],blow=results.get(row)[1],n=0,show=row==animatedRow?(int)Math.floor(pinT*4.01f):4;for(int k=0;k<hit;k++,n++)if(n<show){float fx=feedbackX+(n%2)*15*d,fy=y+(n/2-.5f)*15*d;ball(c,fx,fy,4.2f*d,0xff9b6039);}for(int k=0;k<blow;k++,n++)if(n<show){float fx=feedbackX+(n%2)*15*d,fy=y+(n/2-.5f)*15*d;ball(c,fx,fy,4.2f*d,0xfffaf9f5);}}
                if(row<7){p.setColor(0x33706d68);p.setStrokeWidth(1);c.drawLine(boardLeft+9*d,boardTop+(row+1)*rowH,boardRight-9*d,boardTop+(row+1)*rowH,p);}
            }
            float infoX=feedbackX+52*d;txt(c,ended?"ANSWER":"TURN",infoX,boardTop+24*d,10*d,0xff6b6862,Paint.Align.LEFT);txt(c,ended?"":String.valueOf(guesses.size()+1)+" / 8",infoX,boardTop+58*d,22*d,0xff343434,Paint.Align.LEFT);
            if(ended){for(int i=0;i<4;i++)ball(c,infoX+i*34*d,boardTop+57*d,11*d,COLORS[answer[i]]);float lidY=boardTop+36*d-revealT*48*d;RectF lid=new RectF(infoX-9*d,lidY,infoX+119*d,lidY+43*d);rr(c,lid,7*d,0xff7b5539);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*d);p.setColor(0x55ffffff);c.drawRoundRect(lid,7*d,7*d,p);}
            txt(c,message,infoX,boardTop+108*d,11*d,0xff333333,Paint.Align.LEFT);
            if(!ended){txt(c,"選んだ玉は順番に入ります",infoX,boardTop+122*d,9*d,0xff77736d,Paint.Align.LEFT);txt(c,"予想には同じ色も何度でも使えます",infoX,boardTop+141*d,9*d,0xff77736d,Paint.Align.LEFT);}
            float cy=h-40*d,palGap=Math.min(47*d,(w*.48f)/6f),palStart=side+25*d;paletteY=cy;palette.clear();for(int i=0;i<6;i++){float x=palStart+i*palGap;RectF r=new RectF(x-19*d,cy-19*d,x+19*d,cy+19*d);palette.add(r);hole(c,x,cy,16*d);if(i==selected){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*d);p.setColor(0xff3d3a36);c.drawCircle(x,cy,20*d,p);}ball(c,x,cy,14*d,COLORS[i]);}
            back.set(w-side-184*d,h-59*d,w-side-120*d,h-21*d);rr(c,back,19*d,0xff77736e);txt(c,"もどす",back.centerX(),back.centerY()+4*d,10*d,Color.WHITE,Paint.Align.CENTER);
            ok.set(w-side-108*d,h-61*d,w-side,h-19*d);rr(c,ok,21*d,ended?0xffde7337:(filled()==4?0xffe86a32:0xffaaa7a1));txt(c,ended?"もう一度":"OK",ok.centerX(),ok.centerY()+5*d,13*d,Color.WHITE,Paint.Align.CENTER);
            if(placeT<1f&&movingColor>=0&&movingColor<palette.size()){float sx=palette.get(movingColor).centerX(),sy=paletteY,ex=slotStartX+movingSlot*slotGap,ey=currentY;float t=placeT,x=sx+(ex-sx)*t,y=sy+(ey-sy)*t-(float)Math.sin(Math.PI*t)*38*d;ball(c,x,y,14*d*(1f+.12f*(float)Math.sin(Math.PI*t)),COLORS[movingColor]);}
            if(victory&&celebrateT>0){for(int i=0;i<26;i++){float phase=(i*.137f+celebrateT)%1f,x=(i*97%Math.max(1,(int)w)),y=phase*h;p.setColor(COLORS[i%6]);p.setStyle(Paint.Style.FILL);c.save();c.rotate(i*31+celebrateT*360,x,y);c.drawRect(x-3*d,y-6*d,x+3*d,y+6*d,p);c.restore();}}
        }
        int filled(){int n=0;for(int v:current)if(v>=0)n++;return n;}
        void choose(int value){if(ended)return;int n=filled();if(n<4){current[n]=value;selected=value;movingColor=value;movingSlot=n;placeT=0;tones.startTone(ToneGenerator.TONE_PROP_BEEP,35);ValueAnimator a=ValueAnimator.ofFloat(0,1);a.setDuration(310);a.setInterpolator(new OvershootInterpolator(.7f));a.addUpdateListener(v->placeT=(float)v.getAnimatedValue());animate(a);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}}
        void undo(){int n=filled();if(n>0)current[n-1]=-1;selected=n>1?current[n-2]:-1;}
        void judge(){if(ended){newGame();return;}if(filled()<4){message="4つ並べてからOK";return;}boolean[] usedA=new boolean[4],usedG=new boolean[4];int hit=0,blow=0;for(int i=0;i<4;i++)if(current[i]==answer[i]){hit++;usedA[i]=usedG[i]=true;}for(int i=0;i<4;i++)if(!usedG[i])for(int j=0;j<4;j++)if(!usedA[j]&&current[i]==answer[j]){blow++;usedA[j]=true;break;}guesses.add(current.clone());results.add(new int[]{hit,blow});animatedRow=guesses.size()-1;pinT=0;Arrays.fill(current,-1);selected=-1;tones.startTone(ToneGenerator.TONE_PROP_ACK,90);ValueAnimator pins=ValueAnimator.ofFloat(0,1);pins.setDuration(620);pins.setInterpolator(new OvershootInterpolator(.45f));pins.addUpdateListener(v->pinT=(float)v.getAnimatedValue());animate(pins);if(hit==4){ended=true;victory=true;message="4ヒット！ 正解！";reveal();performHapticFeedback(HapticFeedbackConstants.CONFIRM);}else if(guesses.size()==8){ended=true;message="8ターン終了";reveal();}else message=hit+"ヒット  "+blow+"ブロー";}
        void reveal(){ValueAnimator a=ValueAnimator.ofFloat(0,1);a.setStartDelay(420);a.setDuration(650);a.setInterpolator(new DecelerateInterpolator());a.addUpdateListener(v->{revealT=(float)v.getAnimatedValue();celebrateT=victory?revealT:0;});animate(a);if(victory)tones.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,300);}
        @Override protected void onDetachedFromWindow(){tones.release();super.onDetachedFromWindow();}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();for(int i=0;i<palette.size();i++)if(palette.get(i).contains(x,y)){choose(i);invalidate();return true;}if(back.contains(x,y)){undo();invalidate();return true;}if(ok.contains(x,y)){judge();invalidate();return true;}if(reset.contains(x,y)){newGame();return true;}if(duplicate.contains(x,y)){answerDuplicates=!answerDuplicates;newGame();return true;}return true;}
    }
}
