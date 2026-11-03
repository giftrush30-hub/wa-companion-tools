package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.min
import kotlin.random.Random

/** Offline pass-and-play Ludo practice. No online play, ranking or Games Points are claimed. */
class LudoActivity : Activity() {
    private data class Player(val name: String, val color: Int, val start: Int, val lane: List<Pair<Int,Int>>, val base: List<Pair<Float,Float>>)
    private data class Token(var progress: Int = -1)
    private val colors = listOf(Color.rgb(210,55,61), Color.rgb(40,133,86), Color.rgb(238,184,45), Color.rgb(54,111,194))
    private val track = buildList {
        for (x in 0..13) add(0 to x)
        for (y in 1..13) add(y to 13)
        for (x in 12 downTo 0) add(13 to x)
        for (y in 12 downTo 1) add(y to 0)
    }
    private val starts = listOf(7,20,33,46)
    private val lanes = listOf(
        (1..6).map { it to 7 },
        (12 downTo 7).map { 7 to it },
        (12 downTo 7).map { it to 6 },
        (1..6).map { 6 to it }
    )
    private val bases = listOf(
        listOf(2.5f to 2.5f,4f to 2.5f,2.5f to 4f,4f to 4f),
        listOf(9.5f to 2.5f,11f to 2.5f,9.5f to 4f,11f to 4f),
        listOf(9.5f to 9.5f,11f to 9.5f,9.5f to 11f,11f to 11f),
        listOf(2.5f to 9.5f,4f to 9.5f,2.5f to 11f,4f to 11f)
    )
    private lateinit var players: List<Player>
    private val tokens = mutableListOf<MutableList<Token>>()
    private lateinit var board: LudoBoard
    private lateinit var status: TextView
    private lateinit var tokenButtons: LinearLayout
    private lateinit var rollButton: Button
    private var current = 0
    private var roll: Int? = null
    private var consecutiveSixes = 0
    private var gameOver = false
    private var selectedPlayers = 4

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); choosePlayers() }

    private fun choosePlayers() {
        AlertDialog.Builder(this).setTitle("Set up your Ludo table")
            .setItems(arrayOf("2 players","3 players","4 players")) { _, which ->
                selectedPlayers=which+2; startGame()
            }.setCancelable(false).show()
    }

    private fun startGame() {
        val names=listOf("RED","GREEN","YELLOW","BLUE")
        players=(0 until selectedPlayers).map { i -> Player(names[i],colors[i],starts[i],lanes[i],bases[i]) }
        tokens.clear(); repeat(selectedPlayers) { tokens.add(MutableList(4){Token()}) }
        current=0; roll=null; consecutiveSixes=0; gameOver=false
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(14),dp(12),dp(18)); setBackgroundColor(Color.rgb(246,249,247)) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(label("LUDO • PASS & PLAY",25,Color.rgb(7,91,69),true))
        root.addView(label("One phone • $selectedPlayers players • Offline practice",13,Color.DKGRAY))
        board=LudoBoard(); root.addView(board,LinearLayout.LayoutParams(-1,dp(330)))
        status=label("",16,Color.rgb(18,42,55),true); root.addView(status)
        rollButton=Button(this).apply { text="ROLL DICE"; isAllCaps=false; setOnClickListener { rollDice() } }; root.addView(rollButton)
        tokenButtons=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; root.addView(tokenButtons)
        root.addView(label("Rules: roll a 6 to leave base; starting squares are safe; land on a single rival to send it home; exact roll required to finish. A 6 or capture earns another roll. Three consecutive sixes end your turn.",12,Color.DKGRAY))
        root.addView(Button(this).apply { text="New game"; isAllCaps=false; setOnClickListener { choosePlayers() } })
        updateTurn()
    }

    private fun rollDice() {
        if(gameOver || roll!=null) return
        val value=Random.nextInt(1,7); roll=value
        if(value==6) consecutiveSixes++ else consecutiveSixes=0
        if(consecutiveSixes>=3) {
            roll=null; consecutiveSixes=0; advanceTurn("Three sixes in a row — turn forfeited."); return
        }
        val legal=legalTokens(value)
        if(legal.isEmpty()) {
            board.invalidate()
            if(value==6) { roll=null; updateTurn("No legal piece can move. You keep the turn for rolling a six.") }
            else { roll=null; advanceTurn("No legal move for $value.") }
            return
        }
        status.text="${players[current].name} rolled $value. Choose a token."
        rollButton.isEnabled=false
        tokenButtons.removeAllViews()
        legal.forEach { index ->
            val p=tokens[current][index].progress
            tokenButtons.addView(Button(this).apply {
                text="Move token ${index+1}"+if(p==-1) " out of base" else " forward $value"
                isAllCaps=false; setOnClickListener { moveToken(index) }
            })
        }
    }

    private fun legalTokens(value:Int)=tokens[current].indices.filter { i ->
        val p=tokens[current][i].progress
        (p==-1 && value==6) || (p>=0 && p<57 && p+value<=57 && !blockedByPair(p,p+value))
    }

    private fun blockedByPair(from:Int,next:Int):Boolean {
        val lastTrackStep=minOf(next,51)
        if(from+1>lastTrackStep) return false
        return (from+1..lastTrackStep).any { step ->
            val target=(players[current].start+step)%52
            tokens.indices.filter{it!=current}.any { other ->
                tokens[other].count { it.progress in 0..51 && (players[other].start+it.progress)%52==target }>=2
            }
        }
    }

    private fun moveToken(index:Int) {
        val value=roll ?: return
        val token=tokens[current][index]
        var bonus=value==6
        if(token.progress==-1) token.progress=0 else token.progress+=value
        var captured=false
        if(token.progress in 0..51) {
            val global=(players[current].start+token.progress)%52
            if(!starts.contains(global)) tokens.indices.filter{it!=current}.forEach { other ->
                tokens[other].forEach { rival ->
                    if(rival.progress in 0..51 && (players[other].start+rival.progress)%52==global) {
                        rival.progress=-1; captured=true
                    }
                }
            }
        }
        if(captured) bonus=true
        if(token.progress==57) bonus=true
        roll=null; tokenButtons.removeAllViews(); board.invalidate()
        if(tokens[current].all{it.progress==57}) {
            gameOver=true; status.text="🏆 ${players[current].name} wins the Ludo practice match!"
            rollButton.text="MATCH COMPLETE"; rollButton.isEnabled=false; return
        }
        val note=when { captured -> "Capture! A rival token returned to base."; token.progress==57 -> "Token reached home!"; bonus -> "You rolled a six. Roll again!"; else -> "Move complete." }
        if(bonus) updateTurn(note) else advanceTurn(note)
    }

    private fun advanceTurn(note:String="") {
        current=(current+1)%players.size; consecutiveSixes=0; updateTurn(note)
    }
    private fun updateTurn(note:String="") {
        if(gameOver) return
        status.text="${players[current].name}'s turn • Home: ${tokens[current].count{it.progress==57}}/4\nPass the phone, then roll."+if(note.isNotBlank()) "\n$note" else ""
        rollButton.isEnabled=true; rollButton.text="ROLL DICE"; tokenButtons.removeAllViews(); board.invalidate()
    }

    private inner class LudoBoard:View(this@LudoActivity) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas:Canvas) {
            super.onDraw(canvas)
            val size=min(width.toFloat(),height.toFloat())-dp(10)
            val cell=size/15f; val left=(width-size)/2f; val top=(height-size)/2f
            fun square(row:Int,col:Int,color:Int) {
                p.color=color; canvas.drawRect(left+col*cell,top+row*cell,left+(col+1)*cell,top+(row+1)*cell,p)
                p.color=Color.rgb(205,215,210); p.style=Paint.Style.STROKE; p.strokeWidth=dp(0.7f)
                canvas.drawRect(left+col*cell,top+row*cell,left+(col+1)*cell,top+(row+1)*cell,p); p.style=Paint.Style.FILL
            }
            track.forEach{(r,c)->square(r,c,Color.WHITE)}
            val basesAt=listOf(1 to 1,1 to 9,9 to 9,9 to 1)
            basesAt.forEachIndexed{i,pair->
                val(r,c)=pair; p.color=colors[i]
                canvas.drawRoundRect(RectF(left+c*cell,top+r*cell,left+(c+5)*cell,top+(r+5)*cell),cell,cell,p)
                p.color=Color.WHITE; canvas.drawRoundRect(RectF(left+(c+1)*cell,top+(r+1)*cell,left+(c+4)*cell,top+(r+4)*cell),cell/2,cell/2,p)
            }
            starts.forEachIndexed{i,s->val(r,c)=track[s];square(r,c,colors[i])}
            lanes.forEachIndexed{i,l->l.forEach{(r,c)->square(r,c,colors[i])}}
            p.color=Color.rgb(7,91,69);canvas.drawCircle(left+7.5f*cell,top+7.5f*cell,cell*1.2f,p)
            p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=cell*0.5f;canvas.drawText("HOME",left+7.5f*cell,top+7.7f*cell,p)
            tokens.forEachIndexed{pi,list->list.forEachIndexed{ti,t->
                val(x,y)=when {
                    t.progress==-1->players[pi].base[ti]
                    t.progress==57->7.5f to 7.5f
                    t.progress in 52..56->{val q=players[pi].lane[t.progress-52];q.second+0.5f to q.first+0.5f}
                    else->{val q=track[(players[pi].start+t.progress)%52];q.second+0.5f to q.first+0.5f}
                }
                val cx=left+x*cell;val cy=top+y*cell
                p.color=Color.BLACK;canvas.drawCircle(cx,cy,cell*0.34f,p)
                p.color=players[pi].color;canvas.drawCircle(cx,cy,cell*0.29f,p)
                p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=cell*0.3f;canvas.drawText("${ti+1}",cx,cy+cell*0.1f,p)
            }}
        }
    }
    private fun label(s:String,size:Int,color:Int,bold:Boolean=false)=TextView(this).apply{
        text=s;textSize=size.toFloat();setTextColor(color);if(bold)setTypeface(null,android.graphics.Typeface.BOLD);setPadding(2,dp(4),2,dp(7))
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun dp(v:Float)=(v*resources.displayMetrics.density)
}
