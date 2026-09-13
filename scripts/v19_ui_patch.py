from pathlib import Path

p = Path('app/src/main/java/dev/ryu4696/hitandblow/MainActivity.java')
s = p.read_text(encoding='utf-8')


def must_replace(old, new):
    global s
    if old not in s:
        raise SystemExit('v19 UI patch target not found:\n' + old[:180])
    s = s.replace(old, new, 1)


def replace_method(start_sig, next_sig, replacement):
    global s
    start = s.index(start_sig)
    end = s.index(next_sig, start)
    s = s[:start] + replacement.rstrip() + '\n\n' + s[end:]

must_replace(
    '  final RectF ok=new RectF(),undo=new RectF(),gear=new RectF(),restart=new RectF(),option=new RectF(),close=new RectF();',
    '  final RectF ok=new RectF(),undo=new RectF(),gear=new RectF(),restart=new RectF(),homeBtn=new RectF(),option=new RectF(),close=new RectF();'
)

menu = r'''  void menuButton(Canvas c,RectF r,String label,float h){
   p.setShader(new LinearGradient(r.left,r.top,r.left,r.bottom,new int[]{0xff3c3b37,0xff202120,0xff161717},null,Shader.TileMode.CLAMP));
   p.setStyle(Paint.Style.FILL);p.setShadowLayer(h*.018f,0,h*.010f,0xcc000000);c.drawRoundRect(r,h*.018f,h*.018f,p);p.clearShadowLayer();p.setShader(null);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.0045f);p.setColor(0xffc9c1b2);c.drawRoundRect(r,h*.018f,h*.018f,p);
   p.setStrokeWidth(h*.0018f);p.setColor(0xff716859);c.drawRoundRect(new RectF(r.left+h*.010f,r.top+h*.010f,r.right-h*.010f,r.bottom-h*.010f),h*.012f,h*.012f,p);
   p.setStyle(Paint.Style.FILL);
   for(float sx:new float[]{r.left+h*.026f,r.right-h*.026f}){
    p.setShader(new RadialGradient(sx,r.centerY(),h*.013f,new int[]{0xffffdda0,0xff9a6b35,0xff2d2118},null,Shader.TileMode.CLAMP));
    c.drawCircle(sx,r.centerY(),h*.0105f,p);p.setShader(null);
   }
   text(c,label,r.centerX(),r.centerY()+h*.014f,h*.040f,0xffffead2);
  }

  void menuRack(Canvas c,RectF r,float h,int[] colors){
   p.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{0xff383632,0xff171818,0xff292824},null,Shader.TileMode.CLAMP));
   p.setStyle(Paint.Style.FILL);p.setShadowLayer(h*.014f,0,h*.008f,0xbb000000);c.drawRoundRect(r,h*.017f,h*.017f,p);p.clearShadowLayer();p.setShader(null);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.0038f);p.setColor(0xffbdb29e);c.drawRoundRect(r,h*.017f,h*.017f,p);
   p.setStrokeWidth(h*.0016f);p.setColor(0xff645b4d);c.drawRoundRect(new RectF(r.left+h*.009f,r.top+h*.009f,r.right-h*.009f,r.bottom-h*.009f),h*.011f,h*.011f,p);
   for(int i=0;i<4;i++)marble(c,r.centerX(),r.top+(i+.5f)*r.height()/4f,h*.034f,colors[i]);
  }

  void drawMenu(Canvas c,float w,float h){
   c.drawBitmap(bg,null,new RectF(0,0,w,h),p);

   // Cover the gameplay face completely.  The home screen is its own solid board,
   // not a translucent dialog floating over an active game.
   RectF board=new RectF(.030f*w,.105f*h,.970f*w,.805f*h);
   p.setShader(new LinearGradient(board.left,board.top,board.right,board.bottom,new int[]{0xff343331,0xff1c1d1d,0xff292825},null,Shader.TileMode.CLAMP));
   p.setStyle(Paint.Style.FILL);p.setShadowLayer(h*.025f,0,h*.012f,0xaa000000);c.drawRoundRect(board,h*.030f,h*.030f,p);p.clearShadowLayer();p.setShader(null);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.008f);p.setColor(0xffc8c0b3);c.drawRoundRect(board,h*.030f,h*.030f,p);
   p.setStrokeWidth(h*.002f);p.setColor(0xff696158);c.drawRoundRect(new RectF(board.left+h*.012f,board.top+h*.012f,board.right-h*.012f,board.bottom-h*.012f),h*.020f,h*.020f,p);

   // Brass corner screws, matching the game board.
   p.setStyle(Paint.Style.FILL);
   float sr=h*.0105f;
   for(float sx:new float[]{board.left+h*.025f,board.right-h*.025f})for(float sy:new float[]{board.top+h*.025f,board.bottom-h*.025f}){
    p.setShader(new RadialGradient(sx,sy,sr*1.5f,new int[]{0xffffdda0,0xff8f6231,0xff2a2019},null,Shader.TileMode.CLAMP));c.drawCircle(sx,sy,sr,p);p.setShader(null);
   }

   // Side peg racks are deliberately sparse. They use the exact same marbles as gameplay.
   RectF leftRack=new RectF(.090f*w,.235f*h,.172f*w,.705f*h);
   RectF rightRack=new RectF(.828f*w,.235f*h,.910f*w,.705f*h);
   menuRack(c,leftRack,h,new int[]{0,2,1,3});
   menuRack(c,rightRack,h,new int[]{4,5,2,1});

   // Metallic title: warm HIT, silver ampersand/BLOW, like the approved concept.
   p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setStyle(Paint.Style.FILL);p.setShadowLayer(h*.008f,0,h*.005f,0xaa000000);
   p.setTextSize(h*.089f);p.setColor(0xffd79b60);c.drawText("HIT",.405f*w,.275f*h,p);
   p.setColor(0xffded7cb);c.drawText("&",.500f*w,.275f*h,p);c.drawText("BLOW",.615f*w,.275f*h,p);p.clearShadowLayer();
   text(c,"ヒット＆ブロー",.5f*w,.337f*h,h*.027f,0xffeee0ca);
   peg(c,.421f*w,.326f*h,h*.010f,true);peg(c,.579f*w,.326f*h,h*.010f,false);

   soloBtn.set(.325f*w,.430f*h,.675f*w,.555f*h);
   onlineBtn.set(.325f*w,.600f*h,.675f*w,.725f*h);
   menuButton(c,soloBtn,"ひとりで遊ぶ",h);
   menuButton(c,onlineBtn,"オンライン対戦",h);
  }'''
