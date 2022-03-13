package com.sportsimulationsoftware.chs

/**
 * Created by w20682 on 2/20/18.
 */
class Conference {
    var conferenceId: String? = null
    var conferenceName: String? = null

    constructor() {}
    constructor(conferenceId: String? = null, conferenceName: String? = null) {
        this.conferenceId = conferenceId
        this.conferenceName = conferenceName
    }
}