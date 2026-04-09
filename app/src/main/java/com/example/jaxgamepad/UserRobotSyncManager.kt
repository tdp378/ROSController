package com.example.jaxgamepad

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect


@Composable
fun RobotSyncLogic(
    signedInUser: com.google.firebase.auth.FirebaseUser?,
    robotManager: RobotManager,
    currentRobot: RobotConfig,
    onUpdateRobots: (List<RobotConfig>) -> Unit,
    onUpdateCurrentRobot: (RobotConfig) -> Unit
) {
    LaunchedEffect(signedInUser?.uid) {
        val ownerUid = signedInUser?.uid

        if (ownerUid.isNullOrBlank()) {
            val guestRobots = loadRobotsForOwner(robotManager, RobotManager.GUEST_OWNER_UID)
            onUpdateRobots(guestRobots)
            onUpdateCurrentRobot(
                guestRobots.firstOrNull { it.robotId == currentRobot.robotId }
                    ?: guestRobots.firstOrNull()
                    ?: buildDemoRobot(RobotManager.GUEST_OWNER_UID)
            )
            return@LaunchedEffect
        }

        fetchRobotsFromFirestoreForSignedInUser(
            uid = ownerUid,
            onResult = { cloudRobots ->
                val cloudBase = cloudRobots
                    .filterNot { it.isDemoRobot() }
                    .associateBy { it.robotId }
                    .toMutableMap()

                val guestRobots = robotManager.loadRobots(RobotManager.GUEST_OWNER_UID)
                    .filterNot { it.isDemoRobot() }

                guestRobots.forEach { guestRobot ->
                    val migrated = guestRobot.copy(ownerUid = ownerUid)
                    val duplicateCloud = cloudBase.values.any {
                        it.name.equals(migrated.name, ignoreCase = true)
                    }
                    if (!cloudBase.containsKey(migrated.robotId) && !duplicateCloud) {
                        cloudBase[migrated.robotId] = migrated
                    }
                }

                val merged = cloudBase.values.toList()
                val updatedRobots = if (merged.isEmpty()) {
                    loadRobotsForOwner(robotManager, ownerUid)
                } else {
                    val withDemo = if (merged.any { it.isDemoRobot() }) merged else listOf(buildDemoRobot(ownerUid)) + merged
                    robotManager.saveRobots(withDemo, ownerUid)
                    robotManager.clearOwner(RobotManager.GUEST_OWNER_UID)
                    syncRobotsToFirestoreForSignedInUser(withDemo)
                    withDemo
                }

                onUpdateRobots(updatedRobots)
                onUpdateCurrentRobot(
                    updatedRobots.firstOrNull { it.robotId == currentRobot.robotId }
                        ?: updatedRobots.firstOrNull()
                        ?: buildDemoRobot(ownerUid)
                )
            },
            onFailure = {
                val merged = robotManager.mergeGuestRobotsIntoOwner(ownerUid)
                val withDemo = merged.ifEmpty { loadRobotsForOwner(robotManager, ownerUid) }
                syncRobotsToFirestoreForSignedInUser(withDemo)
                onUpdateRobots(withDemo)
                onUpdateCurrentRobot(
                    withDemo.firstOrNull { it.robotId == currentRobot.robotId }
                        ?: withDemo.firstOrNull()
                        ?: buildDemoRobot(ownerUid)
                )
            }
        )
    }
}