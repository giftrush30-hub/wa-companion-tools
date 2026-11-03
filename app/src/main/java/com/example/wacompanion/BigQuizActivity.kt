package com.example.wacompanion

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Offline Big Quiz practice. No competitive score or Games Points are awarded in this local first milestone. */
class BigQuizActivity : Activity() {
    private data class Question(val text: String, val options: List<String>, val answer: Int, val explanation: String)
    private val questions = listOf(
        Question("Which Nigerian city became the Federal Capital Territory in 1991?", listOf("Lagos","Abuja","Kano","Ibadan"),1,"Abuja replaced Lagos as Nigeria's capital in 1991."),
        Question("Which planet is known as the Red Planet?", listOf("Venus","Mars","Jupiter","Mercury"),1,"Iron-rich dust gives Mars its familiar reddish appearance."),
        Question("Who wrote Things Fall Apart?", listOf("Wole Soyinka","Chinua Achebe","Buchi Emecheta","J. P. Clark"),1,"Chinua Achebe's novel was first published in 1958."),
        Question("What is the chemical symbol for gold?", listOf("Ag","Gd","Au","Go"),2,"Au comes from the Latin word aurum."),
        Question("How many sides does a dodecagon have?", listOf("10","11","12","14"),2,"A dodecagon has twelve sides."),
        Question("Which organelle is often called the powerhouse of the cell?", listOf("Nucleus","Mitochondrion","Ribosome","Golgi apparatus"),1,"Mitochondria generate much of the cell's usable energy."),
        Question("What is the value of 15% of 800?", listOf("80","100","120","140"),2,"10% is 80 and 5% is 40, giving 120."),
        Question("Which Nigerian river gives its name to the Niger Delta region?", listOf("River Benue","River Niger","River Osun","River Kaduna"),1,"The Niger River and its distributaries form the delta system."),
        Question("In computing, what does URL stand for?", listOf("Universal Record Link","Uniform Resource Locator","Unified Routing Language","User Reference List"),1,"A URL is the address used to locate a resource on the web."),
        Question("Which scientist formulated the three laws of motion?", listOf("Albert Einstein","Isaac Newton","Galileo Galilei","Nikola Tesla"),1,"Newton's laws describe the relationship between forces and motion."),
        Question("A product costs ₦8,000 and sells for ₦10,000 before other costs. What is the gross profit?", listOf("₦1,000","₦2,000","₦8,000","₦18,000"),1,"Gross profit before other costs is selling price minus purchase cost."),
        Question("Which layer of Earth's atmosphere contains most of the ozone layer?", listOf("Troposphere","Stratosphere","Mesosphere","Thermosphere"),1,"Most atmospheric ozone is concentrated in the stratosphere."),
        Question("What is the next prime number after 29?", listOf("30","31","33","35"),1,"31 is divisible only by 1 and itself."),
        Question("Which African country was formerly known as Abyssinia?", listOf("Ethiopia","Eritrea","Sudan","Somalia"),0,"Abyssinia is the historical name commonly associated with Ethiopia."),
        Question("If a price falls by 20% and then rises by 20%, where does it end relative to the start?", listOf("Same price","4% lower","4% higher","8% lower"),1,"100 falls to 80; a 20% rise on 80 gives 96, which is 4% below 100."),
        Question("Which is the largest ocean on Earth?", listOf("Atlantic","Indian","Pacific","Arctic"),2,"The Pacific Ocean is the largest and deepest ocean basin."),
        Question("In which year did Nigeria gain independence?", listOf("1957","1960","1963","1970"),1,"Nigeria became independent on 1 October 1960."),
        Question("What number is represented by binary 1010?", listOf("8","9","10","12"),2,"Binary 1010 equals 8 + 2, which is 10."),
        Question("What is three-quarters of 64?", listOf("16","32","48","56"),2,"One quarter of 64 is 16; three quarters is 48."),
        Question("Which process allows green plants to make food using light?", listOf("Respiration","Photosynthesis","Fermentation","Evaporation"),1,"Photosynthesis uses light energy to convert water and carbon dioxide into sugars."),
        Question("Where is the headquarters of the World Health Organization?", listOf("Paris","Geneva","New York","Nairobi"),1,"The WHO headquarters is in Geneva, Switzerland."),
        Question("Which continent contains the Sahara Desert?", listOf("Asia","South America","Africa","Australia"),2,"The Sahara stretches across much of North Africa."),
        Question("What does CPU stand for in computing?", listOf("Central Processing Unit","Computer Power Utility","Core Program Upload","Central Print Unit"),0,"The CPU executes instructions and coordinates core processing tasks."),
        Question("What is the square root of 144?", listOf("10","11","12","14"),2,"12 multiplied by 12 equals 144."),
        Question("A journey of 150 kilometres takes 3 hours. What is the average speed?", listOf("30 km/h","45 km/h","50 km/h","75 km/h"),2,"Average speed is distance divided by time: 150 ÷ 3 = 50 km/h."),
        Question("Which gas do plants absorb from the atmosphere for photosynthesis?", listOf("Oxygen","Carbon dioxide","Nitrogen","Hydrogen"),1,"Plants use carbon dioxide as a carbon source during photosynthesis."),
        Question("What is 0.25 written as a fraction in simplest form?", listOf("1/2","1/3","1/4","1/5"),2,"0.25 is 25/100, which simplifies to 1/4."),
        Question("Which branch of government is primarily responsible for interpreting laws?", listOf("Executive","Legislature","Judiciary","Civil service"),2,"Courts and the judiciary interpret and apply the law."),
        Question("If all bloops are razzies and all razzies are lazzies, which statement must be true?", listOf("All lazzies are bloops","All bloops are lazzies","No bloops are lazzies","Some razzies are not lazzies"),1,"If bloops are inside the razzy group, and razzies are inside the lazzy group, all bloops are lazzies."),
        Question("What is the chemical formula for water?", listOf("CO2","H2O","O2","NaCl"),1,"A water molecule contains two hydrogen atoms and one oxygen atom.")
    )
    private val ink = Color.rgb(18,42,55)
    private val green = Color.rgb(7,91,69)
    private val muted = Color.rgb(91,108,119)
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var questionArea: LinearLayout
    private var index = 0
    private var correct = 0
    private var locked = false
    private var fiftyUsed = false
    private var hiddenOptions = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18),dp(20),dp(18),dp(28))
            setBackgroundColor(Color.rgb(246,249,247))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(label("BIG QUIZ",28,green,true))
        root.addView(label("The practice challenge • 30 questions",14,muted))
        status = label("",14,muted)
        root.addView(status)
        questionArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(questionArea)
        renderQuestion()
    }

    private fun renderQuestion() {
        if (index >= questions.size) { showSummary(); return }
        locked = false
        hiddenOptions.clear()
        val q = questions[index]
        status.text = "LEVEL ${index+1} / ${questions.size}     •     CORRECT: $correct"
        questionArea.removeAllViews()
        val milestone = when (index + 1) {
            5 -> "CHECKPOINT • STARTER"
            10 -> "CHECKPOINT • SKILLED"
            15 -> "CHECKPOINT • SHARP"
            20 -> "CHECKPOINT • MASTER"
            25 -> "CHECKPOINT • EXPERT"
            30 -> "FINAL LEVEL • QUIZ CHAMPION"
            else -> "THE PRACTICE LADDER"
        }
        questionArea.addView(label(milestone,12,green,true))
        questionArea.addView(label("LEVEL ${index+1}",12,muted,true))
        questionArea.addView(label(q.text,21,ink,true))
        val answers = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        q.options.forEachIndexed { optionIndex, option ->
            val button = Button(this).apply {
                text = "${'A'+optionIndex}.  $option"
                isAllCaps = false
                setTextColor(ink)
                setBackgroundColor(Color.WHITE)
                setOnClickListener { choose(optionIndex, answers) }
            }
            answers.addView(button)
        }
        questionArea.addView(answers)
        if (!fiftyUsed) {
            questionArea.addView(Button(this).apply {
                text = "50:50 lifeline  •  use once"
                isAllCaps = false
                setOnClickListener {
                    fiftyUsed = true
                    val wrong = q.options.indices.filter { it != q.answer }.shuffled().take(2).toSet()
                    hiddenOptions.addAll(wrong)
                    for (i in 0 until answers.childCount) answers.getChildAt(i).visibility =
                        if (i in hiddenOptions) View.INVISIBLE else View.VISIBLE
                    text = "50:50 used"
                    isEnabled = false
                }
            })
        }
        questionArea.addView(label("Practice only: this round is offline. No Games Points or competitive ranking are awarded.",12,muted))
    }

    private fun choose(selected: Int, answers: LinearLayout) {
        if (locked) return
        locked = true
        val q = questions[index]
        val isCorrect = selected == q.answer
        if (isCorrect) correct++
        for (i in 0 until answers.childCount) answers.getChildAt(i).isEnabled = false
        val feedback = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12),dp(12),dp(12),dp(12))
            setBackgroundColor(Color.WHITE)
        }
        feedback.addView(label(if (isCorrect) "CORRECT! WELL PLAYED." else "NOT QUITE — KEEP GOING.",17,if(isCorrect) green else Color.rgb(160,65,45),true))
        feedback.addView(label("Answer: ${q.options[q.answer]}",15,ink,true))
        feedback.addView(label(q.explanation,14,muted))
        questionArea.addView(feedback)
        questionArea.addView(Button(this).apply {
            text = if(index == questions.lastIndex) "See final result" else "Next level"
            isAllCaps = false
            setOnClickListener { index++; renderQuestion() }
        })
    }

    private fun showSummary() {
        questionArea.removeAllViews()
        status.text = "CHALLENGE COMPLETE"
        questionArea.addView(label("You got $correct out of ${questions.size} correct.",23,green,true))
        val message = when {
            correct >= 27 -> "Quiz Champion! Outstanding performance."
            correct >= 23 -> "Expert level. Your brain came prepared."
            correct >= 18 -> "Masterful effort. You know your stuff."
            correct >= 13 -> "Sharp performance. Keep building."
            correct >= 8 -> "Good effort. A little more practice will sharpen you."
            else -> "Every champion starts somewhere. Run it back!"
        }
        questionArea.addView(label(message,16,ink))
        questionArea.addView(Button(this).apply {
            text = "Share my Big Quiz result"
            isAllCaps = false
            setOnClickListener {
                ArenaShareCard.share(this@BigQuizActivity,"BIG QUIZ PRACTICE RESULT",
                    "I scored $correct out of ${questions.size} in WA Companion Big Quiz practice. Can you beat my score?",
                    "YOUR TURN  •  GAMES ARENA")
            }
        })
        questionArea.addView(Button(this).apply {
            text = "Play again"
            isAllCaps = false
            setOnClickListener { index=0; correct=0; fiftyUsed=false; renderQuestion() }
        })
        questionArea.addView(Button(this).apply {
            text = "Back to Games Arena"
            isAllCaps = false
            setOnClickListener { finish() }
        })
    }

    private fun label(value:String,size:Int,color:Int,bold:Boolean=false)=TextView(this).apply {
        text=value; textSize=size.toFloat(); setTextColor(color)
        if(bold) setTypeface(null,android.graphics.Typeface.BOLD)
        setPadding(0,dp(4),0,dp(7))
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
