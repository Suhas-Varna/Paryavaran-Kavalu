package com.example.paryavaran_kavalu.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.NaturePeople
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.paryavaran_kavalu.data.UserTypes

/** Icon shown on the map header and in profile to reflect local role choice. */
fun userTypeIcon(userType: String?): ImageVector {
    return when (userType?.trim()) {
        UserTypes.VOLUNTEER -> Icons.Outlined.VolunteerActivism
        UserTypes.BOTH -> Icons.Outlined.NaturePeople
        else -> Icons.Outlined.EditNote
    }
}

fun userTypeShortDescription(userType: String?): String {
    return when (userType?.trim()) {
        UserTypes.VOLUNTEER -> "Volunteer (cleanup)"
        UserTypes.BOTH -> "Reporter & volunteer"
        else -> "Reporter (spot issues)"
    }
}
