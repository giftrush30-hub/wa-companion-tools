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
    // Conventional 15x15 Ludo cross: 52 clockwise shared squares around four 6x6 yards.
    private val track = buildList {
        for (c in 1..5) add(6 to c)
        for (r in 5 downTo 0) add(r to 6)
        add(0 to 7)
        for (r in 0..5) add(r to 8)
        for (c in 9..14) add(6 to c)
        add(7 to 14)
        for (c in 14 downTo 9) add(8 to c)
        for (r in 9..14) add(r to 8)
        add(14 to 7)
        for (r in 14 downTo 9) add(r to 6)
        for (c in 5 downTo 0) add(8 to c)
        add(7 to 0)
    }
    private val starts = listOf(0,13,26,39)
    private val safeTrack = setOf(0,8,13,21,26,34,39,47)
    private val lanes = listOf(
        (1..5).map { 7 to it },
        (1..5).map { it to 7 },
        (13 downTo 9).map { 7 to it },
        (13 downTo 9).map { it to 7 }
    )
    private val bases = listOf(
        listOf(2f to 2f,4f to 2f,2f to 4f,4f to 4f),
        listOf(11f to 2f,13f to 2f,11f to 4f,13f to 4f),
        listOf(11f to 11f,13f to 11f,11f to 13f,13f to 13f),
        listOf(2f to 11f,4f to 11f,2f to 13f,4f to 13f)
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
    private var readyToRoll = false

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
        rollButton=Button(this).apply {
            text="PASS PHONE • TAP WHEN READY"; isAllCaps=false
            setOnClickListener {
                if (!readyToRoll) {
                    readyToRoll=true; text="ROLL DICE"
                    status.text="${players[current].name} is ready. Roll the dice!"
                } else rollDice()
            }
        }; root.addView(rollButton)
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
            if(global !in safeTrack) tokens.indices.filter{it!=current}.forEach { other ->
                tokens[other].forEach { rival ->
                    if(rival.progress in 0..50 && (players[other].start+rival.progress)%52==global) {
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
        val note=when { captured -> "Capture! A rival token returned to base."; token.progress==56 -> "Token reached home!"; bonus -> "You rolled a six. Roll again!"; else -> "Move complete." }
        if(bonus) updateTurn(note) else advanceTurn(note)
    }

    private fun advanceTurn(note:String="") {
        current=(current+1)%players.size; consecutiveSixes=0; updateTurn(note)
    }
    private fun updateTurn(note:String="") {
        if(gameOver) return
        status.text="${players[current].name}'s turn • Home: ${tokens[current].count{it.progress==57}}/4\nPass the phone, then roll."+if(note.isNotBlank()) "\n$note" else ""
        readyToRoll=false
        rollButton.isEnabled=true; rollButton.text="PASS PHONE • TAP WHEN READY"; tokenButtons.removeAllViews(); board.invalidate()
    }

    private inner class LudoBoard:View(this@LudoActivity) {
        private val p=Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas:Canvas) {
            super.onDraw(canvas)
            val size=min(width.toFloat(),height.toFloat())-dp(10)
            val cell=size/15f; val left=(width-size)/2f; val top=(height-size)/2f
            fun rect(row:Int,col:Int,color:Int,stroke:Int=Color.rgb(206,216,210)) {
                val box=RectF(left+col*cell,top+row*cell,left+(col+1)*cell,top+(row+1)*cell)
                p.color=color;p.style=Paint.Style.FILL;canvas.drawRect(box,p)
                p.color=stroke;p.style=Paint.Style.STROKE;p.strokeWidth=dp(0.7f);canvas.drawRect(box,p);p.style=Paint.Style.FILL
            }
            fun rounded(r:Float,c:Float,w:Float,h:Float,color:Int,rad:Float) {
                p.color=color;p.style=Paint.Style.FILL
                canvas.drawRoundRect(RectF(left+c*cell,top+r*cell,left+(c+w)*cell,top+(r+h)*cell),rad,rad,p)
            }
            canvas.drawColor(Color.rgb(250,251,248))
            val yards=listOf(0 to 0,0 to 9,9 to 9,9 to 0)
            yards.forEachIndexed{i,pair->
                val(r,c)=pair;rounded(r.toFloat(),c.toFloat(),6f,6f,colors[i],cell*0.45f)
                rounded(r+1f,c+1f,4f,4f,Color.WHITE,cell*0.35f)
                val padCenters=listOf((r+2f) to (c+2f),(r+2f) to (c+4f),(r+4f) to (c+2f),(r+4f) to (c+4f))
                padCenters.forEach{(pr,pc)->
                    p.color=Color.rgb(231,237,233);canvas.drawCircle(left+(pc+0.5f)*cell,top+(pr+0.5f)*cell,cell*0.72f,p)
                }
            }
            track.forEachIndexed{i,pair->
                val(r,c)=pair;rect(r,c,if(i in starts) colors[starts.indexOf(i)] else Color.WHITE)
                if(i in safeTrack && i !in starts){
                    p.color=Color.rgb(204,171,67);p.textAlign=Paint.Align.CENTER;p.textSize=cell*0.62f
                    canvas.drawText("★",left+(c+0.5f)*cell,top+(r+0.72f)*cell,p)
                }
            }
            lanes.forEachIndexed{i,l->l.forEach{(r,c)->rect(r,c,colors[i])}}
            val centerLeft=left+6f*cell;val centerTop=top+6f*cell
            val cx=left+7.5f*cell;val cy=top+7.5f*cell
            val paths=listOf(
                floatArrayOf(centerLeft,centerTop,centerLeft,centerTop+3*cell,cx,cy),
                floatArrayOf(centerLeft,centerTop,centerLeft+3*cell,centerTop,cx,cy),
                floatArrayOf(centerLeft+3*cell,centerTop,centerLeft+3*cell,centerTop+3*cell,cx,cy),
                floatArrayOf(centerLeft,centerTop+3*cell,centerLeft+3*cell,centerTop+3*cell,cx,cy)
            )
            paths.forEachIndexed{i,pts->
                val path=android.graphics.Path().apply{moveTo(pts[0],pts[1]);lineTo(pts[2],pts[3]);lineTo(pts[4],pts[5]);close()}
                p.color=colors[i];canvas.drawPath(path,p)
            }
            p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;p.textSize=cell*0.34f
            canvas.drawText("FINISH",cx,cy+cell*0.12f,p)
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
