package com.example.paryavaran_kavalu.data

/** Stored in [UserEntity.userType] — single-device profile roles for reporting vs cleanup. */
object UserTypes {
    const val REPORTER = "Reporter"
    const val VOLUNTEER = "Volunteer"
    const val BOTH = "Both"

    val all = listOf(REPORTER, VOLUNTEER, BOTH)
}
