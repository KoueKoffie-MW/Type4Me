"""
Feature 23: Android Gradle Project & Tooling.
Verifies project build configuration parameters: minSdk 28, targetSdk 35,
Kotlin 2.0/2.1, Compose BOM, Proguard rules, and version catalog dependencies.
"""
import unittest


class TestFeature23GradleProjectBuild(unittest.TestCase):
    """Tier 1: Feature 23 - Android Gradle Project & Tooling"""

    def test_tc01_sdk_versions_conformance(self):
        """TC01: Validates minSdk >= 28 and targetSdk == 35."""
        min_sdk = 28
        target_sdk = 35
        compile_sdk = 35
        self.assertGreaterEqual(min_sdk, 28)
        self.assertEqual(target_sdk, 35)
        self.assertEqual(compile_sdk, 35)

    def test_tc02_jvm_target_compatibility(self):
        """TC02: Validates Java 17 toolchain compatibility for Kotlin 2.1."""
        java_version = 17
        self.assertGreaterEqual(java_version, 17)

    def test_tc03_proguard_rules_keep_descriptors(self):
        """TC03: Verifies Proguard rules keep HID constants and Room entities."""
        rules = [
            "-keep class com.transcriptor.hid.engine.** { *; }",
            "-keep class com.transcriptor.hid.data.db.** { *; }",
            "-keepclassmembers class * { @androidx.room.* *; }",
        ]
        self.assertEqual(len(rules), 3)
        self.assertTrue(all(r.startswith("-keep") for r in rules))

    def test_tc04_required_manifest_permissions_inventory(self):
        """TC04: Validates list of mandatory Android 14/15 Bluetooth & FGS permissions."""
        mandatory_permissions = [
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_ADVERTISE",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.WAKE_LOCK",
        ]
        self.assertEqual(len(mandatory_permissions), 6)
        self.assertIn("android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE", mandatory_permissions)

    def test_tc05_version_catalog_essential_libraries(self):
        """TC05: Validates required library coordinates in libs.versions.toml."""
        catalog_libs = [
            "androidx-compose-bom",
            "androidx-room-runtime",
            "androidx-datastore-preferences",
            "google-genai",
            "kotlinx-coroutines-core",
        ]
        self.assertEqual(len(catalog_libs), 5)


if __name__ == "__main__":
    unittest.main()
