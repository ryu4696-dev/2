package dev.ryu4696.hitandblow;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.*;
import android.view.animation.*;
import java.util.*;

public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.BLACK);setContentView(new Game());}

 class Game extends View {
  final int[] C={0xff2459df,0xffed3838,0xff25bd43,0xffffd21d,0xffe85ccf,0xffe9eeeb};
  final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final Path path=new Path();
  final ArrayList<int[]> tries=new ArrayList<>(), marks=new ArrayList<>(); final int[] now={-1,-1,-1,-1};
  final ArrayList<RectF> palette=new ArrayList<>(); final RectF ok=new RectF(),undo=new RectF(),restart=new RectF(),dup=new RectF();
  final ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_MUSIC,35); int[] answer=new int[4];
  boolean duplicates=false,ended=false,win=false; int movingColor=-1,movingSlot=-1,markColumn=-1;
  float putT=1,pinT=1,lidT=0,partyT=0; float palY,colX,colW,slotY0,slotDy;
  Game(){super(MainActivity.this);setLayerType(LAYER_TYPE_SOFTWARE,null);newGame();}
  void newGame(){tries.clear();marks.clear();Arrays.fill(now,-1);ended=win=false;putT=pinT=1;lidT=partyT=0;ArrayList<Integer>b=new ArrayList<>();for(int i=0;i<6;i++)b.add(i);Collections.shuffle(b);Random r=new Random();for(int i=0;i<4;i++)answer[i]=duplicates?r.nextInt(6):b.get(i);invalidate();}
  int filled(){int n=0;for(int v:now)if(v>=0)n++;return n;}
  void rect(Canvas c,RectF r,float rad,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.clearShadowLayer();c.drawRoundRect(r,rad,rad,p);}
  void text(Canvas c,String s,float x,float y,float z,int color,Paint.Align a){p.setShader(null);p.setStyle(Paint.Style.FILL);p.clearShadowLayer();p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextAlign(a);p.setTextSize(z);p.setColor(color);c.drawText(s,x,y,p);}
  int mix(int a,int b,float t){return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));}
  void metal(Canvas c,RectF r,float rad){p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xffcbd2d2,0xff707a7a,0xffaeb6b5,0xff555e5e},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);p.setShadowLayer(8,0,4,0x88000000);c.drawRoundRect(r,rad,rad,p);p.clearShadowLayer();p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,r.width()*.025f));p.setColor(0x99eef4f2);c.drawRoundRect(new RectF(r.left+3,r.top+3,r.right-3,r.bottom-3),rad,rad,p);}
  void hole(Canvas c,float x,float y,float r){p.setShader(new RadialGradient(x-r*.2f,y-r*.2f,r,new int[]{0xff171411,0xff3d3a36,0xff858b88},null,Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);p.setShadowLayer(r*.22f,0,r*.15f,0xaa000000);c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setShader(null);}
  void marble(Canvas c,float x,float y,float r,int idx){int base=C[idx];p.setShader(new RadialGradient(x-r*.35f,y-r*.42f,r*1.35f,new int[]{mix(base,Color.WHITE,.52f),base,mix(base,Color.BLACK,.48f)},new float[]{0,.45f,1},Shader.TileMode.CLAMP));p.setStyle(Paint.Style.FILL);p.setShadowLayer(r*.25f,0,r*.22f,0x99000000);c.drawCircle(x,y,r,p);p.clearShadowLayer();p.setShader(null);c.save();path.reset();path.addCircle(x,y,r*.88f,Path.Direction.CW);c.clipPath(path);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(r*.22f);p.setColor(0xaaffffff);
   if(idx==0){c.drawLine(x-r,y,x+r,y,p);c.drawLine(x,y-r,x,y+r,p);}
   else if(idx==1){p.setStyle(Paint.Style.FILL);float q=r*.62f;for(int a=-2;a<2;a++)for(int b=-2;b<2;b++)if((a+b)%2==0)c.drawRect(x+a*q,y+b*q,x+(a+1)*q,y+(b+1)*q,p);}
   else if(idx==2){for(int k=-1;k<=1;k++){path.reset();path.moveTo(x-r*1.2f,y+k*r*.75f);path.cubicTo(x-r*.55f,y+(k-.8f)*r*.75f,x+r*.15f,y+(k+.8f)*r*.75f,x+r*1.2f,y+k*r*.75f);c.drawPath(path,p);}}
   else if(idx==3){p.setStyle(Paint.Style.FILL);for(int a=-2;a<2;a++)for(int b=-2;b<2;b++){path.reset();path.moveTo(x+a*r*.75f,y+b*r*.75f-r*.28f);path.lineTo(x+a*r*.75f+r*.28f,y+b*r*.75f);path.lineTo(x+a*r*.75f,y+b*r*.75f+r*.28f);path.lineTo(x+a*r*.75f-r*.28f,y+b*r*.75f);path.close();c.drawPath(path,p);}}
   else if(idx==4){for(int k=-2;k<=2;k++)c.drawLine(x-r*1.5f+k*r*.62f,y+r,x+r*.5f+k*r*.62f,y-r,p);}
   else {for(int k=-2;k<=2;k++)c.drawLine(x-r,y+k*r*.5f,x+r,y+k*r*.5f,p);}c.restore();p.setStyle(Paint.Style.FILL);p.setColor(0xaaffffff);c.drawOval(new RectF(x-r*.48f,y-r*.56f,x-r*.12f,y-r*.22f),p);
  }
  void avatar(Canvas c,float x,float y,float r,int n){p.setColor(n%2==0?0xffb52b21:0xff665281);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);p.setColor(0xffe6d2ba);c.drawCircle(x,y+r*.05f,r*.62f,p);p.setColor(n%2==0?0xff63351f:0xff252020);c.drawArc(new RectF(x-r*.62f,y-r*.62f,x+r*.62f,y+r*.36f),180,180,true,p);p.setColor(0xff292522);c.drawCircle(x-r*.2f,y+r*.05f,r*.055f,p);c.drawCircle(x+r*.2f,y+r*.05f,r*.055f,p);}
  @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();
   p.setShader(new LinearGradient(0,0,0,h,new int[]{0xffb97c34,0xffd19d50,0xff9e6429},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);p.setColor(0x22522c10);for(int i=0;i<24;i++)c.drawOval(new RectF((i*149)%w-100,(i*67)%h,(i*149)%w+260,(i*67)%h+5),p);
   float m=w*.035f,top=h*.07f; text(c,"●  ヒット：色と場所が正解",w*.29f,top,h*.034f,Color.WHITE,Paint.Align.CENTER);text(c,"○  ブロー：色だけ正解",w*.61f,top,h*.034f,Color.WHITE,Paint.Align.CENTER);
   RectF board=new RectF(m,h*.105f,w-m,h*.91f);rect(c,board,h*.035f,0xff4d5756);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.012f);p.setColor(0xffd9e0df);c.drawRoundRect(board,h*.035f,h*.035f,p);
   float left=w*.115f,right=w*.82f;colW=(right-left)/8f;slotY0=h*.40f;slotDy=h*.135f;
   for(int col=0;col<8;col++){float cx=left+colW*(col+.5f);if(col<7){p.setColor(0xffadb5b3);p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(cx+colW*.38f,h*.178f);path.lineTo(cx+colW*.55f,h*.155f);path.lineTo(cx+colW*.55f,h*.201f);path.close();c.drawPath(path,p);}avatar(c,cx,h*.157f,h*.036f,col);
    RectF pegBox=new RectF(cx-colW*.28f,h*.215f,cx+colW*.28f,h*.32f);rect(c,pegBox,h*.012f,0xff626d6c);for(int i=0;i<4;i++)hole(c,cx+(i%2-.5f)*colW*.25f,h*.247f+(i/2)*h*.042f,h*.012f);
    RectF tray=new RectF(cx-colW*.34f,h*.34f,cx+colW*.34f,h*.845f);metal(c,tray,h*.025f);if(col==tries.size()&&!ended){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.007f);p.setColor(0xffff42c5);c.drawRoundRect(new RectF(tray.left-3,tray.top-3,tray.right+3,tray.bottom+3),h*.026f,h*.026f,p);}int[] vals=col<tries.size()?tries.get(col):(col==tries.size()?now:null);
    for(int s=0;s<4;s++){float y=slotY0+s*slotDy;hole(c,cx,y,h*.03f);if(vals!=null&&vals[s]>=0&&!(col==tries.size()&&s==movingSlot&&putT<1))marble(c,cx,y,h*.033f,vals[s]);}
    if(col<marks.size()){int hit=marks.get(col)[0],blow=marks.get(col)[1],n=0,show=col==markColumn?(int)(pinT*4.01f):4;for(int k=0;k<hit;k++,n++)if(n<show)marble(c,cx+(n%2-.5f)*colW*.25f,h*.247f+(n/2)*h*.042f,h*.012f,1);for(int k=0;k<blow;k++,n++)if(n<show)marble(c,cx+(n%2-.5f)*colW*.25f,h*.247f+(n/2)*h*.042f,h*.012f,5);}
   }
   colX=left+colW*(tries.size()+.5f);
   float ax=w*.90f;RectF answerCase=new RectF(ax-colW*.38f,h*.33f,ax+colW*.38f,h*.85f);metal(c,answerCase,h*.018f);for(int i=0;i<4;i++){hole(c,ax,slotY0+i*slotDy,h*.03f);marble(c,ax,slotY0+i*slotDy,h*.033f,answer[i]);}RectF lid=new RectF(answerCase.left-2+lidT*colW*.9f,answerCase.top-5,answerCase.right+2+lidT*colW*.9f,answerCase.bottom+5);rect(c,lid,h*.018f,0xff828b89);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xffdce2e0);c.drawRoundRect(lid,h*.018f,h*.018f,p);
   palY=h*.885f;RectF rack=new RectF(w*.27f,h*.83f,w*.76f,h*.94f);metal(c,rack,h*.035f);palette.clear();for(int i=0;i<6;i++){float x=w*.34f+i*w*.071f;RectF r=new RectF(x-h*.045f,palY-h*.045f,x+h*.045f,palY+h*.045f);palette.add(r);hole(c,x,palY,h*.038f);marble(c,x,palY,h*.041f,i);}
   undo.set(w*.79f,h*.86f,w*.86f,h*.925f);ok.set(w*.87f,h*.86f,w*.955f,h*.925f);rect(c,undo,h*.02f,0xff747e7c);rect(c,ok,h*.02f,filled()==4||ended?0xffff8b25:0xff7c8583);text(c,"↶",undo.centerX(),undo.centerY()+h*.012f,h*.036f,Color.WHITE,Paint.Align.CENTER);text(c,ended?"NEW":"OK",ok.centerX(),ok.centerY()+h*.009f,h*.026f,Color.WHITE,Paint.Align.CENTER);
   restart.set(w*.91f,h*.12f,w*.965f,h*.18f);dup.set(w*.845f,h*.12f,w*.90f,h*.18f);rect(c,restart,h*.018f,0xff6d7775);rect(c,dup,h*.018f,duplicates?0xffff5fc8:0xff6d7775);text(c,"↻",restart.centerX(),restart.centerY()+h*.01f,h*.027f,Color.WHITE,Paint.Align.CENTER);text(c,"×2",dup.centerX(),dup.centerY()+h*.009f,h*.021f,Color.WHITE,Paint.Align.CENTER);
   if(putT<1&&movingColor>=0){float sx=palette.get(movingColor).centerX(),ex=colX,ey=slotY0+movingSlot*slotDy,t=putT,x=sx+(ex-sx)*t,y=palY+(ey-palY)*t-(float)Math.sin(Math.PI*t)*h*.11f;marble(c,x,y,h*.041f,movingColor);}
   if(win&&partyT>0)for(int i=0;i<30;i++){float x=(i*83)%w,y=((i*.173f+partyT)%1)*h;p.setColor(C[i%6]);p.setStyle(Paint.Style.FILL);c.save();c.rotate(i*29+partyT*360,x,y);c.drawRect(x-4,y-8,x+4,y+8,p);c.restore();}
  }
  void anim(ValueAnimator a){a.addUpdateListener(v->invalidate());a.start();}
  void choose(int color){if(ended||filled()==4)return;int s=filled();now[s]=color;movingColor=color;movingSlot=s;putT=0;tone.startTone(ToneGenerator.TONE_PROP_BEEP,35);ValueAnimator a=ValueAnimator.ofFloat(0,1);a.setDuration(330);a.setInterpolator(new OvershootInterpolator(.55f));a.addUpdateListener(v->putT=(float)v.getAnimatedValue());anim(a);performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}
  void judge(){if(ended){newGame();return;}if(filled()<4)return;boolean[]ua=new boolean[4],ug=new boolean[4];int hit=0,blow=0;for(int i=0;i<4;i++)if(now[i]==answer[i]){hit++;ua[i]=ug[i]=true;}for(int i=0;i<4;i++)if(!ug[i])for(int j=0;j<4;j++)if(!ua[j]&&now[i]==answer[j]){blow++;ua[j]=true;break;}tries.add(now.clone());marks.add(new int[]{hit,blow});Arrays.fill(now,-1);markColumn=tries.size()-1;pinT=0;tone.startTone(ToneGenerator.TONE_PROP_ACK,90);ValueAnimator pins=ValueAnimator.ofFloat(0,1);pins.setDuration(650);pins.setInterpolator(new OvershootInterpolator(.35f));pins.addUpdateListener(v->pinT=(float)v.getAnimatedValue());anim(pins);if(hit==4||tries.size()==8){ended=true;win=hit==4;ValueAnimator lid=ValueAnimator.ofFloat(0,1);lid.setStartDelay(450);lid.setDuration(700);lid.setInterpolator(new DecelerateInterpolator());lid.addUpdateListener(v->{lidT=(float)v.getAnimatedValue();partyT=win?lidT:0;});anim(lid);if(win)tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,320);}}
  @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=e.getX(),y=e.getY();for(int i=0;i<palette.size();i++)if(palette.get(i).contains(x,y)){choose(i);invalidate();return true;}if(undo.contains(x,y)){int n=filled();if(n>0)now[n-1]=-1;invalidate();}else if(ok.contains(x,y)){judge();invalidate();}else if(restart.contains(x,y))newGame();else if(dup.contains(x,y)){duplicates=!duplicates;newGame();}return true;}
  @Override protected void onDetachedFromWindow(){tone.release();super.onDetachedFromWindow();}
 }
}
