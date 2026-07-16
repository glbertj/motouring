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
        Vehicle("v-1", "u-me", VehicleType.MOTORCYCLE, "Yamaha", "MT-25", 2023, R.drawable.img_vehicle_moto, odometerKm = 12_480),
        Vehicle("v-2", "u-me", VehicleType.CAR, "Toyota", "Raize", 2022, R.drawable.img_vehicle_car, odometerKm = 34_200),
        Vehicle("v-3", "u-2", VehicleType.MOTORCYCLE, "Honda", "CBR150R", 2021, R.drawable.img_vehicle_moto),
        Vehicle("v-4", "u-3", VehicleType.CAR, "Honda", "Civic", 2020, R.drawable.img_vehicle_car),
        Vehicle("v-5", "u-4", VehicleType.MOTORCYCLE, "Kawasaki", "Z250", 2022, R.drawable.img_vehicle_moto),
        Vehicle("v-6", "u-5", VehicleType.MOTORCYCLE, "Yamaha", "R15", 2023, R.drawable.img_vehicle_moto),
        Vehicle("v-7", "u-6", VehicleType.CAR, "Mazda", "CX-5", 2021, R.drawable.img_vehicle_car),
    )

    val serviceItems = listOf(
        // Yamaha MT-25 (v-1) @ 12,480 km  -> Tires OVERDUE, Chain DUE_SOON, Oil/Brakes OK  (2 due)
        ServiceItem("v-1", ServiceType.OIL, lastServicedKm = 9_900, intervalKm = 6_000),
        ServiceItem("v-1", ServiceType.CHAIN, lastServicedKm = 11_850, intervalKm = 700),
        ServiceItem("v-1", ServiceType.TIRES, lastServicedKm = 2_300, intervalKm = 10_000),
        ServiceItem("v-1", ServiceType.BRAKES, lastServicedKm = 8_000, intervalKm = 15_000),
        // Toyota Raize (v-2) @ 34,200 km  -> Coolant DUE_SOON, rest OK  (1 due)
        ServiceItem("v-2", ServiceType.OIL, lastServicedKm = 30_000, intervalKm = 10_000),
        ServiceItem("v-2", ServiceType.TIRES, lastServicedKm = 20_000, intervalKm = 40_000),
        ServiceItem("v-2", ServiceType.BRAKES, lastServicedKm = 18_000, intervalKm = 30_000),
        ServiceItem("v-2", ServiceType.COOLANT, lastServicedKm = 5_000, intervalKm = 30_000),
    )

    val rideBuddies = listOf(
        RideBuddy(users[1], BuddyStatus.FRIEND, isTrustedContact = true),
        RideBuddy(users[2], BuddyStatus.FRIEND, isTrustedContact = true),
        RideBuddy(users[3], BuddyStatus.FRIEND),
        RideBuddy(users[4], BuddyStatus.PENDING_RECEIVED),
        RideBuddy(users[5], BuddyStatus.NOT_CONNECTED),
    )

    val rideHistory = listOf(
        RideHistoryEntry("r-1", "Sudirman Sunday Loop", VehicleType.MOTORCYCLE, 18_400.0, 2_700, 24.5, R.drawable.img_road_1, listOf(R.drawable.img_road_2), 1_752_000_000,
            rideScore = RideScore(72, "B", 78, 70, 68), segmentResult = SegmentResult("Sudirman Sprint", 209, 3)),
        RideHistoryEntry("r-2", "Weekend Car Meet", VehicleType.CAR, 42_000.0, 5_400, 28.0, R.drawable.img_road_3, listOf(R.drawable.img_road_4, R.drawable.img_road_5), 1_752_400_000,
            rideScore = RideScore(61, "C", 55, 74, 52), segmentResult = SegmentResult("Thamrin Flow", 281, 2)),
        RideHistoryEntry("r-3", "Night Ride to Puncak", VehicleType.MOTORCYCLE, 65_000.0, 9_000, 26.0, R.drawable.img_road_6, emptyList(), 1_752_800_000,
            rideScore = RideScore(84, "B", 92, 66, 94), segmentResult = SegmentResult("Puncak Pass", 505, 2)),
        RideHistoryEntry("r-4", "Kemang Evening Spin", VehicleType.MOTORCYCLE, 22_000.0, 3_300, 24.0, R.drawable.img_road_2, emptyList(), 1_751_395_200, rideScore = RideScore(74, "B", 76, 72, 74)),
        RideHistoryEntry("r-5", "Weekend Car Meet II", VehicleType.CAR, 55_000.0, 6_600, 30.0, R.drawable.img_road_3, emptyList(), 1_750_790_400, rideScore = RideScore(60, "C", 55, 72, 53)),
        RideHistoryEntry("r-6", "Sudirman Morning", VehicleType.MOTORCYCLE, 31_000.0, 4_200, 26.5, R.drawable.img_road_1, emptyList(), 1_750_185_600, rideScore = RideScore(78, "B", 82, 70, 82)),
        RideHistoryEntry("r-7", "Short City Hop", VehicleType.MOTORCYCLE, 12_000.0, 1_800, 24.0, R.drawable.img_road_4, emptyList(), 1_749_580_800, rideScore = RideScore(58, "C", 50, 70, 54)),
        RideHistoryEntry("r-8", "Bandung Day Trip", VehicleType.CAR, 88_000.0, 9_000, 35.0, R.drawable.img_road_5, emptyList(), 1_748_976_000, rideScore = RideScore(88, "A", 90, 80, 94)),
        RideHistoryEntry("r-9", "Puncak Loop", VehicleType.MOTORCYCLE, 44_000.0, 5_400, 29.0, R.drawable.img_road_6, emptyList(), 1_748_371_200, rideScore = RideScore(72, "B", 75, 70, 71)),
        RideHistoryEntry("r-10", "Sunday Sudirman", VehicleType.MOTORCYCLE, 18_500.0, 2_700, 24.7, R.drawable.img_road_1, emptyList(), 1_747_766_400, rideScore = RideScore(62, "C", 60, 72, 54)),
        RideHistoryEntry("r-11", "Suburb Errand Run", VehicleType.CAR, 27_000.0, 3_600, 27.0, R.drawable.img_road_2, emptyList(), 1_747_161_600, rideScore = RideScore(70, "B", 72, 70, 68)),
        RideHistoryEntry("r-12", "Long South Ride", VehicleType.MOTORCYCLE, 63_000.0, 8_100, 28.0, R.drawable.img_road_3, emptyList(), 1_746_556_800, rideScore = RideScore(85, "A", 88, 74, 93)),
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

    val segments = listOf(
        RoadSegment(
            "seg-1", "Sudirman Sprint", "Jakarta", 2.4, Twistiness.MELLOW, R.drawable.img_road_1,
            listOf(
                SegmentTime("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 182),
                SegmentTime("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 205),
                SegmentTime("u-3", "Bagas", R.drawable.ic_avatar_placeholder, 214),
                SegmentTime("u-4", "Sarah", R.drawable.ic_avatar_placeholder, 231),
            ),
        ),
        RoadSegment(
            "seg-2", "Puncak Pass", "Bogor", 8.1, Twistiness.TECHNICAL, R.drawable.img_road_6,
            listOf(
                SegmentTime("u-3", "Bagas", R.drawable.ic_avatar_placeholder, 498),
                SegmentTime("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 512),
                SegmentTime("u-5", "Yoga", R.drawable.ic_avatar_placeholder, 540),
            ),
        ),
        RoadSegment(
            "seg-3", "Thamrin Flow", "Jakarta", 3.2, Twistiness.FLOWING, R.drawable.img_road_3,
            listOf(
                SegmentTime("u-me", "Rafi", R.drawable.ic_avatar_placeholder, 268),
                SegmentTime("u-6", "Nadia", R.drawable.ic_avatar_placeholder, 275),
                SegmentTime("u-2", "Dinda", R.drawable.ic_avatar_placeholder, 290),
            ),
        ),
    )

    val scenicRoutes = listOf(
        ScenicRoute(
            "sc-1", "Puncak Pass Run", "Bogor", 38.0, 90,
            listOf(ScenicVibe.MOUNTAIN, ScenicVibe.FOREST), R.drawable.img_road_6,
            "Switchbacks and tea-plantation views up to the pass. Cool air, best early morning before traffic.",
            listOf(GeoPoint(-6.70, 106.90), GeoPoint(-6.66, 106.95), GeoPoint(-6.62, 107.00)),
        ),
        ScenicRoute(
            "sc-2", "South Coast Cruise", "Sukabumi", 64.0, 140,
            listOf(ScenicVibe.COASTAL), R.drawable.img_road_2,
            "Long sweeping bends along the Indian Ocean cliffs. Open throttle, big horizons.",
            listOf(GeoPoint(-7.02, 106.55), GeoPoint(-7.05, 106.62), GeoPoint(-7.08, 106.70)),
        ),
        ScenicRoute(
            "sc-3", "Sudirman City Loop", "Jakarta", 12.0, 35,
            listOf(ScenicVibe.URBAN), R.drawable.img_road_1,
            "A quick after-dark loop through the CBD — lit towers, smooth tarmac, easy pace.",
            listOf(GeoPoint(-6.2246, 106.8091), GeoPoint(-6.2088, 106.8206), GeoPoint(-6.1875, 106.8271)),
        ),
        ScenicRoute(
            "sc-4", "Pine Forest Traverse", "Bandung", 51.0, 120,
            listOf(ScenicVibe.FOREST, ScenicVibe.MOUNTAIN), R.drawable.img_road_4,
            "Dappled light through the pines and cool ridgeline curves. A rider's favourite Sunday.",
            listOf(GeoPoint(-6.85, 107.60), GeoPoint(-6.82, 107.66), GeoPoint(-6.80, 107.72)),
        ),
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
