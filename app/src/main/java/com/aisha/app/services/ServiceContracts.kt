package com.aisha.app.services

/**
 * LOCKED §20 — Background services. Android implementation via WorkManager +
 * foreground services (implementation phase). Contracts locked here:
 *
 *  - DayFinalizationService: midnight → finalize Day Log (§6 lifecycle)
 *  - SyncService: drain upload/download queues when connectivity returns
 *  - NotificationService: reminder/notification delivery (respect policy §10)
 *  - BatteryManager: battery-aware throttling of avatar/wallpaper/sync (§12)
 *  - UpdateManager: DB migration preserving user data on app update (§21)
 *  - CrashRecoveryService: recover local DB and queues after restart (§21)
 *  - StorageManager: storage thresholds, retention lifecycle (§17)
 *  - ConnectivityMonitor: offline-first trigger point (§21)
 *
 * Failure contract (§21): local features continue when offline; cloud ops
 * queue; AI-service failure → preserve state + degraded local behaviour +
 * inform user; corrupt backup → validate, never blind-restore.
 */
interface ServiceContractsMarker
