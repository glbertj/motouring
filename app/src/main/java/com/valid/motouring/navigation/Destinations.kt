package com.valid.motouring.navigation

object Destinations {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val VEHICLE_GARAGE_SETUP = "vehicle_garage_setup"

    const val MAIN = "main"
    const val HOME = "home"
    const val NEARBY = "nearby"
    const val CHALLENGES = "challenges"
    const val RIDES_HISTORY = "rides_history"
    const val PROFILE = "profile"

    const val CHALLENGE_DETAIL_PATTERN = "challenge_detail/{challengeId}"
    fun challengeDetail(challengeId: String) = "challenge_detail/$challengeId"

    const val BADGES = "badges"
    const val BADGE_DETAIL_PATTERN = "badge_detail/{badgeId}"
    fun badgeDetail(badgeId: String) = "badge_detail/$badgeId"

    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"
    const val TRUSTED_CONTACTS = "trusted_contacts"
    const val NOTIFICATIONS = "notifications"
    const val VEHICLE_MAINTENANCE_PATTERN = "vehicle_maintenance/{vehicleId}"
    fun vehicleMaintenance(vehicleId: String) = "vehicle_maintenance/$vehicleId"

    const val START_RIDE = "start_ride"
    const val FRIENDS = "friends"
    const val INVITE_RIDE = "invite_ride"

    const val RIDE_SESSION_PATTERN = "ride_session/{vehicleType}/{isGroup}"
    fun rideSession(vehicleType: String, isGroup: Boolean) = "ride_session/$vehicleType/$isGroup"

    const val RIDE_SUMMARY_PATTERN = "ride_summary/{historyEntryId}"
    fun rideSummary(historyEntryId: String) = "ride_summary/$historyEntryId"

    const val CREATE_POST = "create_post"
    const val POST_DETAIL_PATTERN = "post_detail/{postId}"
    fun postDetail(postId: String) = "post_detail/$postId"

    const val SEGMENTS = "segments"
    const val SEGMENT_DETAIL_PATTERN = "segment_detail/{segmentId}"
    fun segmentDetail(segmentId: String) = "segment_detail/$segmentId"
}
