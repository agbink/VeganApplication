"""Selection contracts and real-Git change detection, without Gradle or Docker."""

import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


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


class RunnerTests(unittest.TestCase):
    def test_windows_wrapper_keeps_paths_and_class_filters_as_arguments(self):
        repo = Path("repo with spaces")
        args = selector.execution_args(repo, "backend", "test", [selector.ORDER + "OrderServiceTest"],
                                       windows=True)
        self.assertEqual(args, [str(repo / "backend/gradlew.bat"), "-p", "backend", "test",
                                "--tests", selector.ORDER + "OrderServiceTest"])

    def test_python_check_uses_the_current_interpreter(self):
        self.assertEqual(selector.execution_args(Path("."), "python", "unittest", [])[0],
                         sys.executable)

    def test_missing_executable_and_interrupt_do_not_report_success(self):
        plan = selector.select(["android/app/src/main/Example.java"])
        for error, code in [(FileNotFoundError("missing wrapper"), 1), (KeyboardInterrupt(), 130)]:
            with self.subTest(error=type(error).__name__), patch.object(selector.subprocess, "run", side_effect=error):
                self.assertEqual(selector.run_selected(plan, Path(".")), code)


class RunnerCliTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="test runner space ")
        self.addCleanup(self.temp.cleanup)
        self.repo = Path(self.temp.name)
        for args in [("init", "-b", "main"), ("config", "user.name", "Runner test"),
                     ("config", "user.email", "runner@example.invalid")]:
            selector.git(self.repo, *args)
        self.entry = self.repo / "scripts/select-tests.py"
        self.entry.parent.mkdir()
        self.entry.write_bytes(SCRIPT.read_bytes())
        (self.repo / ".gitignore").write_text("executed.txt\n")
        for project in ("backend", "android"):
            (self.repo / project).mkdir()
            self.wrapper(project, 0)
        selector.git(self.repo, "add", ".")
        selector.git(self.repo, "commit", "-m", "baseline")

    def wrapper(self, project, code):
        (self.repo / project / "gradlew").write_text(
            'printf "%s\\n" "$*" >> executed.txt\n' + f"exit {code}\n")
        (self.repo / project / "gradlew.bat").write_text(
            "@echo off\necho %*>>executed.txt\n" + f"exit /b {code}\n")

    def cli(self, *args):
        # These child processes run only fixture wrappers, never real Gradle.
        return subprocess.run([sys.executable, str(self.entry), *args], cwd=self.entry.parent,
                              capture_output=True)

    def test_default_remains_read_only_and_run_executes_selected_tasks(self):
        source = self.repo / (selector.JAVA + "product/ProductRepository.java")
        source.parent.mkdir(parents=True)
        source.write_text("changed\n")
        self.assertEqual(self.cli().returncode, 0)
        self.assertFalse((self.repo / "executed.txt").exists())
        result = self.cli("--run")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        lines = (self.repo / "executed.txt").read_text().splitlines()
        self.assertEqual(len(lines), 2)
        self.assertIn("OrderServiceTest", lines[0])
        self.assertIn("OrderPersistenceIntegrationTest", lines[0])
        self.assertIn("concurrencyTest", lines[1])
        self.assertFalse(any("performanceTest" in line for line in lines))

    def test_failure_stops_later_tasks_and_never_runs_performance(self):
        self.wrapper("backend", 7)
        source = self.repo / (selector.JAVA + "product/ProductRepository.java")
        source.parent.mkdir(parents=True)
        source.write_text("changed\n")
        result = self.cli("--run")
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        lines = (self.repo / "executed.txt").read_text().splitlines()
        self.assertEqual(len(lines), 1)
        self.assertIn("-p backend test", lines[0])
        self.assertNotIn("performanceTest", lines[0])
        self.assertIn(b"7", result.stdout)

    def test_clean_or_document_only_change_runs_nothing(self):
        for document in (False, True):
            with self.subTest(document=document):
                if document:
                    (self.repo / "docs").mkdir()
                    (self.repo / "docs/note.md").write_text("documentation\n")
                self.assertEqual(self.cli("--run").returncode, 0)
                self.assertFalse((self.repo / "executed.txt").exists())

    def test_json_run_conflict_or_invalid_base_runs_nothing(self):
        for args in [("--json", "--run"), ("--base", "missing-ref", "--run")]:
            with self.subTest(args=args):
                self.assertEqual(self.cli(*args).returncode, 2)
                self.assertFalse((self.repo / "executed.txt").exists())


if __name__ == "__main__":
    unittest.main()
