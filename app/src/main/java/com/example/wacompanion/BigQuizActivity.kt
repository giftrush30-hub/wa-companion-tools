package com.example.wacompanion

import android.app.Activity
import android.content.Intent
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

/** Offline Big Quiz practice. No competitive score or Games Points are awarded in this local first milestone. */
class BigQuizActivity : Activity() {
    private data class Question(val text: String, val options: List<String>, val answer: Int, val explanation: String)
    private val questionBank = listOf(
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
        Question("What is the chemical formula for water?", listOf("CO2","H2O","O2","NaCl"),1,"A water molecule contains two hydrogen atoms and one oxygen atom."),
        Question("What is the currency of Nigeria?", listOf("Cedi","Naira","Dalasi","Shilling"),1,"Nigeria's currency is the naira, divided into 100 kobo."),
        Question("Where is the headquarters of the African Union?", listOf("Lagos","Addis Ababa","Accra","Cairo"),1,"The African Union Commission is headquartered in Addis Ababa, Ethiopia."),
        Question("How many states make up the Federal Republic of Nigeria?", listOf("30","32","36","40"),2,"Nigeria has 36 states, alongside the Federal Capital Territory."),
        Question("In which year did Ghana gain independence?", listOf("1945","1957","1960","1963"),1,"Ghana became independent from British rule on 6 March 1957."),
        Question("Which African country is the largest by land area?", listOf("Sudan","Democratic Republic of the Congo","Algeria","Libya"),2,"Algeria is Africa's largest country by area."),
        Question("What is Nigeria's official language?", listOf("Hausa","Yoruba","Igbo","English"),3,"English is Nigeria's official language; many Nigerian languages are also widely spoken."),
        Question("At standard atmospheric pressure, what is the boiling point of pure water?", listOf("50°C","80°C","100°C","120°C"),2,"Pure water boils at 100°C at standard atmospheric pressure."),
        Question("How many chambers does the human heart have?", listOf("Two","Three","Four","Five"),2,"The human heart has two atria and two ventricles."),
        Question("What does DNA primarily carry?", listOf("Genetic information","Oxygen","Digestive enzymes","Electrical power"),0,"DNA stores hereditary instructions used in the development and functioning of living organisms."),
        Question("What is the SI unit of force?", listOf("Joule","Watt","Newton","Pascal"),2,"Force is measured in newtons, named after physicist Isaac Newton."),
        Question("Which star is closest to Earth?", listOf("Sirius","The Sun","Polaris","Proxima Centauri"),1,"The Sun is Earth's nearest star."),
        Question("At what temperature does pure water freeze on the Celsius scale?", listOf("-10°C","0°C","10°C","32°C"),1,"Pure water freezes at 0°C under standard pressure."),
        Question("What is 7 multiplied by 8?", listOf("48","54","56","64"),2,"Seven groups of eight make 56."),
        Question("What is 2 raised to the power of 5?", listOf("10","16","25","32"),3,"2 × 2 × 2 × 2 × 2 equals 32."),
        Question("What is 25% of 240?", listOf("40","50","60","80"),2,"25% is one quarter; 240 divided by 4 is 60."),
        Question("A square has sides of 6 cm. What is its perimeter?", listOf("12 cm","18 cm","24 cm","36 cm"),2,"A square has four equal sides, so 4 × 6 cm = 24 cm."),
        Question("What is three-fifths of 100?", listOf("20","40","60","80"),2,"100 divided by 5 is 20; three parts make 60."),
        Question("What does HTML stand for?", listOf("HyperText Markup Language","High Transfer Machine Link","Home Tool Markup List","Hyperlink Text Management Logic"),0,"HTML is the standard markup language used to structure content on web pages."),
        Question("What does RAM stand for?", listOf("Read Access Memory","Random Access Memory","Rapid Application Mode","Remote Allocation Module"),1,"RAM is temporary working memory used by a computer while tasks are running."),
        Question("What is phishing?", listOf("A way to cool a computer","A fraudulent attempt to steal information","A type of screen","A safe backup method"),1,"Phishing uses deceptive messages or websites to trick people into revealing sensitive information."),
        Question("Bluetooth is mainly used for what?", listOf("Short-range wireless communication","Long-distance satellite television","Printing newspapers","Charging every phone wirelessly"),0,"Bluetooth connects nearby devices wirelessly over short distances."),
        Question("In a spreadsheet, what is the intersection of a row and a column called?", listOf("A slide","A cell","A folder","A browser"),1,"A spreadsheet cell is identified by its column letter and row number."),
        Question("What does HTTPS indicate about a web connection?", listOf("The page is always truthful","The connection is encrypted using TLS","The site cannot contain adverts","The site is stored offline"),1,"HTTPS uses TLS to protect data exchanged between a browser and a website."),
        Question("What is the capital city of Japan?", listOf("Kyoto","Osaka","Tokyo","Nagoya"),2,"Tokyo is Japan's capital and the seat of its national government."),
        Question("Where is the United Nations headquarters?", listOf("Geneva","New York City","London","Brussels"),1,"The UN headquarters is in New York City."),
        Question("Which mountain is the highest above sea level?", listOf("Kilimanjaro","Mount Everest","K2","Mount Kenya"),1,"Mount Everest, in the Himalayas, has the highest summit above sea level."),
        Question("Brazil is located on which continent?", listOf("Africa","Europe","South America","Asia"),2,"Brazil is the largest country in South America."),
        Question("What is the currency of Japan?", listOf("Won","Yuan","Yen","Baht"),2,"Japan's currency is the yen."),
        Question("In a democracy, political authority is generally based on what?", listOf("Hereditary rule alone","The consent of the people","Military ownership","One family's private decision"),1,"Democratic systems derive governing authority from the people, commonly through elections and representation."),
        Question("In business, what is revenue?", listOf("Money earned from sales and other business activities","Only the money left after all costs","A loan repayment","The value of unpaid holidays"),0,"Revenue is the income a business earns from its normal activities before expenses are deducted."),
        Question("What is an expense?", listOf("A business cost","A customer's review","The owner's job title","A product's colour"),0,"Expenses are the costs a business incurs while operating."),
        Question("How is net profit generally calculated?", listOf("Revenue plus expenses","Revenue minus all expenses","Expenses minus revenue","Sales multiplied by staff numbers"),1,"Net profit is what remains after all relevant expenses are deducted from revenue."),
        Question("What is a budget?", listOf("A plan for expected income and spending","A list of customer birthdays","A business logo","A tax receipt only"),0,"A budget helps plan and track expected income and expenditure over a period."),
        Question("Simple interest is calculated using which basic relationship?", listOf("Principal × rate × time","Principal + rate + time","Sales × expenses","Profit ÷ customers"),0,"Simple interest is principal multiplied by the interest rate and time, using consistent units."),
        Question("If supply increases while demand stays the same, what generally happens to the market price?", listOf("It tends to rise","It tends to fall","It must double","It never changes"),1,"All else equal, an increase in supply tends to put downward pressure on price."),
        Question("What does break-even mean for a business?", listOf("Revenue equals total costs","Revenue is always zero","Expenses are twice sales","The business has no customers"),0,"At the break-even point, total revenue equals total costs, so there is no profit or loss.")
    )
    private var questions: List<Question> = questionBank
    private val categories = linkedMapOf(
        "All categories" to (0..65).toList(),
        "Nigeria & Africa" to listOf(0,2,7,13,16,30,31,32,33,34,35),
        "Science & Nature" to listOf(1,5,9,11,19,25,29,36,37,38,39,40,41),
        "Maths & Logic" to listOf(4,6,12,14,18,23,24,26,28,42,43,44,45,46,47),
        "Digital & Technology" to listOf(8,17,22,48,49,50,51,52,53),
        "World & Society" to listOf(15,20,21,27,54,55,56,57,58,59),
        "Business & Money" to listOf(10,60,61,62,63,64,65)
    )
    private var selectedCategory = "All categories"
    private val ink = Color.rgb(18,42,55)
    private val green = Color.rgb(7,91,69)
    private val muted = Color.rgb(91,108,119)
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var progressBar: ProgressBar
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
        root.addView(label(if(intent.hasExtra("game_room_id")) "BIG QUIZ • ROOM CHAT CONNECTED" else "BIG QUIZ",28,green,true))
        if(intent.hasExtra("game_room_id"))root.addView(Button(this).apply{
            text="💬 Chat & voice with players";isAllCaps=false;setOnClickListener{openRoomChat()}
        })
        root.addView(label("THE QUIZ SHOW • OFFLINE PRACTICE",14,muted,true))
        status = label("",14,muted,true)
        root.addView(status)
        progressBar = ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply {
            max = questions.size
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(green)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(220,229,224))
        }
        root.addView(progressBar,LinearLayout.LayoutParams(-1,dp(8)).apply { bottomMargin=dp(14) })
        questionArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(questionArea)
        chooseCategory()
    }

    private fun chooseCategory() {
        val names = categories.keys.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("BIG QUIZ • CHOOSE YOUR ARENA")
            .setMessage("Pick a subject or take the full 66-question championship challenge.")
            .setItems(names) { _, which ->
                selectedCategory = names[which]
                prepareRound()
                index = 0
                correct = 0
                fiftyUsed = false
                renderQuestion()
            }
            .setCancelable(false)
            .show()
    }

    /** Shuffle both question order and answer order while preserving each correct answer. */
    private fun prepareRound() {
        questions = categories[selectedCategory].orEmpty()
            .map { questionBank[it] }
            .shuffled()
            .map { question ->
                val order = question.options.indices.shuffled()
                Question(
                    question.text,
                    order.map { question.options[it] },
                    order.indexOf(question.answer),
                    question.explanation
                )
            }
    }

    private fun renderQuestion() {
        if (index >= questions.size) { showSummary(); return }
        locked = false
        hiddenOptions.clear()
        val q = questions[index]
        status.text = "${selectedCategory.uppercase()}  •  LEVEL ${index+1}/${questions.size}  •  SCORE: $correct"
        progressBar.progress = index
        questionArea.removeAllViews()
        val milestone = when {
            index + 1 == questions.size -> "FINAL LEVEL • QUIZ CHAMPION"
            index + 1 == 5 -> "CHECKPOINT • STARTER"
            index + 1 == 10 -> "CHECKPOINT • SKILLED"
            index + 1 == 15 -> "CHECKPOINT • SHARP"
            index + 1 == 20 -> "CHECKPOINT • MASTER"
            index + 1 == 25 -> "CHECKPOINT • EXPERT"
            else -> "THE PRACTICE LADDER"
        }
        questionArea.addView(label(milestone,12,green,true))
        questionArea.addView(label("CATEGORY: $selectedCategory",11,muted,true))
        questionArea.addView(label("LEVEL ${index+1}",12,muted,true))
        questionArea.addView(label("QUESTION ${index+1}",12,muted,true))
        questionArea.addView(label(q.text,23,ink,true))
        val answers = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        q.options.forEachIndexed { optionIndex, option ->
            val button = Button(this).apply {
                text = "${'A'+optionIndex}.  $option"
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    listOf(Color.rgb(28,105,164),Color.rgb(7,117,86),Color.rgb(112,75,155),Color.rgb(174,93,38))[optionIndex]))
                setPadding(dp(12),dp(12),dp(12),dp(12))
                elevation=dp(2).toFloat()
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
        questionArea.addView(label("Think it through. Lock in your answer when ready.",12,muted))
        questionArea.addView(label("Offline practice • No Games Points or competitive ranking.",11,muted))
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
        status.text = "CHALLENGE COMPLETE • $correct/${questions.size}"
        progressBar.progress = questions.size
        questionArea.addView(label("You got $correct out of ${questions.size} correct.",23,green,true))
        val percentage = correct * 100 / questions.size
        val progress = getSharedPreferences("big_quiz_progress", MODE_PRIVATE)
        val categoryKey = selectedCategory.lowercase().replace(Regex("[^a-z0-9]+"), "_")
        val bestKey = "best_$categoryKey"
        val previousBest = progress.getInt(bestKey, -1)
        val newBest = previousBest < correct
        val completed = progress.getInt("completed_rounds", 0) + 1
        val bestScore = maxOf(previousBest, correct)
        val editor = progress.edit().putInt(bestKey, bestScore).putInt("completed_rounds", completed)
        val unlocked = mutableListOf<String>()
        if (!progress.getBoolean("achievement_first_finish", false)) {
            editor.putBoolean("achievement_first_finish", true)
            unlocked.add("FIRST FINISH • Completed your first Big Quiz round")
        }
        if (percentage >= 75 && !progress.getBoolean("achievement_expert", false)) {
            editor.putBoolean("achievement_expert", true)
            unlocked.add("EXPERT MIND • Scored at least 75%")
        }
        if (percentage >= 90 && !progress.getBoolean("achievement_champion", false)) {
            editor.putBoolean("achievement_champion", true)
            unlocked.add("QUIZ CHAMPION • Scored at least 90%")
        }
        if (percentage == 100 && !progress.getBoolean("achievement_perfect", false)) {
            editor.putBoolean("achievement_perfect", true)
            unlocked.add("PERFECT ROUND • Answered every question correctly")
        }
        editor.apply()
        val achievementCount = listOf("achievement_first_finish","achievement_expert","achievement_champion","achievement_perfect")
            .count { progress.getBoolean(it, false) }
        val message = when {
            percentage >= 90 -> "Quiz Champion! Outstanding performance."
            percentage >= 75 -> "Expert level. Your brain came prepared."
            percentage >= 60 -> "Masterful effort. You know your stuff."
            percentage >= 45 -> "Sharp performance. Keep building."
            percentage >= 30 -> "Good effort. A little more practice will sharpen you."
            else -> "Every champion starts somewhere. Run it back!"
        }
        questionArea.addView(label("ACCURACY: $percentage%",12,muted,true))
        questionArea.addView(label(message,16,ink))
        questionArea.addView(label(if(newBest) "🏅 NEW PERSONAL BEST • $correct/${questions.size}" else "PERSONAL BEST • $bestScore/${questions.size}",14,green,true))
        questionArea.addView(label("ROUNDS COMPLETED: $completed  •  ACHIEVEMENTS: $achievementCount/4",12,muted,true))
        if (unlocked.isNotEmpty()) {
            questionArea.addView(label("NEW ACHIEVEMENT UNLOCKED",13,green,true))
            unlocked.forEach { questionArea.addView(label("🏆 $it",13,ink,true)) }
        }
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
            text = "Choose another category"
            isAllCaps = false
            setOnClickListener { chooseCategory() }
        })
        questionArea.addView(Button(this).apply {
            text = "Play again"
            isAllCaps = false
            setOnClickListener { prepareRound(); index=0; correct=0; fiftyUsed=false; progressBar.progress=0; renderQuestion() }
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
    private fun openRoomChat(){
        val id=intent.getStringExtra("game_room_id")?:return
        startActivity(Intent(this,GamesMultiplayerActivity::class.java).apply{
            putExtra("open_room_id",id)
            putExtra("game_display_name",intent.getStringExtra("game_display_name"))
            putExtra("chat_only",true)
        })
    }


}
