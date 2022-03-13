package com.sportsimulationsoftware.chs

/**
 * Created by w20682 on 2/20/18.
 */
class ConferenceFactory {
    fun getConferences(): Array<Conference> {
        return arrayOf(
                Conference(conferenceId = "1", conferenceName = "america_east"),
                Conference(conferenceId = "2", conferenceName = "acc"),
                Conference(conferenceId = "3", conferenceName = "atlantic10"),
                Conference(conferenceId = "4", conferenceName = "big_east"),
                Conference(conferenceId = "5", conferenceName = "big_sky"),
                Conference(conferenceId = "6", conferenceName = "big_south"),
                Conference(conferenceId = "7", conferenceName = "big_ten"),
                Conference(conferenceId = "8", conferenceName = "big12"),
                Conference(conferenceId = "9", conferenceName = "big_west"),
                Conference(conferenceId = "10", conferenceName = "colonial"),
                Conference(conferenceId = "11", conferenceName = "conf_usa"),
                Conference(conferenceId = "12", conferenceName = "ivy"),
                Conference(conferenceId = "13", conferenceName = "metro_atlantic"),
                Conference(conferenceId = "14", conferenceName = "mid_american"),
                Conference(conferenceId = "16", conferenceName = "mid_eastern"),
                Conference(conferenceId = "18", conferenceName = "missouri_valley"),
                Conference(conferenceId = "19", conferenceName = "northeast"),
                Conference(conferenceId = "20", conferenceName = "ohio_valley"),
                Conference(conferenceId = "21", conferenceName = "pac12"),
                Conference(conferenceId = "22", conferenceName = "patriot"),
                Conference(conferenceId = "23", conferenceName = "sec"),
                Conference(conferenceId = "24", conferenceName = "southern"),
                Conference(conferenceId = "25", conferenceName = "southland"),
                Conference(conferenceId = "26", conferenceName = "southwestern"),
                Conference(conferenceId = "27", conferenceName = "sunbelt"),
                Conference(conferenceId = "29", conferenceName = "west_coast"),
                Conference(conferenceId = "30", conferenceName = "wac"),
                Conference(conferenceId = "44", conferenceName = "mountain_west"),
                Conference(conferenceId = "45", conferenceName = "horizon"),
                Conference(conferenceId = "46", conferenceName = "atlantic_sun"),
                Conference(conferenceId = "49", conferenceName = "summit"),
                Conference(conferenceId = "62", conferenceName = "american")
        )
    }
}