replace_method('  void drawMenu(Canvas c,float w,float h){', '  void drawLobby(Canvas c,float w,float h){', menu)

old = '   gear.set(.843f*w,.015f*h,.91f*w,.095f*h);restart.set(.925f*w,.015f*h,.985f*w,.095f*h);'
new = r'''   homeBtn.set(.015f*w,.015f*h,.075f*w,.095f*h);
   rr(c,homeBtn,h*.012f,0xff292b2c);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.0025f);p.setColor(0xffb89a68);c.drawRoundRect(homeBtn,h*.012f,h*.012f,p);
   float hcx=homeBtn.centerX(),hcy=homeBtn.centerY();
   p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xffeee1ca);path.reset();
   path.moveTo(hcx-h*.024f,hcy-h*.002f);path.lineTo(hcx,hcy-h*.027f);path.lineTo(hcx+h*.024f,hcy-h*.002f);path.close();c.drawPath(path,p);
   c.drawRect(hcx-h*.017f,hcy-h*.003f,hcx+h*.017f,hcy+h*.023f,p);
   p.setColor(0xff292b2c);c.drawRect(hcx-h*.005f,hcy+h*.009f,hcx+h*.005f,hcy+h*.023f,p);

   gear.set(.843f*w,.015f*h,.91f*w,.095f*h);restart.set(.925f*w,.015f*h,.985f*w,.095f*h);'''
must_replace(old, new)

old_touch = '''   if(ok.contains(x,y)){
    if(online&&ended)leaveOnline();else judge();
   }else if(undo.contains(x,y)){'''
new_touch = '''   if(homeBtn.contains(x,y)){
    settings=false;
    if(online)leaveOnline();
    else {screen=MENU;selectedColor=-1;dragging=false;invalidate();}
    return true;
   }else if(ok.contains(x,y)){
    if(online&&ended)leaveOnline();else judge();
   }else if(undo.contains(x,y)){'''
must_replace(old_touch, new_touch)

p.write_text(s, encoding='utf-8')

# Build this UI revision as v19.
g = Path('app/build.gradle.kts')
gs = g.read_text(encoding='utf-8')
gs = gs.replace('versionCode = 17', 'versionCode = 19')
gs = gs.replace('versionName = "17.0"', 'versionName = "19.0"')
g.write_text(gs, encoding='utf-8')

print('Applied v19 opaque home/menu UI patch')
