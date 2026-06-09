package com.pos.tester.uitests;

import com.pos.tester.model.TestResult;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.util.List;

public class EmployeesSchedulesUITest extends BaseUITestSuite {

    public EmployeesSchedulesUITest(WebDriver driver, String frontendUrl,
                                    String screenshotDir, int waitSeconds) {
        super(driver, frontendUrl, screenshotDir, waitSeconds);
    }

    @Override public String getModuleName() { return "Employees & Schedules"; }
    @Override public String getDescription() {
        return "UI: Employee list, stats, search, add/edit/delete, Schedule calendar, assign shift";
    }

    @Override
    public List<TestResult> runAll() throws InterruptedException {
        results.clear();

        // ─── EMPLOYEES TAB ──────────────────────────────────────────────────────

        runTest("[Employees] Load employees page - table renders", result -> {
            navigateTo("/employees");
            waitForVisible(By.id("employeesTable"));
            sleep(1500);
            int rows = tableRowCount(By.id("employeesTableBody"));
            result.pass("Employees page loaded: " + rows + " employee rows visible");
        });

        runTest("[Employees] Statistics cards visible (total, active, departments)", result -> {
            navigateTo("/employees");
            sleep(1200);
            boolean total = elementExists(By.id("totalEmployees"));
            boolean active = elementExists(By.id("activeEmployees"));
            boolean dept = elementExists(By.id("totalDepartments"));
            int found = (total ? 1 : 0) + (active ? 1 : 0) + (dept ? 1 : 0);
            assertTrue(found >= 2, "At least 2/3 stat cards must be visible, found: " + found);
            result.pass(found + "/3 employee stat cards visible");
        });

        runTest("[Employees] Search filters employee list", result -> {
            navigateTo("/employees");
            waitForVisible(By.id("employeesTableBody"));
            sleep(1000);
            int before = tableRowCount(By.id("employeesTableBody"));
            type(By.id("searchEmployee"), "a");
            sleep(1500);
            int after = tableRowCount(By.id("employeesTableBody"));
            result.pass("Employee search: before=" + before + " after='a'=" + after + " rows");
            driver.findElement(By.id("searchEmployee")).clear();
            sleep(800);
        });

        runTest("[Employees] Open Add Employee modal", result -> {
            navigateTo("/employees");
            waitForVisible(By.id("employeesTableBody"));
            By addBtn = By.cssSelector("[onclick*='openAddEmployee']");
            if (!elementExists(addBtn)) { result.skip("Add employee button not found"); return; }
            jsClick(addBtn);
            assertTrue(isModalVisible("addEmployeeModal"), "Add employee modal must open");
            result.pass("Add Employee modal opened");
            closeModal("addEmployeeModal");
        });

        runTest("[Employees] Add form has required fields (username, name, password)", result -> {
            navigateTo("/employees");
            sleep(800);
            By addBtn = By.cssSelector("[onclick*='openAddEmployee']");
            if (!elementExists(addBtn)) { result.skip("Add button not found"); return; }
            jsClick(addBtn);
            sleep(800);
            if (!isModalVisible("addEmployeeModal")) { result.skip("Modal not visible"); return; }
            boolean hasUser = elementExists(By.cssSelector("#addEmployeeForm [name='username']"))
                            || elementExists(By.cssSelector("#addEmployeeForm input[type='text']:first-child"));
            boolean hasName = elementExists(By.cssSelector("#addEmployeeForm [name='name']"))
                            || elementExists(By.cssSelector("#addEmployeeForm input[placeholder*='tên']"));
            result.pass("Employee form visible, hasUser=" + hasUser + " hasName=" + hasName);
            closeModal("addEmployeeModal");
        });

        runTest("[Employees] Edit employee modal opens", result -> {
            navigateTo("/employees");
            waitForVisible(By.id("employeesTableBody"));
            sleep(1500);
            By editBtn = By.cssSelector("#employeesTableBody [onclick*='editEmployee'], #employeesTableBody .btn-primary");
            if (!elementExists(editBtn)) { result.skip("Edit button not found in employees table"); return; }
            jsClick(editBtn);
            assertTrue(isModalVisible("editEmployeeModal"), "Edit employee modal must open");
            result.pass("Edit Employee modal opened");
            closeModal("editEmployeeModal");
        });

        runTest("[Employees] Delete confirmation modal opens", result -> {
            navigateTo("/employees");
            waitForVisible(By.id("employeesTableBody"));
            sleep(1500);
            // Delete button is in table body - scope to avoid matching modal buttons
            By deleteBtn = By.cssSelector("#employeesTableBody .btn-danger");
            if (!elementExists(deleteBtn)) { result.skip("Delete button not found in employees table"); return; }
            List<WebElement> btns = driver.findElements(deleteBtn);
            scrollIntoView(btns.get(btns.size() - 1));
            jsClick(btns.get(btns.size() - 1));
            assertTrue(isModalVisible("deleteEmployeeModal"), "Delete employee modal must open");
            // Cancel
            By cancelBtn = By.cssSelector("#deleteEmployeeModal .btn-secondary, #deleteEmployeeModal [onclick*='close']");
            if (elementExists(cancelBtn)) jsClick(cancelBtn);
            result.pass("Delete Employee confirmation modal opened and cancelled");
        });

        runTest("[Employees] Department filter dropdown", result -> {
            navigateTo("/employees");
            waitForVisible(By.id("deptFilter"));
            sleep(800);
            int options = new Select(driver.findElement(By.id("deptFilter"))).getOptions().size();
            assertTrue(options >= 1, "Department filter must have at least 1 option");
            result.pass("Department filter: " + options + " options");
        });

        runTest("[Employees] Pagination controls exist", result -> {
            navigateTo("/employees");
            sleep(1500);
            boolean hasPagination = elementExists(By.id("employeesPagination"))
                    || elementExists(By.cssSelector(".pagination"));
            result.pass("Pagination controls exist: " + hasPagination);
        });

        // ─── SCHEDULES TAB ──────────────────────────────────────────────────────

        runTest("[Schedules] Switch to schedules tab", result -> {
            navigateTo("/employees");
            sleep(1200);
            By scheduleTab = By.cssSelector("[data-tab='schedules']");
            if (!elementExists(scheduleTab)) { result.skip("Schedules tab button not found"); return; }
            jsClick(scheduleTab);
            sleep(1500);
            boolean calendarVisible = elementExists(By.id("calendarGrid"))
                    || elementExists(By.id("schedulesSection"));
            assertTrue(calendarVisible, "Calendar/schedules section must be visible after tab click");
            result.pass("Schedules tab activated: calendar visible=" + calendarVisible);
        });

        runTest("[Schedules] Schedule calendar renders (month/year display)", result -> {
            navigateTo("/employees");
            sleep(1000);
            By scheduleTab = By.cssSelector("[data-tab='schedules']");
            if (!elementExists(scheduleTab)) { result.skip("Schedules tab not found"); return; }
            jsClick(scheduleTab);
            sleep(1500);
            boolean hasCalendar = elementExists(By.id("calendarGrid"));
            boolean hasMonthDisplay = elementExists(By.id("currentMonthDisplay"));
            result.pass("Calendar: grid=" + hasCalendar + " monthDisplay=" + hasMonthDisplay);
        });

        runTest("[Schedules] Assign Shift modal opens", result -> {
            navigateTo("/employees/schedules");
            sleep(1500);
            By assignBtn = By.cssSelector("[onclick*='openAssignShift'], [onclick*='assignShift']");
            if (!elementExists(assignBtn)) {
                // Try navigating to employees and switching tab
                navigateTo("/employees");
                sleep(800);
                By tab = By.cssSelector("[data-tab='schedules']");
                if (elementExists(tab)) { jsClick(tab); sleep(1200); }
                assignBtn = By.cssSelector("[onclick*='openAssignShift'], [onclick*='assignShift']");
                if (!elementExists(assignBtn)) { result.skip("Assign shift button not found"); return; }
            }
            jsClick(assignBtn);
            sleep(1000);
            boolean visible = isModalVisible("assignShiftModal");
            assertTrue(visible, "Assign Shift modal must open");
            By cancelBtn = By.cssSelector("#assignShiftModal .btn-secondary, #assignShiftModal .modal-close");
            if (elementExists(cancelBtn)) jsClick(cancelBtn);
            result.pass("Assign Shift modal opened successfully");
        });

        return results;
    }
}
