package com.example.paryavaran_kavalu.data

/** Stored in [UserEntity.userType] — legacy values; the app no longer exposes roles in the UI. */
object UserTypes {
    const val REPORTER = "Reporter"
    const val VOLUNTEER = "Volunteer"
    const val BOTH = "Both"

    val all = listOf(REPORTER, VOLUNTEER, BOTH)
}
