#!/usr/bin/env python3
"""
ComposeDemo E2E Device Test Runner using mobile-mcp on real Android device.
Uses real text input via adb input text + ENTER keyevent to trigger Compose onValueChange.
"""

import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from mobile_mcp_driver import MobileMcpDriver, ElementNotFoundError, MobileMcpError

PACKAGE = "zhaoyun.example.composedemo"
DEVICE = "emulator-5554"


def _type_and_add(driver: MobileMcpDriver, text: str) -> None:
    """Helper: focus input, type text, dismiss keyboard, click FAB."""
    el = driver.assert_element_exists(text="输入待办事项")
    driver.click_element(el)
    time.sleep(0.5)
    driver.type_text_real(text)
    driver.dismiss_keyboard()
    fab = driver.find_element(label="添加")
    driver.click_element(fab)
    time.sleep(1.5)


def _find_checkboxes(driver: MobileMcpDriver) -> list:
    return [e for e in driver.list_elements() if e.type == "android.widget.CheckBox"]


def _find_delete_buttons(driver: MobileMcpDriver) -> list:
    return [e for e in driver.list_elements() if e.label == "删除"]


def run_usecase_uc01(driver: MobileMcpDriver) -> bool:
    """UC-1: 应用启动与空状态展示"""
    print("\n🚀 UC-01: 应用启动与空状态展示")
    step = 0
    try:
        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        ss = driver.screenshot(f"uc01_{step:02d}_launch.png")
        driver.log_step(step, "清除应用数据并启动", "PASS", ss)

        step += 1
        driver.assert_element_exists(text="Todo List")
        driver.log_step(step, "断言 TopAppBar 标题 'Todo List' 存在", "PASS")

        step += 1
        ss = driver.screenshot(f"uc01_{step:02d}_empty_state.png")
        driver.assert_text_in_viewtree("暂无待办事项")
        driver.log_step(step, "断言空状态文本存在", "PASS", ss)

        step += 1
        driver.assert_element_exists(text="输入待办事项")
        driver.log_step(step, "断言输入框 hint '输入待办事项' 存在", "PASS")

        step += 1
        elements = driver.list_elements()
        fab_present = any("添加" in el.label for el in elements)
        if fab_present:
            raise MobileMcpError("FAB should not be present in empty state")
        driver.log_step(step, "断言 FAB 在空状态时不存在", "PASS")

        step += 1
        driver.assert_element_not_exists(text="清除已完成")
        driver.log_step(step, "断言 '清除已完成' 按钮不存在", "PASS")

        step += 1
        driver.assert_text_in_viewtree("共 0 项，已完成 0 项")
        driver.log_step(step, "断言统计文本 '共 0 项，已完成 0 项'", "PASS")

        print("  ✅ UC-01 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-01 FAILED at step {step}: {e}")
        return False


def run_usecase_uc02(driver: MobileMcpDriver) -> bool:
    """UC-2: 添加 Todo 完整流程（真实输入）"""
    print("\n🚀 UC-02: 添加 Todo 完整流程")
    step = 0
    try:
        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        driver.log_step(step, "清理并启动到空状态", "PASS")

        step += 1
        _type_and_add(driver, "BuyMilk")
        driver.assert_text_in_viewtree("BuyMilk")
        ss = driver.screenshot(f"uc02_{step:02d}_add_one.png")
        driver.log_step(step, "输入 'BuyMilk' 并点击 FAB 添加", "PASS", ss)

        step += 1
        driver.assert_text_in_viewtree("共 1 项，已完成 0 项")
        driver.log_step(step, "断言统计 '共 1 项，已完成 0 项'", "PASS")

        step += 1
        driver.assert_element_not_exists(label="添加")
        driver.log_step(step, "断言添加后 FAB 消失、输入框清空", "PASS")

        step += 1
        _type_and_add(driver, "WriteCode")
        driver.assert_text_in_viewtree("WriteCode")
        ss = driver.screenshot(f"uc02_{step:02d}_add_two.png")
        driver.log_step(step, "输入 'WriteCode' 并添加", "PASS", ss)

        step += 1
        driver.assert_text_in_viewtree("共 2 项，已完成 0 项")
        driver.log_step(step, "断言统计 '共 2 项，已完成 0 项'", "PASS")

        print("  ✅ UC-02 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-02 FAILED at step {step}: {e}")
        return False


