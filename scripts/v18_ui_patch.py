from pathlib import Path

p = Path('app/src/main/java/dev/ryu4696/hitandblow/MainActivity.java')
s = p.read_text(encoding='utf-8')


def must_replace(old, new):
    global s
    if old not in s:
        raise SystemExit('UI patch target not found:\n' + old[:160])
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
   p.setShader(new LinearGradient(r.left,r.top,r.left,r.bottom,new int[]{0xff3b3a36,0xff1b1c1c,0xff2a2926},null,Shader.TileMode.CLAMP));
   p.setStyle(Paint.Style.FILL);p.setShadowLayer(h*.018f,0,h*.010f,0xcc000000);c.drawRoundRect(r,h*.022f,h*.022f,p);p.clearShadowLayer();p.setShader(null);
   p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(h*.004f);p.setColor(0xffc8b999);c.drawRoundRect(r,h*.022f,h*.022f,p);
   p.setStrokeWidth(h*.0018f);p.setColor(0xff625a4d);c.drawRoundRect(new RectF(r.left+h*.010f,r.top+h*.010f,r.right-h*.010f,r.bottom-h*.010f),h*.014f,h*.014f,p);
   p.setStyle(Paint.Style.FILL);
   for(float sx:new float[]{r.left+h*.024f,r.right-h*.024f}){
    p.setShader(new RadialGradient(sx,r.centerY(),h*.012f,new int[]{0xffffe1a0,0xff8d6735,0xff2b2118},null,Shader.TileMode.CLAMP));
    c.drawCircle(sx,r.centerY(),h*.010f,p);p.setShader(null);
   }
   text(c,label,r.centerX(),r.centerY()+h*.014f,h*.040f,0xffffead0);
  }

  void drawMenu(Canvas c,float w,float h){
   c.drawBitmap(bg,null,new RectF(0,0,w,h),p);
   p.setColor(0x26000000);p.setStyle(Paint.Style.FILL);c.drawRect(0,0,w,h,p);

   RectF center=new RectF(.265f*w,.115f*h,.735f*w,.86f*h);panel(c,center,h);

   p.setShader(null);p.setStyle(Paint.Style.FILL);p.clearShadowLayer();
   p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(h*.085f);p.setColor(0xffffe0a5);p.setShadowLayer(h*.009f,0,h*.006f,0xaa000000);
   c.drawText("HIT & BLOW",.5f*w,.275f*h,p);p.clearShadowLayer();
   text(c,"ヒット＆ブロー",.5f*w,.345f*h,h*.027f,0xffe6d7bd);
   peg(c,.421f*w,.334f*h,h*.010f,true);peg(c,.579f*w,.334f*h,h*.010f,false);

   RectF leftRack=new RectF(.125f*w,.205f*h,.205f*w,.795f*h);
   RectF rightRack=new RectF(.795f*w,.205f*h,.875f*w,.795f*h);
   panel(c,leftRack,h);panel(c,rightRack,h);
   int[] lc={0,2,1,3};int[] rc={4,5,2,1};
   for(int i=0;i<4;i++){
    float yy=(.29f+i*.135f)*h;
    marble(c,leftRack.centerX(),yy,h*.035f,lc[i]);
    marble(c,rightRack.centerX(),yy,h*.035f,rc[i]);
   }

   soloBtn.set(.34f*w,.455f*h,.66f*w,.585f*h);
   onlineBtn.set(.34f*w,.635f*h,.66f*w,.765f*h);
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

# Build this UI revision as v18 without requiring a separate source commit.
g = Path('app/build.gradle.kts')
gs = g.read_text(encoding='utf-8')
gs = gs.replace('versionCode = 17', 'versionCode = 18')
gs = gs.replace('versionName = "17.0"', 'versionName = "18.0"')
g.write_text(gs, encoding='utf-8')

print('Applied v18 home/menu UI patch')
