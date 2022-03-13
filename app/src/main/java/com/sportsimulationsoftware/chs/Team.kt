package com.sportsimulationsoftware.chs

class Team {
    var teamId: String? = null
    var teamName: String? = null
    var fullName: String? = null
    var gamesPlayed: Int = 0
    var fgMade: Float = 0F
    var fgAtt: Float = 0F
    var ftMade : Float = 0F
    var ftAtt : Float = 0F
    var tpMade : Float = 0F
    var tpAtt : Float = 0F
    var offReb : Float = 0F
    var defReb : Float = 0F
    var turnovers : Float = 0F
    var steals : Float = 0F
    var blocks : Float = 0F
    var fouls : Int = 0

    constructor() {}
    constructor(teamName: String? = null) {
        this.teamName = teamName
    }
    constructor(teamId: String? = null, teamName: String? = null) {
        this.teamId = teamId
        this.teamName = teamName
    }

    companion object {
        fun findTeamByName(name: String, teams: MutableList<Team>): Team {
            val matches = teams.filter { t -> t.teamName == name }
            if (matches.isEmpty()) {
                val team = Team()
                teams.add(team)
                return team
            }

            return matches.single()
        }
    }
}
