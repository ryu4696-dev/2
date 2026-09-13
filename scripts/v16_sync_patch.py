from pathlib import Path

p = Path('app/src/main/java/dev/ryu4696/hitandblow/MainActivity.java')
s = p.read_text(encoding='utf-8')


def replace_method(text, start_sig, next_sig, replacement):
    start = text.index(start_sig)
    end = text.index(next_sig, start)
    return text[:start] + replacement.rstrip() + '\n\n' + text[end:]

s = s.replace('onlineMsg="4桁のルームコード"', 'onlineMsg="4桁の部屋番号"')

create_room = r'''  void createRoom(){
   authenticate(()->{
    busy=true;onlineMsg="部屋を用意しています…";invalidate();
    net.execute(()->{
     try{
      String code="";
      for(int n=0;n<30;n++){
       String candidate=String.format(Locale.US,"%04d",new Random().nextInt(10000));
       if("null".equals(request("GET","/rooms/"+candidate,null))){code=candidate;break;}
      }
      if(code.isEmpty())throw new IOException("room code");
      String shared=makeSharedAnswer();
      JSONObject j=new JSONObject();
      j.put("hostId",uid)
       .put("status","waiting")
       .put("turnUid",uid)
       .put("moveIndex",0)
       .put("answer",shared)
       .put("createdAt",System.currentTimeMillis());
      request("PUT","/rooms/"+code,j.toString());
      int[]a=unCsv(shared);String finalCode=code;
      ui.post(()->{
       resetBoard();System.arraycopy(a,0,answer,0,4);
       roomCode=finalCode;host=true;role="host";online=true;busy=false;myTurn=false;roomStatus="waiting";screen=PLAY;onlineMsg="";
       startPolling();invalidate();
      });
     }catch(Exception e){ui.post(()->fail("部屋を作れませんでした"));}
    });
   });
  }'''

join_room = r'''  void joinRoom(){
   if(roomCode.length()!=4)return;
   final String wanted=roomCode;
   authenticate(()->{
    busy=true;onlineMsg="部屋を探しています…";invalidate();
    net.execute(()->{
     try{
      String raw=request("GET","/rooms/"+wanted,null);
      if(raw==null||raw.isEmpty()||"null".equals(raw)){ui.post(()->fail("その部屋は存在しません"));return;}
      JSONObject room=new JSONObject(raw);
      String hostId=room.optString("hostId","");
      if(hostId.isEmpty()){ui.post(()->fail("部屋データが壊れています"));return;}
      String guest=room.optString("guestId","");
      if(!guest.isEmpty()&&!guest.equals(uid)){ui.post(()->fail("その部屋は満員です"));return;}
      if("ended".equals(room.optString("status"))){ui.post(()->fail("その部屋の対戦は終了しています"));return;}

      JSONObject patch=new JSONObject();
      patch.put("guestId",uid).put("status","playing");
      if(room.optString("turnUid","").isEmpty())patch.put("turnUid",hostId);
      if(!room.has("moveIndex"))patch.put("moveIndex",0);
      request("PATCH","/rooms/"+wanted,patch.toString());

      String fresh=request("GET","/rooms/"+wanted,null);
      JSONObject joined=new JSONObject(fresh);
      int[]a=unCsv(joined.optString("answer","-1,-1,-1,-1"));
      if(a[0]<0){ui.post(()->fail("部屋の答えデータがありません"));return;}
      ui.post(()->{
       resetBoard();System.arraycopy(a,0,answer,0,4);
       roomCode=wanted;host=false;role="guest";online=true;busy=false;myTurn=false;roomStatus="playing";screen=PLAY;onlineMsg="";
       applyRoom(joined);startPolling();invalidate();
      });
     }catch(Exception e){
      String m=e.getMessage()==null?"":e.getMessage();
      ui.post(()->fail(m.contains("401")||m.contains("403")?"Firebaseの権限設定エラー":"部屋への接続に失敗しました"));
     }
    });
   });
  }'''