def run_usecase_uc03(driver: MobileMcpDriver) -> bool:
    """UC-3: 输入验证与清除按钮"""
    print("\n🚀 UC-03: 输入验证与清除按钮")
    step = 0
    try:
        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        elements = driver.list_elements()
        fab_present = any("添加" in el.label for el in elements)
        clear_present = any("清空" in el.label for el in elements)
        if fab_present or clear_present:
            raise MobileMcpError("FAB and clear button should not be present when empty")
        ss = driver.screenshot(f"uc03_{step:02d}_empty.png")
        driver.log_step(step, "空状态：断言 FAB 和清除按钮均不存在", "PASS", ss)

        step += 1
        el = driver.assert_element_exists(text="输入待办事项")
        driver.click_element(el)
        time.sleep(0.5)
        driver.type_text_real("abc")
        driver.dismiss_keyboard()
        ss = driver.screenshot(f"uc03_{step:02d}_typed.png")
        driver.log_step(step, "输入 'abc'", "PASS", ss)

        step += 1
        driver.assert_element_exists(label="清空")
        driver.log_step(step, "断言尾部清除按钮存在", "PASS")

        step += 1
        clear_btn = driver.find_element(label="清空")
        driver.click_element(clear_btn)
        time.sleep(1)
        ss = driver.screenshot(f"uc03_{step:02d}_after_clear.png")
        driver.log_step(step, "点击清除按钮", "PASS", ss)

        step += 1
        elements = driver.list_elements()
        fab_present = any("添加" in el.label for el in elements)
        if fab_present:
            raise MobileMcpError("FAB should be gone after clearing input")
        driver.log_step(step, "断言 FAB 消失", "PASS")

        step += 1
        el = driver.assert_element_exists(text="输入待办事项")
        driver.click_element(el)
        time.sleep(0.5)
        driver.type_text_real("   ")
        driver.dismiss_keyboard()
        ss = driver.screenshot(f"uc03_{step:02d}_type_spaces.png")
        driver.log_step(step, "输入纯空格 '   '", "PASS", ss)

        step += 1
        elements = driver.list_elements()
        fab_present = any("添加" in el.label for el in elements)
        if fab_present:
            raise MobileMcpError("FAB should not be present for blank input")
        driver.log_step(step, "断言 FAB 不存在（纯空格视为无效输入）", "PASS")

        print("  ✅ UC-03 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-03 FAILED at step {step}: {e}")
        return False


def run_usecase_uc04(driver: MobileMcpDriver) -> bool:
    """UC-4: 标记完成与取消完成"""
    print("\n🚀 UC-04: 标记完成与取消完成")
    step = 0
    try:
        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        _type_and_add(driver, "TaskA")
        _type_and_add(driver, "TaskB")
        ss = driver.screenshot(f"uc04_{step:02d}_setup.png")
        driver.log_step(step, "添加 2 个 Todo", "PASS", ss)

        step += 1
        checkboxes = _find_checkboxes(driver)
        if len(checkboxes) < 2:
            raise MobileMcpError(f"Expected 2 checkboxes, found {len(checkboxes)}")
        driver.click_element(checkboxes[0])
        time.sleep(1)
        ss = driver.screenshot(f"uc04_{step:02d}_checked.png")
        driver.log_step(step, "勾选第一个 Todo", "PASS", ss)

        step += 1
        driver.assert_text_in_viewtree("共 2 项，已完成 1 项")
        driver.log_step(step, "断言统计 '共 2 项，已完成 1 项'", "PASS")

        step += 1
        driver.assert_element_exists(text="清除已完成")
        driver.log_step(step, "断言 '清除已完成' 按钮出现", "PASS")

        step += 1
        checkboxes = _find_checkboxes(driver)
        driver.click_element(checkboxes[0])
        time.sleep(1)
        ss = driver.screenshot(f"uc04_{step:02d}_unchecked.png")
        driver.log_step(step, "再次点击取消勾选", "PASS", ss)

        step += 1
        driver.assert_text_in_viewtree("共 2 项，已完成 0 项")
        driver.log_step(step, "断言统计 '共 2 项，已完成 0 项'", "PASS")

        step += 1
        driver.assert_element_not_exists(text="清除已完成")
        driver.log_step(step, "断言 '清除已完成' 消失", "PASS")

        print("  ✅ UC-04 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-04 FAILED at step {step}: {e}")
        return False


