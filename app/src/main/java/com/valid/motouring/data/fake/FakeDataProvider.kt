package com.valid.motouring.data.fake

import com.valid.motouring.R
import com.valid.motouring.data.model.*

object FakeDataProvider {

    const val currentUserId = "u-me"

    // Route roughly following Jl. Sudirman -> Jl. Thamrin, Jakarta, used both as a
    // ride-history route preview and as the polyline RideSimulator animates along.
    val sampleRoute = listOf(
        GeoPoint(lat = -6.2246, lng = 106.8091),
        GeoPoint(lat = -6.2153, lng = 106.8149),
        GeoPoint(lat = -6.2088, lng = 106.8206),
        GeoPoint(lat = -6.1976, lng = 106.8235),
        GeoPoint(lat = -6.1875, lng = 106.8271),
    )

    val userLocation = GeoPoint(lat = -6.2088, lng = 106.8206)

    val users = listOf(
        User("u-me", "Rafi", R.drawable.ic_avatar_placeholder, listOf("v-1", "v-2")),
        User("u-2", "Dinda", R.drawable.ic_avatar_placeholder, listOf("v-3")),
        User("u-3", "Bagas", R.drawable.ic_avatar_placeholder, listOf("v-4")),
        User("u-4", "Sarah", R.drawable.ic_avatar_placeholder, listOf("v-5")),
        User("u-5", "Yoga", R.drawable.ic_avatar_placeholder, listOf("v-6")),
        User("u-6", "Nadia", R.drawable.ic_avatar_placeholder, listOf("v-7")),
    )

    val vehicles = listOf(
        Vehicle("v-1", "u-me", VehicleType.MOTORCYCLE, "Yamaha", "MT-25", 2023, R.drawable.img_vehicle_moto),
        Vehicle("v-2", "u-me", VehicleType.CAR, "Toyota", "Raize", 2022, R.drawable.img_vehicle_car),
        Vehicle("v-3", "u-2", VehicleType.MOTORCYCLE, "Honda", "CBR150R", 2021, R.drawable.img_vehicle_moto),
        Vehicle("v-4", "u-3", VehicleType.CAR, "Honda", "Civic", 2020, R.drawable.img_vehicle_car),
        Vehicle("v-5", "u-4", VehicleType.MOTORCYCLE, "Kawasaki", "Z250", 2022, R.drawable.img_vehicle_moto),
        Vehicle("v-6", "u-5", VehicleType.MOTORCYCLE, "Yamaha", "R15", 2023, R.drawable.img_vehicle_moto),
        Vehicle("v-7", "u-6", VehicleType.CAR, "Mazda", "CX-5", 2021, R.drawable.img_vehicle_car),
    )

    val rideBuddies = listOf(
        RideBuddy(users[1], BuddyStatus.FRIEND),
        RideBuddy(users[2], BuddyStatus.FRIEND),
        RideBuddy(users[3], BuddyStatus.FRIEND),
        RideBuddy(users[4], BuddyStatus.PENDING_RECEIVED),
        RideBuddy(users[5], BuddyStatus.NOT_CONNECTED),
    )

    val rideHistory = listOf(
        RideHistoryEntry("r-1", "Sudirman Sunday Loop", VehicleType.MOTORCYCLE, 18_400.0, 2_700, 24.5, R.drawable.img_road_1, listOf(R.drawable.img_road_2), 1_752_000_000),
        RideHistoryEntry("r-2", "Weekend Car Meet", VehicleType.CAR, 42_000.0, 5_400, 28.0, R.drawable.img_road_3, listOf(R.drawable.img_road_4, R.drawable.img_road_5), 1_752_400_000),
        RideHistoryEntry("r-3", "Night Ride to Puncak", VehicleType.MOTORCYCLE, 65_000.0, 9_000, 26.0, R.drawable.img_road_6, emptyList(), 1_752_800_000),
    )

