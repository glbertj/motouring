package com.valid.motouring.di

import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.ChallengeRepository
import com.valid.motouring.data.repository.MaintenanceRepository
import com.valid.motouring.data.repository.NotificationRepository
import com.valid.motouring.data.repository.PoiRepository
import com.valid.motouring.data.repository.PostRepository
import com.valid.motouring.data.repository.RideBuddyRepository
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.data.repository.ScenicRouteRepository
import com.valid.motouring.data.repository.SegmentRepository
import com.valid.motouring.data.repository.UserRepository
import com.valid.motouring.data.repository.VehicleRepository

class AppContainer {
    val userRepository = UserRepository()
    val vehicleRepository = VehicleRepository()
    val rideBuddyRepository = RideBuddyRepository()
    val rideRepository = RideRepository()
    val challengeRepository = ChallengeRepository()
    val badgeRepository = BadgeRepository()
    val poiRepository = PoiRepository()
    val postRepository = PostRepository()
    val notificationRepository = NotificationRepository()
    val maintenanceRepository = MaintenanceRepository()
    val segmentRepository = SegmentRepository()
    val scenicRouteRepository = ScenicRouteRepository()
}
