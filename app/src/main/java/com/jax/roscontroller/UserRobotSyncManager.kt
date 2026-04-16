package com.jax.roscontroller

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
                    .associateBy { it.robotId }
                    .toMutableMap()

                // Rule: If local storage has a demo robot, we MUST keep it in the merged list.
                // Cloud robots don't contain the demo. If it's missing from cloudBase, 
                // but present in local, it stays.
                val localRobots = robotManager.loadRobots(ownerUid) ?: emptyList()
                localRobots.filter { it.isDemoRobot() }.forEach {
                    cloudBase[it.robotId] = it
                }

                val guestRobots = (robotManager.loadRobots(RobotManager.GUEST_OWNER_UID) ?: emptyList())
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
                    // Only save what we have from cloud/migration. 
                    // No longer forcing the Demo robot if other robots exist.
                    robotManager.saveRobots(merged, ownerUid)
                    robotManager.clearOwner(RobotManager.GUEST_OWNER_UID)
                    syncRobotsToFirestoreForSignedInUser(merged)
                    merged
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
                syncRobotsToFirestoreForSignedInUser(merged)
                onUpdateRobots(merged)
                onUpdateCurrentRobot(
                    merged.firstOrNull { it.robotId == currentRobot.robotId }
                        ?: merged.firstOrNull()
                        ?: buildDemoRobot(ownerUid)
                )
            }
        )
    }
}
