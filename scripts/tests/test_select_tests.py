"""Selection contracts and real-Git change detection, without Gradle or Docker."""

import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "select-tests.py"
spec = importlib.util.spec_from_file_location("select_tests", SCRIPT)
selector = importlib.util.module_from_spec(spec)
spec.loader.exec_module(selector)


class SelectionTests(unittest.TestCase):
    def test_order_service_selects_existing_order_tests_and_concurrency(self):
        plan = selector.select([selector.JAVA + "order/OrderService.java"])
        self.assertEqual(plan["tests"], [selector.ORDER + "OrderPersistenceIntegrationTest",
                                         selector.ORDER + "OrderServiceTest"])
        self.assertTrue(plan["concurrency"])
        self.assertFalse(plan["full_backend"])
        self.assertFalse(plan["android"])
        for name in plan["tests"]:
            source = SCRIPT.parents[1] / "backend/src/test/java" / (name.replace(".", "/") + ".java")
            self.assertTrue(source.is_file(), name)

    def test_product_inventory_keeps_concurrency_and_optional_measurement_separate(self):
        plan = selector.select([selector.JAVA + "product/ProductRepository.java"])
        regular, optional = selector.commands(plan, "powershell")
        self.assertTrue(plan["concurrency"])
        self.assertTrue(any("concurrencyTest" in c for c in regular))
        self.assertFalse(any("performanceTest" in c for c in regular))
        self.assertTrue(any("ProductSearchIndexPerformanceTest" in c for c in optional))

    def test_android_and_documentation_are_independent(self):
        plan = selector.select(["android/app/src/main/java/com/vegan/Cart.java", "docs/notes.md"])
        self.assertEqual(selector.commands(plan, "powershell")[0],
                         [".\\android\\gradlew.bat -p android testDebugUnitTest"])
        self.assertEqual(selector.commands(selector.select(["docs/notes.md"]), "posix"), ([], []))

    def test_unknown_paths_and_configuration_use_full_regression(self):
        for path in ["backend/build.gradle", ".github/workflows/ci.yml",
                     selector.JAVA + "newfeature/NewService.java", "scripts/deploy.sh"]:
            with self.subTest(path=path):
                plan = selector.select([path])
                self.assertTrue(all(plan[k] for k in ("full_backend", "concurrency", "android")))

    def test_full_regression_supersedes_filters_and_deduplicates(self):
        path = selector.JAVA + "order/OrderService.java"
        plan = selector.select([path, path, "backend/build.gradle"])
        self.assertEqual(len(plan["changed_files"]), 2)
        self.assertEqual(plan["tests"], [])
        self.assertEqual(selector.commands(plan, "posix")[0][0], "bash ./backend/gradlew -p backend test")

    def test_performance_test_change_stays_optional(self):
        plan = selector.select(["backend/src/test/java/com/vegan/api/product/ProductCacheApiPerformanceTest.java"])
        regular, optional = selector.commands(plan, "posix")
        self.assertFalse(any("performanceTest" in c for c in regular))
        self.assertEqual(len(optional), 1)
        self.assertIn("ProductCacheApiPerformanceTest", optional[0])


class GitChangesTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        self.git("init", "-b", "main")
        self.git("config", "user.name", "Selector test")
        self.git("config", "user.email", "selector@example.invalid")
        for path in ("staged.txt", "unstaged.txt", "deleted.txt", "old name.txt"):
            (self.repo / path).write_text("baseline\n")
        (self.repo / ".gitignore").write_text("ignored.txt\n__pycache__/\n")
        self.git("add", ".")
        self.git("commit", "-m", "baseline")

    def git(self, *args):
        return selector.git(self.repo, *args)

    def test_staged_unstaged_untracked_deleted_renamed_and_ignored(self):
        (self.repo / "staged.txt").write_text("staged change\n")
        self.git("add", "staged.txt")
        (self.repo / "unstaged.txt").write_text("unstaged change\n")
        (self.repo / "deleted.txt").unlink()
        self.git("mv", "old name.txt", "new name.txt")
        (self.repo / "new 한글.txt").write_text("new\n")
        (self.repo / "ignored.txt").write_text("ignored\n")
        self.assertEqual(selector.changed_files(self.repo), sorted([
            "staged.txt", "unstaged.txt", "deleted.txt", "old name.txt", "new name.txt", "new 한글.txt"]))

    def test_merge_base_excludes_changes_only_on_target_branch(self):
        self.git("checkout", "-b", "feature")
        (self.repo / "feature.txt").write_text("feature\n")
        self.git("add", ".")
        self.git("commit", "-m", "feature")
        self.git("checkout", "main")
        (self.repo / "main-only.txt").write_text("target branch change\n")
        self.git("add", ".")
        self.git("commit", "-m", "advance main")
        self.git("checkout", "feature")
        (self.repo / "unstaged.txt").write_text("working change\n")
        self.assertEqual(selector.changed_files(self.repo), ["unstaged.txt"])
        self.assertEqual(selector.changed_files(self.repo, "main"), ["feature.txt", "unstaged.txt"])

    def test_clean_tree_and_invalid_ref(self):
        self.assertEqual(selector.changed_files(self.repo), [])
        with self.assertRaises(ValueError):
            selector.changed_files(self.repo, "missing-ref")

    def test_cli_is_read_only_and_works_from_subdirectory(self):
        scripts = self.repo / "scripts"
        scripts.mkdir()
        entry = scripts / "select-tests.py"
        entry.write_bytes(SCRIPT.read_bytes())
        before = self.git("status", "--porcelain", "-z")
        result = subprocess.run([sys.executable, str(entry), "--json", "--shell", "powershell"],
                                cwd=scripts, capture_output=True, check=True)
        plan = json.loads(result.stdout)
        self.assertEqual(plan["commands"], ["python -m unittest discover -s scripts/tests -v"])
        self.assertEqual(before, self.git("status", "--porcelain", "-z"))
        invalid = subprocess.run([sys.executable, str(entry), "--base", "missing-ref"],
                                 capture_output=True)
        self.assertEqual(invalid.returncode, 2)
        self.assertFalse(invalid.stdout)


if __name__ == "__main__":
    unittest.main()
