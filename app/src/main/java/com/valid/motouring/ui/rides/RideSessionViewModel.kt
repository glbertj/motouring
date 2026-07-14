package com.valid.motouring.ui.rides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.R
import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.GroupSignal
import com.valid.motouring.data.model.Notification
import com.valid.motouring.data.model.NotificationType
import com.valid.motouring.data.model.RideGoal
import com.valid.motouring.data.model.RideMode
import com.valid.motouring.data.model.RideParticipantState
import com.valid.motouring.data.model.RideSession
import com.valid.motouring.data.model.RideSessionEvent
import com.valid.motouring.data.model.RideSessionStatus
import com.valid.motouring.data.model.RiderRole
import com.valid.motouring.data.model.SafetyAlert
import com.valid.motouring.data.model.SafetyAlertType
import com.valid.motouring.data.model.VehicleType
import com.valid.motouring.data.repository.BadgeRepository
import com.valid.motouring.data.repository.NotificationRepository
import com.valid.motouring.data.repository.PoiRepository
import com.valid.motouring.data.repository.RideBuddyRepository
import com.valid.motouring.data.repository.RideRepository
import com.valid.motouring.data.repository.UserRepository
import com.valid.motouring.simulation.RideSimulator
import com.valid.motouring.simulation.assignInitialRoles
import com.valid.motouring.simulation.buildSafetyAlert
import com.valid.motouring.simulation.explorerBadgeEarned
import com.valid.motouring.simulation.nearestGasStation
import com.valid.motouring.simulation.neverEndingBadgeEarned
import com.valid.motouring.simulation.toHistoryEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RideSessionViewModel(
    initialSession: RideSession,
    private val rideRepository: RideRepository,
    private val badgeRepository: BadgeRepository,
    private val poiRepository: PoiRepository,
    private val rideBuddyRepository: RideBuddyRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val simulator = RideSimulator(viewModelScope, initialSession)

    val session: StateFlow<RideSession> = simulator.session
    val events: SharedFlow<RideSessionEvent> = simulator.events

    private val _activeAlert = MutableStateFlow<SafetyAlert?>(null)
    val activeAlert: StateFlow<SafetyAlert?> = _activeAlert.asStateFlow()

    init {
        simulator.start()
    }

    fun pickGoal(goal: RideGoal) = simulator.setGoal(goal)

    fun simulateDrift() = simulator.simulateDrift()

    fun setRole(userId: String, role: RiderRole) = simulator.setRole(userId, role)

    fun broadcastRegroup() = simulator.broadcastRegroup()

    fun forceSweepBehind() = simulator.forceSweepBehind()

    fun callFuel() {
        val self = simulator.session.value.participants.firstOrNull()
        val nearest = self?.let { nearestGasStation(poiRepository.observePois().value, it.position) }
        simulator.callFuel(nearest)
    }

    fun triggerSos() = raiseSelfAlert(SafetyAlertType.SOS)

    fun confirmCrashAlert() = raiseSelfAlert(SafetyAlertType.CRASH)

    fun simulateHardStop() = simulator.simulateHardStop()

    fun simulateRiderInTrouble() = simulator.simulateRiderInTrouble()

    fun raiseInTroubleAlert(participant: RideParticipantState) {
        val current = _activeAlert.value
        if (current != null && (current.type == SafetyAlertType.SOS || current.type == SafetyAlertType.CRASH)) return
        val alert = buildSafetyAlert(
            id = "sa-${System.currentTimeMillis()}",
            type = SafetyAlertType.RIDER_IN_TROUBLE,
            fromUserId = participant.userId,
            fromName = participant.name,
            trustedContactNames = trustedNames(),
            startedAtSeconds = System.currentTimeMillis() / 1000,
        )
        _activeAlert.value = alert
    }

    fun resolveActiveAlert() {
        _activeAlert.value = null
    }

    private fun raiseSelfAlert(type: SafetyAlertType) {
        val self = simulator.session.value.participants.firstOrNull() ?: return
        val contacts = trustedNames()
        val alert = buildSafetyAlert(
            id = "sa-${System.currentTimeMillis()}",
            type = type,
            fromUserId = self.userId,
            fromName = self.name,
            trustedContactNames = contacts,
            startedAtSeconds = System.currentTimeMillis() / 1000,
        )
        _activeAlert.value = alert
        val label = if (type == SafetyAlertType.CRASH) "Crash alert" else "SOS"
        val to = if (contacts.isEmpty()) "your group" else contacts.joinToString(", ")
        notificationRepository.add(
            Notification(
                id = alert.id,
                type = NotificationType.SAFETY,
                message = "$label sent to $to",
                createdAtEpochSeconds = alert.startedAtSeconds,
                isRead = false,
            ),
        )
        // Simulate a contact acknowledging so the state feels two-way.
        viewModelScope.launch {
            delay(3_500)
            val current = _activeAlert.value
            if (current?.id == alert.id && contacts.isNotEmpty()) {
                _activeAlert.value = current.copy(respondingContactName = contacts.first())
            }
        }
    }

    private fun trustedNames(): List<String> = rideBuddyRepository.trustedContacts().map { it.user.name }

    fun endRide(): String {
        simulator.stop()
        val finalSession = simulator.session.value
        val completedAt = System.currentTimeMillis() / 1000
        val entry = finalSession.toHistoryEntry(
            id = "r-${System.currentTimeMillis()}",
            completedAtEpochSeconds = completedAt,
            routePreviewRes = R.drawable.ic_route_preview_placeholder,
        )
        rideRepository.addHistoryEntry(entry)
        if (explorerBadgeEarned(entry.legs)) {
            badgeRepository.markEarned("b-7", completedAt)
        }
        if (neverEndingBadgeEarned(entry.legs)) {
            badgeRepository.markEarned("b-8", completedAt)
        }
        return entry.id
    }

    companion object {
        fun factory(
            vehicleType: VehicleType,
            isGroup: Boolean,
            initialGoal: RideGoal?,
            userRepository: UserRepository,
            rideBuddyRepository: RideBuddyRepository,
            rideRepository: RideRepository,
            badgeRepository: BadgeRepository,
            poiRepository: PoiRepository,
            notificationRepository: NotificationRepository,
        ) = viewModelFactory {
            initializer {
                val currentUser = userRepository.currentUser()
                val route = FakeDataProvider.sampleRoute
                val rawParticipants = buildList {
                    add(
                        RideParticipantState(
                            userId = currentUser.id,
                            name = currentUser.name,
                            avatarRes = currentUser.avatarRes,
                            position = route.first(),
                        ),
                    )
                    if (isGroup) {
                        rideBuddyRepository.friends().forEach { buddy ->
                            add(
                                RideParticipantState(
                                    userId = buddy.user.id,
                                    name = buddy.user.name,
                                    avatarRes = buddy.user.avatarRes,
                                    position = route.first(),
                                ),
                            )
                        }
                    }
                }
                val participants = assignInitialRoles(rawParticipants)
                val initialSession = RideSession(
                    id = "rs-${System.currentTimeMillis()}",
                    vehicleType = vehicleType,
                    route = route,
                    participants = participants,
                    distanceMeters = 0.0,
                    speedKmh = 0.0,
                    elapsedSeconds = 0,
                    status = RideSessionStatus.ACTIVE,
                    mode = if (initialGoal != null) RideMode.GOAL else RideMode.ENDLESS,
                    activeGoal = initialGoal,
                    completedLegs = emptyList(),
                )
                RideSessionViewModel(initialSession, rideRepository, badgeRepository, poiRepository, rideBuddyRepository, notificationRepository)
            }
        }
    }
}
