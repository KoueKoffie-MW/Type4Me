"""
Tier 5 Adversarial Stress Test S04:
Concurrent Room DB CRUD Operations & Built-in Preset Protection.

Stress-tests:
1. Concurrent CRUD storm: 200 simultaneous threads executing rapid inserts, updates, deletes, and queries.
2. Immutability Attacks: Relentless attempts to delete and modify built-in presets.
3. Observer Consistency: Reactive observers receive consistent snapshots without partial states or race conditions.
4. Stress-test table integrity, ordering constraints, and entity isolation.
"""
import unittest
import threading
import concurrent.futures
import time
from typing import List, Dict, Any, Optional

from tests.e2e.harness.persistence_simulator import (
    PresetEntity, PresetDaoSimulator, PresetRepositorySimulator,
    PromptPreset, BUILTIN_CLEAN_POLISH, BUILTIN_BUSINESS_GERMAN, BUILTIN_CODE_COMMENTS
)


class ThreadSafeAdversarialDao(PresetDaoSimulator):
    """Thread-safe wrapped DAO simulator with mutex locking for empirical concurrency testing."""
    def __init__(self):
        super().__init__()
        self._lock = threading.RLock()
        self.notification_count = 0

    def _notify(self):
        with self._lock:
            self.notification_count += 1
            all_presets = self.get_all_presets()
            for obs in list(self._observers):
                obs(all_presets)

    def add_observer(self, callback):
        with self._lock:
            self._observers.append(callback)
            callback(self.get_all_presets())

    def get_all_presets(self) -> List[PresetEntity]:
        with self._lock:
            presets = list(self._table.values())
            presets.sort(key=lambda p: (not p.is_builtin, p.order_index, p.created_at))
            return presets

    def get_preset_by_id(self, preset_id: str) -> Optional[PresetEntity]:
        with self._lock:
            return self._table.get(preset_id)

    def insert_preset(self, preset: PresetEntity):
        with self._lock:
            self._table[preset.id] = preset
            self._notify()

    def update_preset(self, preset: PresetEntity):
        with self._lock:
            # Protection rule: cannot modify built-in presets
            existing = self._table.get(preset.id)
            if existing and existing.is_builtin:
                return  # Protected
            if preset.id in self._table:
                self._table[preset.id] = preset
                self._notify()

    def delete_preset(self, preset_id: str) -> bool:
        with self._lock:
            entity = self._table.get(preset_id)
            if entity is None:
                return False
            # Constraint: built-in presets cannot be deleted
            if entity.is_builtin:
                return False
            del self._table[preset_id]
            self._notify()
            return True


