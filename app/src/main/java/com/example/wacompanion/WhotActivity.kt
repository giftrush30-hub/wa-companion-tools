package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

/** Local pass-and-play Nigerian WHOT practice using a disclosed Pagat-style 54-card deck. */
class WhotActivity : Activity() {
    private data class Card(val suit:String,val number:Int) {
        val wild get()=suit=="WHOT"
        val display get()=if(wild) "WHOT 20" else "$suit $number"
    }
    private val suits=listOf("BALL","ANGLE","CROSS","CARPET","STAR")
    private val numbers=mapOf(
        "BALL" to listOf(1,2,3,4,5,7,8,10,11,12,13,14),
        "ANGLE" to listOf(1,2,3,4,5,7,8,10,11,12,13,14),
        "CROSS" to listOf(1,2,3,5,7,10,11,13,14),
        "CARPET" to listOf(1,2,3,5,7,10,11,13,14),
        "STAR" to listOf(1,2,3,4,5,7,8)
    )
    private val ink=Color.rgb(30,39,48)
    private val green=Color.rgb(7,91,69)
    private val hands=mutableListOf<MutableList<Card>>()
    private val discard=mutableListOf<Card>()
    private val market=mutableListOf<Card>()
    private lateinit var players:List<String>
    private lateinit var root:LinearLayout
    private lateinit var status:TextView
    private lateinit var table:LinearLayout
    private var current=0
    private var top:Card?=null
    private var calledSuit:String?=null
    private var pendingDraw=0
    private var pendingType=0
    private var gameOver=false
    private var playerCount=2

    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);choosePlayers()}

    private fun choosePlayers(){
        AlertDialog.Builder(this).setTitle("WHOT! • Choose players")
            .setItems(arrayOf("2 players","3 players","4 players")){_,which->playerCount=which+2;newGame()}
            .setCancelable(false).show()
    }

    private fun newGame(){
        players=listOf("PLAYER 1","PLAYER 2","PLAYER 3","PLAYER 4").take(playerCount)
        hands.clear();repeat(playerCount){hands.add(mutableListOf())}
        discard.clear();market.clear()
        suits.forEach{s->numbers[s].orEmpty().forEach{n->market.add(Card(s,n))}}
        repeat(5){market.add(Card("WHOT",20))}
        market.shuffle()
        repeat(6){hands.forEach{it.add(market.removeAt(market.lastIndex))}}
        var first=market.removeAt(market.lastIndex)
        while(first.wild || first.number in listOf(1,2,5,8,14)){market.add(0,first);first=market.removeAt(market.lastIndex)}
        discard.add(first);top=first;calledSuit=null
        current=0;pendingDraw=0;pendingType=0;gameOver=false
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),dp(16),dp(14),dp(24));setBackgroundColor(Color.rgb(246,249,247))}
        setContentView(ScrollView(this).apply{addView(root)})
        root.addView(label("WHOT! • NAIJA CARD TABLE",24,green,true))
        root.addView(label("Pass-and-play practice • $playerCount players • No Games Points",13,Color.DKGRAY))
        status=label("",16,ink,true);root.addView(status)
        val topPanel=panel();topPanel.addView(label("CALL CARD",11,green,true))
        topPanel.addView(cardFace(first, true, null, compact=true))
        root.addView(topPanel)
        root.addView(label("YOUR HAND",13,green,true))
        table=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(table)
        root.addView(Button(this).apply{text="New match";isAllCaps=false;setOnClickListener{choosePlayers()}})
        root.addView(label("Rules in this practice table: match by shape or number. 1 = Hold On; 2 = Pick Two (stackable); 5 = Pick Three (stackable); 8 = Suspension; Star 8 skips two players; 14 = General Market; WHOT 20 = wild and calls a shape. Drawing ends your turn. First to clear their hand wins.",12,Color.DKGRAY))
        showTurn()
    }

    private fun showTurn(){
        if(gameOver)return
        status.text="${players[current]}'s turn • Cards left: "+hands.mapIndexed{i,h->"${players[i]} ${h.size}"}.joinToString("  |  ")+
            if(pendingDraw>0)"\nPICK PENALTY: $pendingDraw cards. Defend with another $pendingType or pick up." else ""
        table.removeAllViews()
        table.addView(label("Pass the phone to ${players[current]}. Only that player should view the hand.",13,Color.rgb(120,85,35),true))
        table.addView(Button(this).apply{text="I'm ready — show my cards";isAllCaps=false;setOnClickListener{showHand()}})
    }

    private fun showHand(){
        if(gameOver)return
        table.removeAllViews()
        table.addView(label("${players[current]}'S CARDS",14,green,true))
        hands[current].chunked(2).forEachIndexed{rowIndex,rowCards->
            val row=LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL
                gravity=Gravity.CENTER
            }
            rowCards.forEachIndexed{columnIndex,card->
                val index=rowIndex*2+columnIndex
                val legal=isLegal(card)
                val tile=cardFace(card,legal,{playCard(index)})
                val params=LinearLayout.LayoutParams(0,dp(116),1f)
                if(columnIndex==0)params.marginEnd=dp(6) else params.marginStart=dp(6)
                row.addView(tile,params)
            }
            if(rowCards.size==1)row.addView(android.view.View(this),LinearLayout.LayoutParams(0,dp(116),1f).apply{marginStart=dp(6)})
            val rowParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)}
            table.addView(row,rowParams)
        }
        table.addView(Button(this).apply{
            text=if(pendingDraw>0)"Pick up $pendingDraw cards" else "Go to market • draw 1"
            isAllCaps=false;setOnClickListener{
                if(!drawCards(if(pendingDraw>0)pendingDraw else 1))return@setOnClickListener
                pendingDraw=0;pendingType=0;nextPlayer()
            }
        })
        table.addView(Button(this).apply{text="Hide hand";isAllCaps=false;setOnClickListener{showTurn()}})
    }

    private fun isLegal(card:Card):Boolean{
        if(pendingDraw>0)return !card.wild && card.number==pendingType
        val face=top ?: return true
        return card.wild || card.number==face.number || card.suit==calledSuit || (!face.wild && card.suit==face.suit)
    }

    private fun playCard(index:Int){
        if(gameOver)return
        val card=hands[current].getOrNull(index)?:return
        if(!isLegal(card))return
        hands[current].removeAt(index);discard.add(card);top=card;calledSuit=null
        if(hands[current].isEmpty()){finishGame(current);return}
        when{
            card.wild->chooseSuit()
            card.number==2||card.number==5->{
                val amount=if(card.number==2)2 else 3
                if(pendingDraw==0)pendingType=card.number
                pendingDraw+=amount;nextPlayer("Defend with another $pendingType or pick up $pendingDraw.")
            }
            card.number==1->showTurn()
            card.number==8->{
                val skips=if(card.suit=="STAR")2 else 1
                repeat(skips){current=(current+1)%playerCount}
                showTurn("Suspension! $skips turn(s) skipped.")
            }
            card.number==14->{
                for(i in hands.indices)if(i!=current && !drawCards(1,i))return
                showTurn("General Market! Every other player picks one card.")
            }
            else->nextPlayer()
        }
    }

    private fun chooseSuit(){
        AlertDialog.Builder(this).setTitle("WHOT! — call the next shape")
            .setItems(suits.toTypedArray()){_,which->calledSuit=suits[which];nextPlayer("Shape called: ${suits[which]}.")}
            .setCancelable(false).show()
    }

    /** Draws the full requested amount, or ends the round if no card can be drawn. */
    private fun drawCards(count:Int,player:Int=current):Boolean{
        for (draw in 0 until count) {
            if(market.isEmpty()&&discard.size>1){
                val face=discard.removeAt(discard.lastIndex)
                market.addAll(discard);discard.clear();discard.add(face);market.shuffle()
            }
            if(market.isEmpty()) {
                finishByLowestHand()
                return false
            }
            hands[player].add(market.removeAt(market.lastIndex))
        }
        return true
    }

    /** If the draw pile and recyclable discard are exhausted, the lowest hand wins. */
    private fun finishByLowestHand(){
        if(gameOver)return
        val fewest=hands.minOfOrNull{it.size} ?: return
        val leaders=hands.indices.filter{hands[it].size==fewest}
        gameOver=true;table.removeAllViews();status.text="THE MARKET IS EMPTY • ROUND COMPLETE"
        val names=leaders.joinToString(" & "){players[it]}
        val recordLine=recordMatch("$names finished with the fewest cards",leaders.size)
        table.addView(label(if(leaders.size==1) "🏆 $names wins on the fewest cards!" else "🤝 Draw! $names finish level.",22,green,true))
        table.addView(label("No cards remained to draw. The player(s) holding the fewest cards take the round.",14,ink))
        showRecordPanel(recordLine)
        table.addView(Button(this).apply{text="Share the result";isAllCaps=false;setOnClickListener{
            ArenaShareCard.share(this@WhotActivity,"WHOT! MATCH RESULT","$names finished with the fewest cards in a $playerCount-player practice match.","YOUR TURN • GAMES ARENA")
        }})
        table.addView(Button(this).apply{text="Play again";isAllCaps=false;setOnClickListener{newGame()}})
    }

    /** Cream-and-ink card face inspired by physical Nigerian WHOT cards. */
    private fun cardFace(card:Card,playable:Boolean,onTap:(()->Unit)?,compact:Boolean=false):LinearLayout{
        val face=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            gravity=Gravity.CENTER
            setPadding(dp(8),dp(if(compact)6 else 8),dp(8),dp(if(compact)6 else 8))
            minimumHeight=dp(if(compact)90 else 110)
            background=GradientDrawable().apply{
                shape=GradientDrawable.RECTANGLE
                cornerRadius=dp(13).toFloat()
                setColor(if(playable)Color.rgb(255,250,232) else Color.rgb(235,235,229))
                setStroke(dp(if(playable)3 else 1),if(playable)green else Color.rgb(174,174,164))
            }
            alpha=if(playable)1f else 0.58f
            elevation=if(playable)dp(4).toFloat() else 0f
            isClickable=onTap!=null && playable
            isFocusable=isClickable
            contentDescription="${card.display}. ${if(playable)"Playable" else "Not playable"}."
            onTap?.let{action->setOnClickListener{if(playable)action()}}
        }
        val corner=TextView(this).apply{
            text=if(card.wild)"20" else card.number.toString()
            textSize=if(compact)15f else 16f
            setTextColor(Color.rgb(75,48,31))
            setTypeface(null,android.graphics.Typeface.BOLD)
            gravity=Gravity.START
        }
        face.addView(corner,LinearLayout.LayoutParams(-1,-2))
        val symbol=TextView(this).apply{
            text=suitGlyph(card.suit)
            textSize=if(compact)34f else 38f
            setTextColor(if(card.wild)Color.rgb(176,119,12) else Color.rgb(91,55,34))
            setTypeface(null,android.graphics.Typeface.BOLD)
            gravity=Gravity.CENTER
        }
        face.addView(symbol,LinearLayout.LayoutParams(-1,0,1f))
        val footer=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            gravity=Gravity.CENTER_VERTICAL
        }
        footer.addView(TextView(this).apply{
            text=if(card.wild)"WHOT" else card.suit
            textSize=if(compact)10f else 11f
            setTextColor(Color.rgb(75,48,31))
            setTypeface(null,android.graphics.Typeface.BOLD)
        },LinearLayout.LayoutParams(0,-2,1f))
        footer.addView(TextView(this).apply{
            text=if(onTap==null)card.number.toString() else if(playable)"PLAY" else "LOCKED"
            textSize=9f
            setTextColor(if(playable)green else Color.DKGRAY)
            setTypeface(null,android.graphics.Typeface.BOLD)
        })
        face.addView(footer,LinearLayout.LayoutParams(-1,-2))
        return face
    }

    private fun suitGlyph(suit:String):String=when(suit){
        "BALL"->"●"
        "ANGLE"->"▲"
        "CROSS"->"✚"
        "CARPET"->"▦"
        "STAR"->"★"
        else->"✦"
    }

    private fun cardTint(card:Card):Int = when(card.suit){
        "BALL" -> Color.rgb(30,105,190)
        "ANGLE" -> Color.rgb(15,130,94)
        "CROSS" -> Color.rgb(113,68,165)
        "CARPET" -> Color.rgb(190,83,42)
        "STAR" -> Color.rgb(190,139,20)
        else -> Color.rgb(22,47,68)
    }

    private fun nextPlayer(note:String=""){
        current=(current+1)%playerCount
        showTurn(note)
    }

    private fun showTurn(note:String){
        showTurn()
        if(note.isNotBlank())status.text=status.text.toString()+"\n"+note
    }

    private fun finishGame(winner:Int){
        if(gameOver)return
        gameOver=true;table.removeAllViews()
        status.text="MATCH COMPLETE"
        val recordLine=recordMatch("${players[winner]} won",1)
        table.addView(label("🏆 ${players[winner]} wins! CHECK UP!",23,green,true))
        table.addView(label("The first player to play their final card wins this practice round.",15,ink))
        showRecordPanel(recordLine)
        table.addView(Button(this).apply{text="Share the result";isAllCaps=false;setOnClickListener{
            ArenaShareCard.share(this@WhotActivity,"WHOT! MATCH RESULT","${players[winner]} won a $playerCount-player practice match. Who is ready for a rematch?","YOUR TURN • GAMES ARENA")
        }})
        table.addView(Button(this).apply{text="Play again";isAllCaps=false;setOnClickListener{newGame()}})
    }

    private fun recordMatch(result:String,winnerCount:Int):String {
        val prefs=getSharedPreferences("whot_local_records",MODE_PRIVATE)
        val played=prefs.getInt("matches_played",0)+1
        val wins=prefs.getInt("player_wins",0)+winnerCount
        val history=(prefs.getString("recent_results","") ?: "").split("|").filter{it.isNotBlank()}.toMutableList()
        history.add(0,result)
        val firstMatch=!prefs.getBoolean("first_match",false)
        val firstWin=winnerCount>0 && !prefs.getBoolean("first_win",false)
        val editor=prefs.edit().putInt("matches_played",played).putInt("player_wins",wins)
            .putBoolean("first_match",true).putString("recent_results",history.take(8).joinToString("|"))
        if(firstWin)editor.putBoolean("first_win",true)
        editor.apply()
        return "Matches completed: $played  •  Player wins recorded: $wins" +
            if(firstMatch) "  •  FIRST MATCH unlocked" else if(firstWin) "  •  FIRST WIN unlocked" else ""
    }

    private fun showRecordPanel(recordLine:String) {
        table.addView(label("YOUR LOCAL MATCH RECORD",13,green,true))
        table.addView(label(recordLine,13,ink,true))
        val history=(getSharedPreferences("whot_local_records",MODE_PRIVATE).getString("recent_results","") ?: "")
            .split("|").filter{it.isNotBlank()}
        history.take(5).forEach{table.addView(label("• $it",12,ink))}
    }

    private fun panel()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(10),dp(12),dp(10));setBackgroundColor(Color.WHITE)}
    private fun label(s:String,size:Int,color:Int,bold:Boolean=false)=TextView(this).apply{text=s;textSize=size.toFloat();setTextColor(color);if(bold)setTypeface(null,android.graphics.Typeface.BOLD);setPadding(2,dp(4),2,dp(6))}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
