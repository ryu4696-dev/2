package dev.ryu4696.hitandblow;

import android.app.Activity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.*;
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

        BoardView(){super(MainActivity.this);d=getResources().getDisplayMetrics().density;setLayerType(View.LAYER_TYPE_SOFTWARE,null);newGame();}
        void newGame(){guesses.clear();results.clear();Arrays.fill(current,-1);selected=-1;ended=false;ArrayList<Integer> bag=new ArrayList<>();for(int i=0;i<6;i++)bag.add(i);Collections.shuffle(bag);Random r=new Random();for(int i=0;i<4;i++)answer[i]=answerDuplicates?r.nextInt(6):bag.get(i);message="色の玉を4つ並べよう";invalidate();}
        void txt(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(align);p.setTextSize(size);p.setColor(color);c.drawText(s,x,y,p);}
        void rr(Canvas c,RectF r,float rad,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(r,rad,rad,p);}
        void hole(Canvas c,float x,float y,float r){p.setShadowLayer(r*.25f,0,r*.15f,0x66000000);p.setColor(0xffb9b7b0);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setColor(0x55ffffff);c.drawCircle(x-r*.22f,y-r*.28f,r*.22f,p);}
        void ball(Canvas c,float x,float y,float r,int color){p.setShadowLayer(r*.28f,0,r*.2f,0x77000000);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setColor(color==COLORS[5]?0x99ffffff:0x77ffffff);c.drawCircle(x-r*.27f,y-r*.31f,r*.21f,p);p.setColor(0x22000000);c.drawCircle(x+r*.18f,y+r*.2f,r*.62f,p);}
        @Override protected void onDraw(Canvas c){
            float w=getWidth(),h=getHeight();p.setShader(new LinearGradient(0,0,w,h,0xffeeeeea,0xffc8c6bf,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            float side=Math.max(12*d,w*.025f),top=13*d;txt(c,"ヒット＆ブロー",side,top+18*d,19*d,0xff323232,Paint.Align.LEFT);
            txt(c,"● ヒット：色と場所が正解",side,top+40*d,10*d,0xff764b32,Paint.Align.LEFT);txt(c,"○ ブロー：色だけ正解",side+151*d,top+40*d,10*d,0xff555555,Paint.Align.LEFT);
            reset.set(w-side-86*d,top,w-side,top+32*d);rr(c,reset,16*d,0xff555555);txt(c,"やり直す",reset.centerX(),reset.centerY()+4*d,10*d,Color.WHITE,Paint.Align.CENTER);
            duplicate.set(w-side-207*d,top,w-side-95*d,top+32*d);rr(c,duplicate,16*d,answerDuplicates?0xffd45f45:0xff77736e);txt(c,"答えの同色 "+(answerDuplicates?"あり":"なし"),duplicate.centerX(),duplicate.centerY()+4*d,9*d,Color.WHITE,Paint.Align.CENTER);
            float boardTop=top+54*d,boardBottom=h-79*d,rowH=(boardBottom-boardTop)/8f,boardLeft=side,boardRight=w-side;rr(c,new RectF(boardLeft,boardTop,boardRight,boardBottom),10*d,0xffdedcd5);
            float numberX=boardLeft+22*d,ballsStart=boardLeft+68*d,ballGap=Math.min(42*d,(w*.43f)/4f),br=Math.min(14*d,rowH*.29f),feedbackX=ballsStart+4*ballGap+35*d;
            for(int row=0;row<8;row++){
                float y=boardTop+rowH*(row+.5f);if(row==guesses.size()&&!ended)rr(c,new RectF(boardLeft+5*d,boardTop+row*rowH+3*d,boardRight-5*d,boardTop+(row+1)*rowH-3*d),7*d,0x66ffffff);
                txt(c,String.valueOf(row+1),numberX,y+4*d,10*d,0xff77736d,Paint.Align.CENTER);int[] vals=row<guesses.size()?guesses.get(row):(row==guesses.size()?current:null);
                for(int j=0;j<4;j++){float x=ballsStart+j*ballGap;hole(c,x,y,br);if(vals!=null&&vals[j]>=0)ball(c,x,y,br*.9f,COLORS[vals[j]]);}
                for(int k=0;k<4;k++){float fx=feedbackX+(k%2)*15*d,fy=y+(k/2-.5f)*15*d;hole(c,fx,fy,4.7f*d);}
                if(row<results.size()){int hit=results.get(row)[0],blow=results.get(row)[1],n=0;for(int k=0;k<hit;k++,n++){float fx=feedbackX+(n%2)*15*d,fy=y+(n/2-.5f)*15*d;ball(c,fx,fy,4.2f*d,0xff8d522e);}for(int k=0;k<blow;k++,n++){float fx=feedbackX+(n%2)*15*d,fy=y+(n/2-.5f)*15*d;ball(c,fx,fy,4.2f*d,0xfff8f7f3);}}
                if(row<7){p.setColor(0x33706d68);p.setStrokeWidth(1);c.drawLine(boardLeft+9*d,boardTop+(row+1)*rowH,boardRight-9*d,boardTop+(row+1)*rowH,p);}
            }
            float infoX=feedbackX+52*d;txt(c,ended?"答え":"残り",infoX,boardTop+24*d,10*d,0xff6b6862,Paint.Align.LEFT);txt(c,ended?"":String.valueOf(8-guesses.size()),infoX,boardTop+58*d,27*d,0xff343434,Paint.Align.LEFT);
            if(ended)for(int i=0;i<4;i++)ball(c,infoX+i*34*d,boardTop+57*d,11*d,COLORS[answer[i]]);txt(c,message,infoX,boardTop+98*d,11*d,0xff333333,Paint.Align.LEFT);
            if(!ended){txt(c,"選んだ玉は順番に入ります",infoX,boardTop+122*d,9*d,0xff77736d,Paint.Align.LEFT);txt(c,"予想には同じ色も何度でも使えます",infoX,boardTop+141*d,9*d,0xff77736d,Paint.Align.LEFT);}
            float cy=h-40*d,palGap=Math.min(47*d,(w*.48f)/6f),palStart=side+25*d;palette.clear();for(int i=0;i<6;i++){float x=palStart+i*palGap;RectF r=new RectF(x-19*d,cy-19*d,x+19*d,cy+19*d);palette.add(r);if(i==selected){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*d);p.setColor(0xff3d3a36);c.drawCircle(x,cy,20*d,p);}ball(c,x,cy,14*d,COLORS[i]);}
            back.set(w-side-184*d,h-59*d,w-side-120*d,h-21*d);rr(c,back,19*d,0xff77736e);txt(c,"もどす",back.centerX(),back.centerY()+4*d,10*d,Color.WHITE,Paint.Align.CENTER);
            ok.set(w-side-108*d,h-61*d,w-side,h-19*d);rr(c,ok,21*d,ended?0xffde7337:(filled()==4?0xffe86a32:0xffaaa7a1));txt(c,ended?"もう一度":"OK",ok.centerX(),ok.centerY()+5*d,13*d,Color.WHITE,Paint.Align.CENTER);
        }
        int filled(){int n=0;for(int v:current)if(v>=0)n++;return n;}
        void choose(int value){if(ended)return;int n=filled();if(n<4){current[n]=value;selected=value;performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}}
        void undo(){int n=filled();if(n>0)current[n-1]=-1;selected=n>1?current[n-2]:-1;}
        void judge(){if(ended){newGame();return;}if(filled()<4){message="4つ並べてからOK";return;}boolean[] usedA=new boolean[4],usedG=new boolean[4];int hit=0,blow=0;for(int i=0;i<4;i++)if(current[i]==answer[i]){hit++;usedA[i]=usedG[i]=true;}for(int i=0;i<4;i++)if(!usedG[i])for(int j=0;j<4;j++)if(!usedA[j]&&current[i]==answer[j]){blow++;usedA[j]=true;break;}guesses.add(current.clone());results.add(new int[]{hit,blow});Arrays.fill(current,-1);selected=-1;if(hit==4){ended=true;message="4ヒット！ 正解！";performHapticFeedback(HapticFeedbackConstants.CONFIRM);}else if(guesses.size()==8){ended=true;message="8ターン終了";}else message=hit+"ヒット  "+blow+"ブロー";}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();for(int i=0;i<palette.size();i++)if(palette.get(i).contains(x,y)){choose(i);invalidate();return true;}if(back.contains(x,y)){undo();invalidate();return true;}if(ok.contains(x,y)){judge();invalidate();return true;}if(reset.contains(x,y)){newGame();return true;}if(duplicate.contains(x,y)){answerDuplicates=!answerDuplicates;newGame();return true;}return true;}
    }
}
