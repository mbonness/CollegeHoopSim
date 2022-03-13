package com.sportsimulationsoftware.chs

import android.content.Context
import android.util.Log
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

/**
 * Created by mbonn on 9/19/2017.
 */

class StatsParser {
    companion object {
        fun parseStats(context: Context, conference: Conference, teams: MutableList<Team>) {
            Log.d("StatsParser", "Parsing stats for " + conference.conferenceName)

            // points per game
            var htmlFile = File(context.filesDir.path + "/" + conference.conferenceName + "_scoring-per-game.html")
            var doc = Jsoup.parse(htmlFile, "UTF-8")
            var tableHead = doc.select("table.tablehead")
            var rows = tableHead.select("tr.oddrow") + tableHead.select("tr.evenrow")
            for (row in rows) {
                parsePointsPerGameRow(row, teams)
            }

            // rebounds
            htmlFile = File(context.filesDir.path + "/" + conference.conferenceName + "_rebounds.html")
            doc = Jsoup.parse(htmlFile, "UTF-8")
            tableHead = doc.select("table.tablehead")
            rows = tableHead.select("tr.oddrow") + tableHead.select("tr.evenrow")
            for (row in rows) {
                parseReboundsRow(row, teams)
            }

            // steals
            htmlFile = File(context.filesDir.path + "/" + conference.conferenceName + "_steals.html")
            doc = Jsoup.parse(htmlFile, "UTF-8")
            tableHead = doc.select("table.tablehead")
            rows = tableHead.select("tr.oddrow") + tableHead.select("tr.evenrow")
            for (row in rows) {
                parseStealsRow(row, teams)
            }

            // blocks
            htmlFile = File(context.filesDir.path + "/" + conference.conferenceName + "_blocks.html")
            doc = Jsoup.parse(htmlFile, "UTF-8")
            tableHead = doc.select("table.tablehead")
            rows = tableHead.select("tr.oddrow") + tableHead.select("tr.evenrow")
            for (row in rows) {
                parseBlocksRow(row, teams)
            }
        }

        private fun parsePointsPerGameRow(row: Element, teams: MutableList<Team>) {
            val cells = row.select("td")

            if (cells.size < 10) {
                Log.wtf("StatsParser", "Points per game row does not have enough cells to parse")
                return
            }

            val teamName = cells[1].text()
            val team = Team.findTeamByName(teamName, teams)

            team.teamName = cells[1].text()
            team.gamesPlayed = cells[2].text().toInt()
            team.fgMade = cells[4].text().split("-")[0].toFloat()
            team.fgAtt = cells[4].text().split("-")[1].toFloat()
            team.tpMade = cells[6].text().split("-")[0].toFloat()
            team.tpAtt = cells[6].text().split("-")[1].toFloat()
            team.ftMade = cells[8].text().split("-")[0].toFloat()
            team.ftAtt = cells[8].text().split("-")[1].toFloat()
        }

        private fun parseReboundsRow(row: Element, teams: MutableList<Team>) {
            val cells = row.select("td")

            if (cells.size < 9) {
                Log.wtf("StatsParser", "Rebounds row does not have enough cells to parse")
                return
            }

            val teamName = cells[1].text()
            val team = Team.findTeamByName(teamName, teams)

            team.offReb = cells[4].text().toFloat()
            team.defReb = cells[6].text().toFloat()
        }

        private fun parseStealsRow(row: Element, teams: MutableList<Team>) {
            val cells = row.select("td")

            if (cells.size < 10) {
                Log.wtf("StatsParser", "Steals row does not have enough cells to parse")
                return
            }

            val teamName = cells[1].text()
            val team = Team.findTeamByName(teamName, teams)

            team.steals = cells[4].text().toFloat()
            team.turnovers = cells[6].text().toFloat()
            team.fouls = cells[7].text().toInt()
        }

        private fun parseBlocksRow(row: Element, teams: MutableList<Team>) {
            val cells = row.select("td")

            if (cells.size < 7) {
                Log.wtf("StatsParser", "Blocks row does not have enough cells to parse")
                return
            }

            val teamName = cells[1].text()
            val team = Team.findTeamByName(teamName, teams)

            team.blocks = cells[5].text().toFloat()
        }
    }
}