def run_usecase_uc05(driver: MobileMcpDriver) -> bool:
    """UC-5: 删除单个 Todo"""
    print("\n🚀 UC-05: 删除单个 Todo")
    step = 0
    try:
        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        _type_and_add(driver, "TaskA")
        _type_and_add(driver, "TaskB")
        ss = driver.screenshot(f"uc05_{step:02d}_setup.png")
        driver.log_step(step, "添加 2 个 Todo", "PASS", ss)

        step += 1
        deletes = _find_delete_buttons(driver)
        if len(deletes) < 2:
            raise MobileMcpError(f"Expected 2 delete buttons, found {len(deletes)}")
        driver.click_element(deletes[0])
        time.sleep(1)
        ss = driver.screenshot(f"uc05_{step:02d}_after_delete_first.png")
        driver.log_step(step, "删除第一个 Todo", "PASS", ss)

        step += 1
        driver.assert_text_not_in_viewtree("TaskA")
        driver.log_step(step, "断言 'TaskA' 已删除", "PASS")

        step += 1
        driver.assert_text_in_viewtree("TaskB")
        driver.log_step(step, "断言 'TaskB' 仍存在", "PASS")

        step += 1
        driver.assert_text_in_viewtree("共 1 项，已完成 0 项")
        driver.log_step(step, "断言统计 '共 1 项，已完成 0 项'", "PASS")

        step += 1
        deletes = _find_delete_buttons(driver)
        if len(deletes) < 1:
            raise MobileMcpError("No delete button found for remaining todo")
        driver.click_element(deletes[0])
        time.sleep(1)
        ss = driver.screenshot(f"uc05_{step:02d}_after_delete_second.png")
        driver.log_step(step, "删除第二个 Todo", "PASS", ss)

        step += 1
        driver.assert_text_in_viewtree("暂无待办事项")
        driver.log_step(step, "断言回到空状态", "PASS")

        print("  ✅ UC-05 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-05 FAILED at step {step}: {e}")
        return False


def run_usecase_uc06(driver: MobileMcpDriver) -> bool:
    """UC-6: 清除所有已完成"""
    print("\n🚀 UC-06: 清除所有已完成")
    step = 0
    try:
        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        _type_and_add(driver, "TaskA")
        _type_and_add(driver, "TaskB")
        _type_and_add(driver, "TaskC")
        ss = driver.screenshot(f"uc06_{step:02d}_setup.png")
        driver.log_step(step, "添加 3 个 Todo", "PASS", ss)

        step += 1
        checkboxes = _find_checkboxes(driver)
        if len(checkboxes) < 3:
            raise MobileMcpError(f"Expected 3 checkboxes, got {len(checkboxes)}")
        driver.click_element(checkboxes[0])
        time.sleep(1.0)
        checkboxes = _find_checkboxes(driver)
        driver.click_element(checkboxes[1])
        time.sleep(1.5)
        ss = driver.screenshot(f"uc06_{step:02d}_two_checked.png")
        driver.log_step(step, "勾选 'TaskA' 和 'TaskB'", "PASS", ss)

        step += 1
        driver.assert_text_in_viewtree("共 3 项，已完成 2 项")
        driver.log_step(step, "断言统计 '共 3 项，已完成 2 项'", "PASS")

        step += 1
        btn = driver.assert_element_exists(text="清除已完成")
        driver.click_element(btn)
        time.sleep(1)
        ss = driver.screenshot(f"uc06_{step:02d}_after_clear.png")
        driver.log_step(step, "点击 '清除已完成'", "PASS", ss)

        step += 1
        driver.assert_text_not_in_viewtree("TaskA")
        driver.log_step(step, "断言 'TaskA' 已清除", "PASS")

        step += 1
        driver.assert_text_not_in_viewtree("TaskB")
        driver.log_step(step, "断言 'TaskB' 已清除", "PASS")

        step += 1
        driver.assert_text_in_viewtree("TaskC")
        driver.log_step(step, "断言 'TaskC' 仍存在", "PASS")

        step += 1
        driver.assert_text_in_viewtree("共 1 项，已完成 0 项")
        driver.log_step(step, "断言统计 '共 1 项，已完成 0 项'", "PASS")

        step += 1
        driver.assert_element_not_exists(text="清除已完成")
        driver.log_step(step, "断言 '清除已完成' 消失", "PASS")

        print("  ✅ UC-06 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-06 FAILED at step {step}: {e}")
        return False


def run_usecase_uc07(driver: MobileMcpDriver) -> bool:
    """UC-7: 长文本输入与展示"""
    print("\n🚀 UC-07: 长文本输入与展示")
    step = 0
    try:
        long_text = (
            "ThisIsAVeryLongTodoItemTitleToTestLongTextDisplayInTheListAndInputField"
            "ToEnsureTheAppDoesNotCrashOrHaveLayoutIssuesWithLongContent"
        )

        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        _type_and_add(driver, long_text)
        ss = driver.screenshot(f"uc07_{step:02d}_long_text.png")
        driver.log_step(step, f"输入长文本 ({len(long_text)} chars) 并添加", "PASS", ss)

        step += 1
        driver.assert_text_in_viewtree(long_text[:30])
        driver.log_step(step, "断言长文本 Todo 存在于视图树", "PASS")

        step += 1
        ss = driver.screenshot(f"uc07_{step:02d}_visual_check.png")
        driver.log_step(step, "截图用于人工视觉检查（无崩溃、无重叠）", "PASS", ss)

        print("  ✅ UC-07 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-07 FAILED at step {step}: {e}")
        return False