    val challenges = listOf(
        Challenge(
            id = "c-1",
            title = "Ride 100km This Week",
            description = "Log at least 100km across any rides before the week ends.",
            metric = ChallengeMetric.DISTANCE_KM,
            goalValue = 100.0,
            currentValue = 62.0,
            deadlineEpochSeconds = 1_753_000_000,
            leaderboard = listOf(
                LeaderboardEntry("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 88.0),
                LeaderboardEntry("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 62.0),
                LeaderboardEntry("u-3", "Bagas", R.drawable.ic_avatar_placeholder, 40.0),
            ),
        ),
        Challenge(
            id = "c-2",
            title = "5 Rides This Month",
            description = "Complete 5 separate ride sessions this month, solo or group.",
            metric = ChallengeMetric.RIDE_COUNT,
            goalValue = 5.0,
            currentValue = 3.0,
            deadlineEpochSeconds = 1_754_500_000,
            leaderboard = listOf(
                LeaderboardEntry("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 3.0),
                LeaderboardEntry("u-4", "Sarah", R.drawable.ic_avatar_placeholder, 2.0),
            ),
        ),
        Challenge(
            id = "c-3",
            title = "Group Ride Weekend",
            description = "Join or host a group ride with 3+ riders this weekend.",
            metric = ChallengeMetric.RIDE_COUNT,
            goalValue = 1.0,
            currentValue = 0.0,
            deadlineEpochSeconds = 1_753_200_000,
            leaderboard = emptyList(),
        ),
    )

    val goalPresets = listOf(
        RideGoal(GoalType.DISTANCE, "10 km", 10_000.0),
        RideGoal(GoalType.DISTANCE, "25 km", 25_000.0),
        RideGoal(GoalType.DISTANCE, "50 km", 50_000.0),
        RideGoal(GoalType.DESTINATION, "Warung Kopi Susu", 8_000.0),
        RideGoal(GoalType.DESTINATION, "Puncak Pass", 60_000.0),
    )

    val badges = listOf(
        Badge("b-1", "First Ride", R.drawable.ic_badge_placeholder, "Complete your first tracked ride", "Complete 1 ride", true, 1_751_000_000),
        Badge("b-2", "Century Rider", R.drawable.ic_badge_placeholder, "Ride 100km in a single session", "Single ride >= 100km", false, null),
        Badge("b-3", "Squad Leader", R.drawable.ic_badge_placeholder, "Host a group ride with 5+ riders", "Host group ride, 5+ participants", true, 1_752_100_000),
        Badge("b-4", "Night Owl", R.drawable.ic_badge_placeholder, "Complete a ride starting after 10pm", "Ride start time >= 22:00", true, 1_752_800_500),
        Badge("b-5", "Early Bird", R.drawable.ic_badge_placeholder, "Complete a ride starting before 6am", "Ride start time <= 06:00", false, null),
        Badge("b-6", "Wrench Turner", R.drawable.ic_badge_placeholder, "Check in at 3 different repair shops", "3 unique repair shop check-ins", false, null),
        Badge("b-7", "Explorer", R.drawable.ic_badge_placeholder, "Make 3 or more stops in a single ride", "3+ goal stops in one ride", false, null),
        Badge("b-8", "Never Ending", R.drawable.ic_badge_placeholder, "Ride 50km or more without a goal", "50km+ on a single Endless leg", false, null),
    )

    val pois = listOf(
        PointOfInterest("p-1", "Pertamina Sudirman", PoiType.GAS_STATION, GeoPoint(lat = -6.2088, lng = 106.8206), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.3),
        PointOfInterest("p-2", "Shell Thamrin", PoiType.GAS_STATION, GeoPoint(lat = -6.1976, lng = 106.8235), setOf(VehicleType.CAR), 4.1),
        PointOfInterest("p-3", "Bengkel Motor Jaya", PoiType.REPAIR_SHOP, GeoPoint(lat = -6.2153, lng = 106.8149), setOf(VehicleType.MOTORCYCLE), 4.6),
        PointOfInterest("p-4", "Auto Repair Kemang", PoiType.REPAIR_SHOP, GeoPoint(lat = -6.2608, lng = 106.8130), setOf(VehicleType.CAR), 4.4),
        PointOfInterest("p-5", "Pertamina Kuningan", PoiType.GAS_STATION, GeoPoint(lat = -6.2241, lng = 106.8306), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.0),
        PointOfInterest("p-6", "Bengkel Jaya Motor 2", PoiType.REPAIR_SHOP, GeoPoint(lat = -6.1875, lng = 106.8271), setOf(VehicleType.MOTORCYCLE), 4.2),
        PointOfInterest("p-7", "Warung Rindu Alam", PoiType.REST_STOP, GeoPoint(lat = -6.2015, lng = 106.8180), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.5),
        PointOfInterest("p-8", "Kopi Titik Temu", PoiType.REST_STOP, GeoPoint(lat = -6.2200, lng = 106.8250), setOf(VehicleType.MOTORCYCLE, VehicleType.CAR), 4.7),
    )

    val comments = listOf(
        Comment("cm-1", "post-1", "u-2", "Dinda", R.drawable.ic_avatar_placeholder, "Nice route!", 1_752_000_500),
        Comment("cm-2", "post-1", "u-3", "Bagas", R.drawable.ic_avatar_placeholder, "Let's ride together next time", 1_752_000_800),
        Comment("cm-3", "post-2", "u-4", "Sarah", R.drawable.ic_avatar_placeholder, "That CX-5 looks clean", 1_752_400_400),
    )

    val posts = listOf(
        Post("post-1", "u-me", "Rafi", R.drawable.ic_avatar_placeholder, listOf(R.drawable.img_road_1), "Sunday morning loop around Sudirman", "r-1", 12, false, listOf("cm-1", "cm-2"), 1_752_000_100),
        Post("post-2", "u-6", "Nadia", R.drawable.ic_avatar_placeholder, listOf(R.drawable.img_road_2, R.drawable.img_road_3), "Weekend car meet turnout was huge", null, 24, true, listOf("cm-3"), 1_752_400_100),
        Post("post-3", "u-2", "Dinda", R.drawable.ic_avatar_placeholder, listOf(R.drawable.img_road_4), "New chain and sprocket installed", null, 8, false, emptyList(), 1_752_600_000),
    )

    val notifications = listOf(
        Notification("n-1", NotificationType.RIDE_INVITE, "Bagas invited you to a group ride", 1_752_900_000, false),
        Notification("n-2", NotificationType.BADGE_EARNED, "You earned the Night Owl badge", 1_752_800_600, false),
        Notification("n-3", NotificationType.CHALLENGE_PROGRESS, "You're 62% through Ride 100km This Week", 1_752_950_000, true),
        Notification("n-4", NotificationType.SOCIAL, "Dinda commented on your post", 1_752_000_900, true),
    )

    fun previewRideSessionWithGoal(): RideSession = RideSession(
        id = "preview-goal",
        vehicleType = VehicleType.MOTORCYCLE,
        route = sampleRoute,
        participants = listOf(
            RideParticipantState("u-me", "Rafi", R.drawable.ic_avatar_placeholder, sampleRoute[4], role = RiderRole.LEAD, distanceAlongRouteMeters = 6_000.0),
            RideParticipantState("u-2", "Dinda", R.drawable.ic_avatar_placeholder, sampleRoute[3], isSpeaking = true, role = RiderRole.RIDER, distanceAlongRouteMeters = 5_880.0),
            RideParticipantState("u-3", "Bagas", R.drawable.ic_avatar_placeholder, sampleRoute[1], role = RiderRole.SWEEP, distanceAlongRouteMeters = 5_400.0, hasFallenBehind = true),
        ),
        distanceMeters = 6_000.0,
        speedKmh = 28.0,
        elapsedSeconds = 720,
        status = RideSessionStatus.ACTIVE,
        mode = RideMode.GOAL,
        activeGoal = RideGoal(GoalType.DISTANCE, "10 km", 10_000.0),
    )

    fun previewRideSessionEndless(): RideSession = RideSession(
        id = "preview-endless",
        vehicleType = VehicleType.MOTORCYCLE,
        route = sampleRoute,
        participants = listOf(
            RideParticipantState("u-me", "Rafi", R.drawable.ic_avatar_placeholder, sampleRoute[4], role = RiderRole.LEAD, distanceAlongRouteMeters = 12_500.0),
            RideParticipantState("u-2", "Dinda", R.drawable.ic_avatar_placeholder, sampleRoute[2], role = RiderRole.SWEEP, distanceAlongRouteMeters = 12_050.0),
        ),
        distanceMeters = 12_500.0,
        speedKmh = 26.0,
        elapsedSeconds = 1_500,
        status = RideSessionStatus.ACTIVE,
        mode = RideMode.ENDLESS,
        completedLegs = listOf(
            Leg(RideGoal(GoalType.DISTANCE, "10 km", 10_000.0), 10_000.0, 1_200, 30.0, LegEndReason.GOAL_REACHED),
        ),
    )
}