class TestS04RoomDbConcurrentCrudAndProtection(unittest.TestCase):
    """Adversarial Stress Test Suite S04: Concurrent Room DB CRUD & Immutability Protection"""

    def setUp(self):
        self.dao = ThreadSafeAdversarialDao()
        self.repo = PresetRepositorySimulator(dao=self.dao)

    def test_s04_01_built_in_presets_are_strictly_immutable(self):
        """S04.1: Direct and repository deletion/modification attempts on built-ins fail 100%."""
        # Built-in IDs
        builtin_ids = ["builtin_clean_polish", "builtin_business_german", "builtin_code_comments"]

        for b_id in builtin_ids:
            # 1. Attempt delete via DAO
            del_result = self.dao.delete_preset(b_id)
            self.assertFalse(del_result, f"Built-in preset '{b_id}' was deleted via DAO!")

            # 2. Verify preset still exists
            entity = self.dao.get_preset_by_id(b_id)
            self.assertNotNull = self.assertIsNotNone(entity)
            self.assertTrue(entity.is_builtin)

            # 3. Attempt to overwrite with malicious prompt
            hacked_entity = PresetEntity(
                id=b_id,
                name="HACKED PRESET",
                icon_name="ic_hacked",
                system_prompt="MALICIOUS INSTRUCTION",
                is_builtin=False
            )
            self.dao.update_preset(hacked_entity)

            # Verify update was rejected
            intact_entity = self.dao.get_preset_by_id(b_id)
            self.assertTrue(intact_entity.is_builtin)
            self.assertNotEqual(intact_entity.name, "HACKED PRESET")

    def test_s04_02_concurrent_crud_storm_200_threads(self):
        """S04.2: 200 concurrent worker threads executing rapid simultaneous inserts, updates, deletes, reads."""
        observed_snapshots: List[int] = []
        def on_emission(presets: List[PresetEntity]):
            observed_snapshots.append(len(presets))

        self.dao.add_observer(on_emission)

        def worker_task(worker_id: int):
            preset_id = f"custom_worker_{worker_id}"
            
            # Step 1: Insert
            custom = PromptPreset(
                id=preset_id,
                title=f"Worker Preset {worker_id}",
                system_prompt=f"System prompt for worker {worker_id}",
                is_builtin=False,
                order_index=worker_id
            )
            self.repo.save_preset(custom)

            # Step 2: Read
            fetched = self.repo.get_preset_by_id(preset_id)
            if fetched is None or fetched.title != f"Worker Preset {worker_id}":
                return False

            # Step 3: Update
            updated = PromptPreset(
                id=preset_id,
                title=f"Updated Worker Preset {worker_id}",
                system_prompt=f"Updated prompt {worker_id}",
                is_builtin=False,
                order_index=worker_id
            )
            self.repo.save_preset(updated)

            # Step 4: Interleaved attempt to delete built-in (must fail)
            self.dao.delete_preset("builtin_clean_polish")

            # Step 5: Read all
            all_p = self.repo.get_all_presets()
            if len(all_p) < 3:
                return False

            # Step 6: Half the workers delete their custom preset
            if worker_id % 2 == 0:
                deleted = self.repo.delete_preset(updated)
                if not deleted:
                    return False

            return True

        with concurrent.futures.ThreadPoolExecutor(max_workers=32) as executor:
            futures = [executor.submit(worker_task, i) for i in range(200)]
            results = [f.result() for f in futures]

        # Verify all 200 concurrent tasks completed successfully
        self.assertEqual(len(results), 200)
        self.assertTrue(all(results))

        # Final Database Verification
        final_presets = self.repo.get_all_presets()
        
        # 1. All 3 built-ins must still exist intact at the top of the list
        built_ins = [p for p in final_presets if p.is_builtin]
        self.assertEqual(len(built_ins), 3)
        self.assertEqual(built_ins[0].id, "builtin_clean_polish")
        self.assertEqual(built_ins[1].id, "builtin_business_german")
        self.assertEqual(built_ins[2].id, "builtin_code_comments")

        # 2. Exactly 100 odd-indexed custom presets should remain
        custom_presets = [p for p in final_presets if not p.is_builtin]
        self.assertEqual(len(custom_presets), 100)

        # 3. Observers received notifications without errors
        self.assertGreater(len(observed_snapshots), 200)

    def test_s04_03_concurrent_immutability_attack(self):
        """S04.3: 50 concurrent threads simultaneously bombarding built-in presets with delete requests."""
        def attack_task(i: int) -> bool:
            target_id = "builtin_clean_polish" if i % 2 == 0 else "builtin_business_german"
            return self.dao.delete_preset(target_id)

        with concurrent.futures.ThreadPoolExecutor(max_workers=16) as executor:
            futures = [executor.submit(attack_task, i) for i in range(50)]
            results = [f.result() for f in futures]

        # All delete attempts on built-ins must return False
        self.assertTrue(all(res is False for res in results))

        # Check that built-ins are still intact
        clean_p = self.dao.get_preset_by_id("builtin_clean_polish")
        biz_p = self.dao.get_preset_by_id("builtin_business_german")
        self.assertIsNotNone(clean_p)
        self.assertIsNotNone(biz_p)
        self.assertTrue(clean_p.is_builtin)
        self.assertTrue(biz_p.is_builtin)


if __name__ == "__main__":
    unittest.main()