apply_room = r'''  void applyRoom(JSONObject room){
   try{
    roomStatus=room.optString("status","waiting");
    String a=room.optString("answer","");
    if(!a.isEmpty()){int[]v=unCsv(a);if(v[0]>=0)System.arraycopy(v,0,answer,0,4);}

    String hostId=room.optString("hostId","");
    String guestId=room.optString("guestId","");
    String turnUid=room.optString("turnUid",hostId);
    myTurn="playing".equals(roomStatus)&&!ended&&!guestId.isEmpty()&&uid.equals(turnUid);

    ArrayList<Integer>ids=new ArrayList<>();JSONObject moves=room.optJSONObject("moves");
    if(moves!=null){Iterator<String>it=moves.keys();while(it.hasNext())try{ids.add(Integer.parseInt(it.next()));}catch(Exception ignored){}Collections.sort(ids);}
    if(ids.size()!=remoteMoveCount){
     tries.clear();marks.clear();
     for(int id:ids){
      JSONObject m=moves.optJSONObject(String.valueOf(id));if(m==null)continue;
      tries.add(unCsv(m.optString("guess","-1,-1,-1,-1")));marks.add(new int[]{m.optInt("hit"),m.optInt("blow")});
      lastHit=m.optInt("hit");lastBlow=m.optInt("blow");
     }
     remoteMoveCount=tries.size();Arrays.fill(now,-1);editHistory.clear();selectedColor=-1;movingColor=movingSlot=-1;
     if(remoteMoveCount>0){markCol=remoteMoveCount-1;pinT=1;}
    }

    String winnerUid=room.optString("winnerUid","");
    String winner=room.optString("winner","");
    if(!winnerUid.isEmpty()||!winner.isEmpty()||"ended".equals(roomStatus)){
     ended=true;win=(!winnerUid.isEmpty()&&uid.equals(winnerUid))||(winnerUid.isEmpty()&&role.equals(winner));myTurn=false;lidT=1;
     if(win&&partyT==0){partyT=.01f;ValueAnimator f=ValueAnimator.ofFloat(.01f,1f);f.setDuration(1400);f.addUpdateListener(v->partyT=(float)v.getAnimatedValue());anim(f);}
    }
   }catch(Exception ignored){}
   invalidate();
  }'''

judge_online = r'''  void judgeOnline(){
   if(filled()<4||!myTurn||busy||tries.size()>=8)return;
   final int[]guess=now.clone();
   busy=true;myTurn=false;invalidate();

   net.execute(()->{
    try{
     String raw=request("GET","/rooms/"+roomCode,null);
     if(raw==null||raw.isEmpty()||"null".equals(raw))throw new IOException("room closed");
     JSONObject room=new JSONObject(raw);
     if(!"playing".equals(room.optString("status")))throw new IOException("not playing");

     String hostId=room.optString("hostId","");
     String guestId=room.optString("guestId","");
     String turnUid=room.optString("turnUid",hostId);
     if(!uid.equals(turnUid)){
      ui.post(()->{busy=false;myTurn=false;onlineMsg="相手のターンです";invalidate();});
      return;
     }

     int[]serverAnswer=unCsv(room.optString("answer","-1,-1,-1,-1"));
     if(serverAnswer[0]<0)throw new IOException("answer missing");
     System.arraycopy(serverAnswer,0,answer,0,4);
     int[]hb=judgeValues(guess);

     int index=room.optInt("moveIndex",-1);
     JSONObject moves=room.optJSONObject("moves");
     if(moves==null)moves=new JSONObject();
     if(index<0)index=moves.length();
     if(index>=8)throw new IOException("board full");

     JSONObject move=new JSONObject();
     move.put("playerUid",uid).put("player",role).put("guess",csv(guess)).put("hit",hb[0]).put("blow",hb[1]).put("at",System.currentTimeMillis());
     moves.put(String.valueOf(index),move);

     JSONObject patch=new JSONObject();
     patch.put("moves",moves).put("moveIndex",index+1);
     if(hb[0]==4){
      patch.put("winnerUid",uid).put("winner",role).put("status","ended");
     }else if(index>=7){
      patch.put("winner","draw").put("status","ended");
     }else{
      String nextUid=uid.equals(hostId)?guestId:hostId;
      if(nextUid.isEmpty())throw new IOException("opponent missing");
      patch.put("turnUid",nextUid);
     }

     request("PATCH","/rooms/"+roomCode,patch.toString());
     String fresh=request("GET","/rooms/"+roomCode,null);
     JSONObject synced=new JSONObject(fresh);
     ui.post(()->{
      busy=false;onlineMsg="";applyRoom(synced);
      resultT=0;ValueAnimator v=ValueAnimator.ofFloat(0,1);v.setDuration(1700);v.addUpdateListener(x->resultT=(float)x.getAnimatedValue());anim(v);
     });
    }catch(Exception e){
     ui.post(()->{busy=false;fail("手の同期に失敗しました");});
    }
   });
  }'''

s = replace_method(s, '  void createRoom(){', '  void joinRoom(){', create_room)
s = replace_method(s, '  void joinRoom(){', '  void startPolling(){', join_room)
s = replace_method(s, '  void applyRoom(JSONObject room){', '  int[] judgeValues(int[]guess){', apply_room)
s = replace_method(s, '  void judgeOnline(){', '  void leaveOnline(){', judge_online)

p.write_text(s, encoding='utf-8')
print('Applied v16 authoritative multiplayer sync patch')