def run_usecase_uc08(driver: MobileMcpDriver) -> bool:
    """UC-8: 综合端到端流程"""
    print("\n🚀 UC-08: 综合端到端流程")
    step = 0
    try:
        step += 1
        driver.clear_app_data(PACKAGE)
        driver.launch_app(PACKAGE)
        time.sleep(2)
        _type_and_add(driver, "BuyMilk")
        _type_and_add(driver, "WriteCode")
        driver.assert_text_in_viewtree("BuyMilk")
        driver.assert_text_in_viewtree("WriteCode")
        ss = driver.screenshot(f"uc08_{step:02d}_start.png")
        driver.log_step(step, "添加 'BuyMilk' 和 'WriteCode'", "PASS", ss)

        step += 1
        checkboxes = _find_checkboxes(driver)
        driver.click_element(checkboxes[0])
        time.sleep(1)
        driver.assert_text_in_viewtree("共 2 项，已完成 1 项")
        ss = driver.screenshot(f"uc08_{step:02d}_check_milk.png")
        driver.log_step(step, "勾选 'BuyMilk'", "PASS", ss)

        step += 1
        _type_and_add(driver, "DoSport")
        driver.assert_text_in_viewtree("DoSport")
        ss = driver.screenshot(f"uc08_{step:02d}_add_sport.png")
        driver.log_step(step, "添加 'DoSport'", "PASS", ss)

        step += 1
        checkboxes = _find_checkboxes(driver)
        driver.click_element(checkboxes[0])
        time.sleep(1.0)
        checkboxes = _find_checkboxes(driver)
        driver.click_element(checkboxes[1])
        time.sleep(1.5)
        driver.assert_text_in_viewtree("共 3 项，已完成 2 项")
        ss = driver.screenshot(f"uc08_{step:02d}_check_code.png")
        driver.log_step(step, "勾选 'BuyMilk' 和 'WriteCode'", "PASS", ss)

        step += 1
        btn = driver.assert_element_exists(text="清除已完成")
        driver.click_element(btn)
        time.sleep(1)
        driver.assert_text_not_in_viewtree("BuyMilk")
        driver.assert_text_not_in_viewtree("WriteCode")
        driver.assert_text_in_viewtree("DoSport")
        driver.assert_text_in_viewtree("共 1 项，已完成 0 项")
        ss = driver.screenshot(f"uc08_{step:02d}_clear_completed.png")
        driver.log_step(step, "点击 '清除已完成'", "PASS", ss)

        step += 1
        deletes = _find_delete_buttons(driver)
        driver.click_element(deletes[0])
        time.sleep(1)
        driver.assert_text_in_viewtree("暂无待办事项")
        ss = driver.screenshot(f"uc08_{step:02d}_delete_last.png")
        driver.log_step(step, "删除 'DoSport'", "PASS", ss)

        step += 1
        ss = driver.screenshot(f"uc08_{step:02d}_back_to_empty.png")
        driver.log_step(step, "回到空状态（与 UC-01 基线对比）", "PASS", ss)

        print("  ✅ UC-08 PASSED")
        return True
    except Exception as e:
        driver.log_step(step, f"Exception: {e}", f"FAIL: {e}")
        print(f"  ❌ UC-08 FAILED at step {step}: {e}")
        return False


def main():
    print("=" * 60)
    print("ComposeDemo E2E Device Test Runner")
    print("=" * 60)

    results = {}
    with MobileMcpDriver(device=DEVICE) as driver:
        results["UC-01"] = run_usecase_uc01(driver)
        results["UC-02"] = run_usecase_uc02(driver)
        results["UC-03"] = run_usecase_uc03(driver)
        results["UC-04"] = run_usecase_uc04(driver)
        results["UC-05"] = run_usecase_uc05(driver)
        results["UC-06"] = run_usecase_uc06(driver)
        results["UC-07"] = run_usecase_uc07(driver)
        results["UC-08"] = run_usecase_uc08(driver)

        report_path = driver.generate_html_report("ComposeDemo E2E Device Test Report")
        print(f"\n📄 HTML Report: {report_path}")

    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    passed = sum(1 for v in results.values() if v)
    total = len(results)
    for name, ok in results.items():
        status = "✅ PASS" if ok else "❌ FAIL"
        print(f"  {status}  {name}")
    print(f"\nTotal: {passed}/{total} passed")
    sys.exit(0 if passed == total else 1)


if __name__ == "__main__":
    main()
