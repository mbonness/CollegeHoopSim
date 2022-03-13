package com.sportsimulationsoftware.chs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import java.util.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import android.graphics.drawable.Drawable

class HoopSimActivity : Activity() {
    private var mVisitorTeam: Team? = null
    private var mHomeTeam: Team? = null
    private var mVisTotalPoints: Long = 0
    private var mHomeTotalPoints: Long = 0
    private var mProgressDialog: ProgressDialog? = null
    private var mProgress: Int = 0
    private var mProgressHandler: Handler? = null
    private val mNumGames: Int = 1000
    private val mNumConferences = 32
    private var mStopSimulation: Boolean = false
    private var mStopSync = false
    private var mState = State.Idle
    private var mTeams: MutableList<Team> = ArrayList<Team>()

    @SuppressLint("HandlerLeak")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hoop_sim)

        val spinnerVisTeam = findViewById<Spinner>(R.id.SpinnerVisTeam)
        val spinnerHomeTeam = findViewById<Spinner>(R.id.SpinnerHomeTeam)
        val imageViewVisTeam = findViewById<ImageView>(R.id.imageViewVisitorTeam)
        val imageViewHomeTeam = findViewById<ImageView>(R.id.imageViewHomeTeam)
        val buttonSimulate = findViewById<Button>(R.id.buttonSimulate)
        val buttonSync = findViewById<Button>(R.id.buttonSync)
        val visScore = findViewById<TextView>(R.id.VisScoreTextView)
        val homeScore = findViewById<TextView>(R.id.HomeScoreTextView)

        spinnerVisTeam.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (id > 0) {
                    loadTeamImage(spinnerVisTeam.selectedItem.toString(), imageViewVisTeam)
                    if (spinnerHomeTeam.selectedItemId > 0) {
                        buttonSimulate.visibility = View.VISIBLE
                    }
                } else {
                    imageViewVisTeam.visibility = View.GONE
                    buttonSimulate.visibility = View.INVISIBLE
                }
                hideResults()
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }

        spinnerHomeTeam.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (id > 0) {
                    loadTeamImage(spinnerHomeTeam.selectedItem.toString(), imageViewHomeTeam)
                    if (spinnerVisTeam.selectedItemId > 0) {
                        buttonSimulate.visibility = View.VISIBLE
                    }
                } else {
                    imageViewHomeTeam.visibility = View.GONE
                    buttonSimulate.visibility = View.INVISIBLE
                }
                hideResults()
            }

            override fun onNothingSelected(arg0: AdapterView<*>) {}
        }

        buttonSync.setOnClickListener {
            mState = State.SyncStats
            syncStats()
        }

        buttonSimulate.setOnClickListener {
            mState = State.Simulate
            if (loadStats()) {
                simulate()
            }
        }

        mProgressHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val max = if (mState == State.SyncStats) mNumConferences else mNumGames
                if (mProgress >= max) {
                    mProgressDialog!!.dismiss()
                } else {
                    mProgress++
                    mProgressDialog!!.incrementProgressBy(1)
                }
            }
        }

        val t = Typeface.createFromAsset(assets, "fonts/scoreboard-regular.ttf")
        visScore.setTypeface(t, Typeface.NORMAL)
        homeScore.setTypeface(t, Typeface.NORMAL)
    }

    protected fun showResults() {
        // calculate average home/vis score
        var homeScore = 0
        var visScore = 0
        if (mNumGames > 0) {
            homeScore = (mHomeTotalPoints / mNumGames).toInt()
            visScore = (mVisTotalPoints / mNumGames).toInt()

            val visScoreTextView = findViewById<TextView>(R.id.VisScoreTextView)
            val homeScoreTextView = findViewById<TextView>(R.id.HomeScoreTextView)
            val linearLayoutScoreboard = findViewById<LinearLayout>(R.id.linearLayoutScoreboard)

            visScoreTextView.text = Integer.valueOf(visScore)!!.toString()
            homeScoreTextView.text = Integer.valueOf(homeScore)!!.toString()
            linearLayoutScoreboard.visibility = View.VISIBLE
        } else {
            Toast.makeText(this@HoopSimActivity, "Invalid number of games - please check settings", Toast.LENGTH_LONG).show()
        }
    }

    protected fun hideResults() {
        val visScoreTextView = findViewById<TextView>(R.id.VisScoreTextView)
        val homeScoreTextView = findViewById<TextView>(R.id.HomeScoreTextView)
        val linearLayoutScoreboard = findViewById<LinearLayout>(R.id.linearLayoutScoreboard)
        visScoreTextView.text = "0"
        homeScoreTextView.text = "0"
        linearLayoutScoreboard.visibility = View.INVISIBLE
    }

    protected fun syncStats() {
        // show progress dialog
        showDialog(0)
        mProgress = 0
        mProgressDialog!!.progress = 0
        mProgressDialog!!.max = mNumConferences
        mStopSync = false

        // launch AsyncTask to sync stats
        SyncTask(this).execute()
    }

    private inner class SyncTask(private val mHoopSimActivity: HoopSimActivity) : AsyncTask<Void, Int, Void>() {
        // http://www.espn.com/mens-college-basketball/conferences/statistics/team/_/stat/scoring-per-game/year/2021/seasontype/2/id/7
        // http://www.espn.com/mens-college-basketball/conferences/statistics/team/_/id/7/stat/scoring-per-game
        val statsBaseUrl = "http://www.espn.com/mens-college-basketball/conferences/statistics/team/_/stat/"

        override fun doInBackground(vararg params: Void): Void? {
            Log.d("HoopSimActivity", "Syncing stats...")
            val conferenceFactory = ConferenceFactory()
            val conferences = conferenceFactory.getConferences()

            for ((index, conference) in conferences.withIndex()) {
                Log.d("HoopSimActivity", "Syncing stats for conference " + conference.conferenceName)
                try {
                    downloadStatsCategory("scoring-per-game", conference)
                    downloadStatsCategory("rebounds", conference)
                    downloadStatsCategory("steals", conference)
                    downloadStatsCategory("blocks", conference)

                    Log.d("HoopSimActivity", "Parsing stats for conference " + conference.conferenceName)
                    StatsParser.parseStats(this@HoopSimActivity, conference, mTeams)
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null
                }

                // update progress bar
                publishProgress(index + 1)

                if (mStopSync) break
            }

            for (team in mTeams) {
                Log.d("StatsParser", "Parsed stats for team " + team.teamName + " (GP=" + team.gamesPlayed +
                        " FGM=" + team.fgMade + " FGA=" + team.fgAtt +
                        " 3PM=" + team.tpMade + " 3PA=" + team.tpAtt +
                        " FTM=" + team.ftMade + " FTA=" + team.ftAtt +
                        " ORPG=" + team.offReb + " DRPG=" + team.defReb +
                        " STPG=" + team.steals + " BLKPG=" + team.blocks +
                        " TOPG=" + team.turnovers + " PF=" + team.fouls +
                        ")")
            }

            return null
        }

        private fun downloadStatsCategory(category: String, conference: Conference) {
            Log.d("HoopSimActivity", "Downloading stats category: $category")
            // http://www.espn.com/mens-college-basketball/conferences/statistics/team/_/id/7/stat/scoring-per-game
            val htmlText = downloadData(statsBaseUrl + category + "/id/" + conference.conferenceId)
            val htmlFile = File(filesDir.path + "/" + conference.conferenceName + "_" + category + ".html")
            htmlFile.writeText(htmlText)
            Log.d("HoopSimActivity", "Downloaded " + htmlText.length + " bytes")
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)

            mProgressHandler!!.sendEmptyMessage(0)
        }

        override fun onPostExecute(result: Void?) {
            mProgressDialog!!.dismiss()
            mState = State.Idle
        }

        @Throws(IOException::class)
        private fun downloadData(urlString: String): String {
            var inputStream: InputStream? = null
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connect()

                inputStream = conn.inputStream
                return inputStream.bufferedReader().use { it.readText() }
            } finally {
                if (inputStream != null) {
                    inputStream.close()
                }
            }
        }
    }

    protected fun simulate() {
        val homeFieldAdvStr = "0"

        // show progress dialog
        showDialog(0)
        mProgress = 0
        mProgressDialog!!.progress = 0
        mProgressDialog!!.max = mNumGames
        mStopSimulation = false

        // launch AsyncTask to simulate games
        SimulateTask(this).execute(homeFieldAdvStr)
    }

    private inner class SimulateTask(private val mHoopSimActivity: HoopSimActivity) : AsyncTask<String, Int, Long>() {
        val rnd = Random() // seed random number generator
        var curTeam: Int = 0

        override fun doInBackground(vararg params: String): Long? {
            var homeScore: Int
            var visScore: Int
            var numPlays: Int
            val maxNumPlays: Double = 160.0 // about 2.5 plays/minute for a 60 minute game
            var coinFlip: Int
            var diceRoll: Float
            var fieldGoalPct: Double = 0.0
            var turnoverPct: Double = 0.0
            var foulPct: Double = 0.0
            var ftPct: Double = 0.0
            var treyPct: Double = 0.0
            var treyMadePct: Double = 0.0
            var offRebPerGame: Double = 0.0
            var defRebPerGame: Double = 0.0
            var offRebPct: Double = 0.0
            var homeFieldAdv: Int

            // HFA
            val homeFieldAdvStr = params[0]
            try {
                homeFieldAdv = Integer.parseInt(homeFieldAdvStr)
            } catch (e: Exception) {
                homeFieldAdv = 3
            }

            // simulate number of games specified
            mHomeTotalPoints = 0
            mVisTotalPoints = 0
            var i = 0
            while (i < mNumGames && !mStopSimulation) {
                // update progress bar
                publishProgress(i)

                // initialize game variables
                homeScore = homeFieldAdv
                visScore = 0

                // flip coin to determine who gets possession at opening tip
                // team 1 = visitors, team 2 = home
                curTeam = flipCoin() + 1

                // simulate fixed number of plays per game
                numPlays = 0
                while (numPlays < maxNumPlays && !mStopSimulation) {
                    numPlays++

                    // assume turnover can occur any given play
                    // flip coin to determine whether offense or defense turnover stats are used
                    coinFlip = flipCoin()
                    when (coinFlip) {
                        1 -> {
                            // use offense's turnover stats
                            when (curTeam) {
                                1 -> turnoverPct = mVisitorTeam!!.turnovers / maxNumPlays * 2.0 // visitor offense
                                2 -> turnoverPct = mHomeTeam!!.turnovers / maxNumPlays * 2.0 // home offense
                            }
                        }
                        0 -> {
                            // use defense's steal and blocked shot stats
                            when (curTeam) {
                                1 -> turnoverPct = (mHomeTeam!!.steals + mHomeTeam!!.blocks) / maxNumPlays * 2.0 // home defense
                                2 -> turnoverPct = (mVisitorTeam!!.steals + mVisitorTeam!!.blocks) / maxNumPlays * 2.0 // visitor
                            }
                        }
                    }

                    // roll dice to see if turnover occurs
                    diceRoll = rollDice()
                    if (diceRoll < turnoverPct) {
                        // turnover - change of possession occurs
                        changeOfPossession()
                    } else {
                        // no turnover
                        // check for fouls next
                        // flip coin to determine whether offense or defense foul stats are used
                        coinFlip = flipCoin()
                        when (coinFlip) {
                            0 -> {
                                // use offense free throw sttas
                                when (curTeam) {
                                    1 -> foulPct = mVisitorTeam!!.ftAtt / maxNumPlays // visitor offense
                                    2 -> foulPct = mHomeTeam!!.ftAtt / maxNumPlays // home offense
                                }
                            }
                            1 -> {
                                // use defense foul stats
                                when (curTeam) {
                                    1 -> foulPct = mHomeTeam!!.fouls / mHomeTeam!!.gamesPlayed / maxNumPlays * 2.0 // home
                                // defense
                                    2 -> foulPct = mVisitorTeam!!.fouls / mVisitorTeam!!.gamesPlayed / maxNumPlays * 2.0 // visitor
                                // defense
                                }
                            }
                        }

                        // use previous dice roll to see if player gets fouled
                        if (diceRoll < (turnoverPct + foulPct)) {
                            // foul
                            // get free throw shooting percentage from offense
                            when (curTeam) {
                                1 -> ftPct = (mVisitorTeam!!.ftMade / mVisitorTeam!!.ftAtt).toDouble()
                                2 -> ftPct = (mHomeTeam!!.ftMade / mHomeTeam!!.ftAtt).toDouble()
                            }

                            // roll dice to see if first free throw is good
                            var diceRoll2 = rollDice()
                            if (diceRoll2 < ftPct) {
                                // player sinks first foul shot
                                when (curTeam) {
                                    1 -> visScore++
                                    2 -> homeScore++
                                }
                            }

                            // roll dice to see if second free throw is good
                            diceRoll2 = rollDice()
                            if (diceRoll2 < ftPct) {
                                // player sinks second foul shot
                                when (curTeam) {
                                    1 -> visScore++
                                    2 -> homeScore++
                                }
                            }

                            // change of possession occurs
                            changeOfPossession()
                        } else {
                            // no foul
                            // player will shoot the ball
                            // calculate rebounding percentages if needed
                            when (curTeam) {
                                1 -> {
                                    offRebPerGame = mVisitorTeam!!.offReb.toDouble()
                                    defRebPerGame = mHomeTeam!!.defReb.toDouble()
                                }
                                2 -> {
                                    offRebPerGame = mHomeTeam!!.offReb.toDouble()
                                    defRebPerGame = mVisitorTeam!!.defReb.toDouble()
                                }
                            }
                            offRebPct = offRebPerGame / (defRebPerGame + offRebPerGame)

                            // check for three-pointers next
                            // use offense's 3-point stats
                            when (curTeam) {
                                1 -> {
                                    // use visitor offense 3-point and field goal stats
                                    treyPct = mVisitorTeam!!.tpAtt / maxNumPlays * 2.0
                                    treyMadePct = (mVisitorTeam!!.tpMade / mVisitorTeam!!.tpAtt).toDouble()
                                    fieldGoalPct = (mVisitorTeam!!.fgMade / mVisitorTeam!!.fgAtt).toDouble()
                                }
                                2 -> {
                                    // use home offense 3-point and field goal stats
                                    treyPct = mHomeTeam!!.tpAtt / maxNumPlays * 2.0
                                    treyMadePct = (mHomeTeam!!.tpMade / mHomeTeam!!.tpAtt).toDouble()
                                    fieldGoalPct = (mHomeTeam!!.fgMade / mHomeTeam!!.fgAtt).toDouble()
                                }
                            }

                            // use previous dice roll to see if 3-pointer is attempted
                            if (diceRoll < (turnoverPct + foulPct + treyPct)) {
                                // player attempts a 3-pointer
                                // roll the dice to see if 3-pointer is good
                                diceRoll = rollDice()
                                if (diceRoll < treyMadePct) {
                                    // player sinks the three
                                    when (curTeam) {
                                        1 -> visScore += 3
                                        2 -> homeScore += 3
                                    }

                                    // change of possession occurs
                                    changeOfPossession()
                                } else {
                                    // player misses the three
                                    // rebounding situation
                                    // roll the dice to see who gets the ball
                                    diceRoll = rollDice()
                                    if (diceRoll < offRebPct) {
                                        // offense keeps the ball
                                    } else {
                                        // change of possession
                                        changeOfPossession()
                                    }
                                }
                            } else {
                                // player attempts a two-pointer
                                // roll the dice to see if basket is good
                                diceRoll = rollDice()
                                if (diceRoll < fieldGoalPct) {
                                    // player makes the bucket
                                    when (curTeam) {
                                        1 -> visScore += 2
                                        2 -> homeScore += 2
                                    }

                                    // change of possession occurs
                                    changeOfPossession()
                                } else {
                                    // player misses the shot
                                    // rebounding situation
                                    // roll the dice to see who gets the ball
                                    diceRoll = rollDice()
                                    if (diceRoll < offRebPct) {
                                        // offense keeps the ball
                                    } else {
                                        // change of possession
                                        changeOfPossession()
                                    }
                                }
                            }
                        }
                    }
                }

                // update home/away scores
                mHomeTotalPoints += homeScore.toLong()
                mVisTotalPoints += visScore.toLong()
                i++
            }
            return 0L
        }

        private fun flipCoin(): Int {
            val coinFlip = (2 * rnd.nextFloat()).toInt()
            return coinFlip
        }

        private fun rollDice(): Float {
            val diceRoll = rnd.nextFloat()
            return diceRoll
        }

        private fun changeOfPossession() {
            when (curTeam) {
                1 -> curTeam = 2
                2 -> curTeam = 1
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)

            mProgressHandler!!.sendEmptyMessage(0)
        }

        override fun onPostExecute(result: Long?) {
            mProgressDialog!!.dismiss()

            if (!mStopSimulation) {
                mHoopSimActivity.showResults()
            }

            mState = State.Idle
        }
    }

    override fun onCreateDialog(id: Int): Dialog {
        val progressDialog = ProgressDialog(this@HoopSimActivity)
        progressDialog!!.setIcon(R.drawable.basketball_icon)
        progressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog!!.setTitle("Please wait...")
        progressDialog!!.max = 1
        progressDialog!!.setButton("Hide") { dialog, whichButton -> }
        progressDialog!!.setButton2("Cancel") { dialog, whichButton -> }
        mProgressDialog = progressDialog
        return progressDialog
    }

    override fun onPrepareDialog(id: Int, dialog: Dialog) {
        super.onPrepareDialog(id, dialog)

        val progressDialog = dialog as ProgressDialog
        if (mState == State.Simulate) {
            progressDialog.setTitle("Simulating...")
            progressDialog.max = mNumGames
            progressDialog.setButton("Hide") { dialog, whichButton -> }
            progressDialog.setButton2("Cancel") { dialog, whichButton -> mStopSimulation = true }
        } else {
            progressDialog.setTitle("Downloading stats...")
            progressDialog.max = mNumConferences
            progressDialog.setButton("Hide") { dialog, whichButton -> }
            progressDialog.setButton2("Cancel") { dialog, whichButton -> mStopSync = true }
        }
    }

    protected fun loadStats(): Boolean {
        // load stats from HTML files into memory
        val conferenceFactory = ConferenceFactory()
        val conferences = conferenceFactory.getConferences()
        for (conference in conferences) {
            try {
                StatsParser.parseStats(this@HoopSimActivity, conference, mTeams)
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        }


//        for (team in mTeams) {
//            Log.d("StatsParser", "Parsed stats for team " + team.teamName + " (GP=" + team.gamesPlayed +
//                    " FGM=" + team.fgMade + " FGA=" + team.fgAtt +
//                    " 3PM=" + team.tpMade + " 3PA=" + team.tpAtt +
//                    " FTM=" + team.ftMade + " FTA=" + team.ftAtt +
//                    " ORPG=" + team.offReb + " DRPG=" + team.defReb +
//                    " STPG=" + team.steals + " BLKPG=" + team.blocks +
//                    " TOPG=" + team.turnovers + " PF=" + team.fouls +
//                    ")")
//        }

        // find selected teams
        val spinnerVisTeam = findViewById<Spinner>(R.id.SpinnerVisTeam)
        val spinnerHomeTeam = findViewById<Spinner>(R.id.SpinnerHomeTeam)
        val visitorTeam = Team.findTeamByName(spinnerVisTeam.selectedItem.toString(), mTeams)
        val homeTeam = Team.findTeamByName(spinnerHomeTeam.selectedItem.toString(), mTeams)
        mVisitorTeam = visitorTeam
        mHomeTeam = homeTeam
        if (visitorTeam.gamesPlayed > 0 && homeTeam.gamesPlayed > 0) {
            return true
        }
        if (visitorTeam.gamesPlayed == 0) {
            Toast.makeText(this, "Could not find stats for visitor", Toast.LENGTH_LONG).show()
        }
        if (homeTeam.gamesPlayed == 0) {
            Toast.makeText(this, "Could not find stats for home", Toast.LENGTH_LONG).show()
        }
        return false
    }

    protected fun loadTeamImage(team: String, imageViewTeam: ImageView) {
        imageViewTeam.visibility = View.VISIBLE

        // load image
        try {
            // get input stream
            val ims = assets.open("images/" + team.toLowerCase() + ".bmp")
            // load image as Drawable
            val d = Drawable.createFromStream(ims, null)
            // set image to ImageView
            imageViewTeam.setImageDrawable(d)
        } catch (ex: IOException) {
            Log.e("HoopSimActivity", ex.toString())
            return
        }
    }
}
