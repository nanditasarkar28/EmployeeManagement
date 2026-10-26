# Employee Attendance and Payroll Tracker

A console application written in **Core Java + JDBC + MySQL 8**. It manages employees, daily attendance and monthly payroll. No frameworks (no Spring, Hibernate, JPA) and no GUI.

---

## 1. Required software

| Software | Version | Where to get it |
|---|---|---|
| JDK (Java Development Kit) | 17 or 21 | https://adoptium.net (Temurin, Windows `.msi`) |
| Eclipse IDE for Java Developers | any recent | https://www.eclipse.org/downloads/ |
| MySQL Server + MySQL Workbench | 8.0 | https://dev.mysql.com/downloads/installer/ |
| MySQL Connector/J | 8.x or 9.x | https://dev.mysql.com/downloads/connector/j/ |
| Git (only for GitHub upload) | any | https://git-scm.com/download/win |

### Install Java and Eclipse
1. Install the **JDK** (Temurin 21 is fine). Tick "Set JAVA_HOME" and "Add to PATH" if the installer offers it.
2. Open Command Prompt and run `java -version` and `javac -version`. Both must print 17 or 21.
3. Run the Eclipse installer and choose **Eclipse IDE for Java Developers**. Start Eclipse and pick a workspace folder.

### Install MySQL Server and Workbench
1. Download **MySQL Installer for Windows** and run it.
2. Choose **Custom** (or Developer Default) and install **MySQL Server 8.0** and **MySQL Workbench**.
3. When asked, set a **root password**. Write it down. You will put it in `DBConnection.java`.
4. Keep the defaults: port **3306**, Windows service name `MySQL80`, "Start at system startup".
5. Open **MySQL Workbench**, double-click the `Local instance MySQL80` connection, enter your root password. If you see the query editor, MySQL works.

---

## 2. Database setup

1. In Workbench choose **File → Open SQL Script...** and open `database.sql` from this project.
2. Click the **lightning bolt** (Execute) button.
3. The last lines show the three sample employees. Click the refresh icon in the left "Schemas" panel and you will see `payroll_db` with `employees`, `attendance`, `payroll`.

The script is safe to run again: it uses `CREATE ... IF NOT EXISTS` and inserts a sample employee only if that name is not already there. It never drops anything.

---

## 3. Eclipse import / setup

The project already contains Eclipse's `.project` and `.classpath` files.

1. Unzip `EmployeePayrollTracker.zip` somewhere simple, for example `C:\dev\EmployeePayrollTracker` (avoid OneDrive folders).
2. In Eclipse: **File → Import... → General → Existing Projects into Workspace → Next**.
3. Click **Browse...** next to "Select root directory" and choose the unzipped `EmployeePayrollTracker` folder. Tick the project and click **Finish**.
4. Check **Window → Preferences → Java → Installed JREs**: the default must be Java 17 or 21. If not, add your JDK there.

(If you prefer to create the project yourself: **File → New → Java Project**, name `EmployeePayrollTracker`, and when asked about `module-info.java` choose **Don't Create**. Then create package `payroll` under `src` and copy the six `.java` files into it.)

---

## 4. Connector/J setup (adds the MySQL driver)

1. Go to https://dev.mysql.com/downloads/connector/j/
2. In "Select Operating System" choose **Platform Independent**. Download the **ZIP Archive** and click "No thanks, just start my download".
3. Unzip it. Inside is a file named like `mysql-connector-j-9.x.x.jar`.
4. Copy that jar into the project's `lib` folder (optionally rename it `mysql-connector-j.jar`). In Eclipse, right-click the project and press **F5** (Refresh).
5. Add it to the build path:
   **Project → Properties → Java Build Path → Libraries → Classpath → Add External JARs...** → pick the jar → **Open → Apply and Close**.
   (You may instead use **Add JARs...** and pick the copy inside `lib`, which keeps the project portable.)
6. Under the project, **Referenced Libraries** should now list the jar.

The jar is not included in this ZIP because it has to be downloaded from MySQL.

---

## 5. Configure the password

Open `src/payroll/DBConnection.java` and change only this line:

```java
private static final String PASSWORD = "YOUR_MYSQL_PASSWORD_HERE";
```

to your own root password, for example `"MyRealPassword"`. Save the file. Host `localhost`, port `3306`, database `payroll_db` and user `root` are already set.

Never upload a file containing your real password to GitHub (see section 11).

---

## 6. Exact run steps

1. MySQL service is running (Windows: `services.msc` → `MySQL80` → Running).
2. `database.sql` has been executed.
3. The Connector/J jar is on the build path and the password is set.
4. In Eclipse's Package Explorer open `src → payroll → Main.java`.
5. Right-click `Main.java` → **Run As → Java Application**.
6. Click inside the **Console** tab at the bottom, then type your menu choice and press Enter.

You should see `Connected to MySQL database successfully.` followed by the menu.

---

## 7. Project structure

```
EmployeePayrollTracker/
  src/payroll/
    Main.java             Menu, input reading/validation, printing
    DBConnection.java     The one place with host/port/user/password
    Employee.java         Model: one employee
    EmployeeDAO.java      SQL for employees
    AttendanceDAO.java    SQL for attendance
    PayrollDAO.java       SQL for payroll + working-day/deduction calculations
    AttendanceRecord.java Helper (extra)
    PayrollRecord.java    Helper (extra)
  lib/                    Put the Connector/J jar here
  database.sql
  README.md
```

**Why two extra helper classes?** `AttendanceRecord` and `PayrollRecord` carry one row each (plus the employee name from a JOIN). Without them the DAO classes would have to print to the screen themselves. With them, DAOs only talk to the database, and `Main` only talks to the user. That separation is the main design idea of the project.

---

## 8. Business rules

### Working days
**Working days = calendar days in the month, excluding Sundays.**
September 2026: 30 days, 4 Sundays (6, 13, 20, 27) → **26 working days**.
The same function (`PayrollDAO.calculateWorkingDays`) is used everywhere, including the monthly attendance summary.
Because Sundays are not working days, the program refuses to mark attendance on a Sunday. Public holidays are not modelled in this beginner version.

### Formulas
```
Daily Salary = Monthly Salary / Total Working Days
Deduction    = (Monthly Salary / Total Working Days) x Absent Days
Net Salary   = Monthly Salary - Deduction
```

### Money and rounding (BigDecimal)
- All money is `BigDecimal`, never `double` (`double` cannot store 0.10 exactly).
- Every result is rounded to **2 decimal places** with **`RoundingMode.HALF_UP`**: 0.005 and above rounds up (4615.384 → 4615.38, 4615.385 → 4615.39).
- Deduction is computed as `Monthly Salary x Absent Days / Working Days`: multiply first, divide **once**. It is the same formula, but only one rounding happens, so the error is at most half a paisa/cent.
- Because of that, `Daily Salary x Absent Days` can differ from the saved deduction by a paisa/cent. The **deduction** is the official figure. Daily salary is shown for information and is not stored.
- Net salary = monthly salary − the already-rounded deduction, so the numbers always add up exactly.
- Salary input must be greater than 0, with at most 2 decimals, and at most 99999999.99 (the limit of `DECIMAL(10,2)`).

### Attendance rules
- Employee ID must exist. Date must be a real date (`2026-02-30` is rejected), not in the future, and not a Sunday.
- One record per employee per date: checked in Java **and** enforced by the `UNIQUE` constraint in MySQL (two layers).

### Payroll rules
- Only past or current months. A payroll for a month is generated **once** per employee (`UNIQUE` constraint plus a check beforehand).
- **Only days marked Absent are deducted.** Working days with no attendance record are *not* deducted. The program warns you ("22 working day(s) have NO attendance record") and asks for confirmation. Mark attendance for the whole month first for a real payroll.
- Changing an employee's salary later does **not** change old payroll rows. History shows the salary that applied then (computed as net + deduction).

### When deletion is blocked
An employee **cannot be deleted while any attendance or payroll rows exist** for them. The foreign keys use `ON DELETE RESTRICT`, so MySQL itself refuses, and the program checks first to give a friendly message with the counts. This protects pay history. Only an employee who has no attendance and no payroll (for example one added by mistake) can be deleted.

---

## 9. JDBC concepts used (and where)

| Concept | What it is | Where |
|---|---|---|
| `DriverManager` | Finds the MySQL driver and opens a connection from a URL | `DBConnection.getConnection()` |
| `Connection` | One open conversation with MySQL | every DAO method |
| `PreparedStatement` | SQL with `?` placeholders; values are sent separately from the SQL text | every DAO method |
| Parameterized queries | `ps.setInt(1, id)` instead of `"... WHERE id=" + id`. Stops SQL injection | everywhere; no user input is ever concatenated into SQL |
| `executeQuery()` | Runs a `SELECT`, returns a `ResultSet` | `getAllEmployees`, `getEmployeeById`, counts... |
| `executeUpdate()` | Runs `INSERT`/`UPDATE`/`DELETE`, returns rows changed | `addEmployee`, `updateEmployee`, `deleteEmployee`, `markAttendance`, `savePayroll` |
| `ResultSet` | The rows returned; `while (rs.next())` moves row by row | all SELECT methods |
| try-with-resources | `try (Connection c = ...; PreparedStatement ps = ...; ResultSet rs = ...)` closes everything automatically, even after errors | every DAO method |
| `SQLException` | Any database error. `getErrorCode()` gives MySQL's number (1045 bad password, 1062 duplicate, 1451 FK blocks delete...) | DAOs throw; `Main.printDatabaseError` explains |
| Foreign keys | `attendance.employee_id` and `payroll.employee_id` must exist in `employees` | `database.sql`; error 1452/1451 handled |

The connection URL is:
`jdbc:mysql://localhost:3306/payroll_db?useSSL=false&allowPublicKeyRetrieval=true`
The two options only prevent common login problems with a local MySQL 8 server. Each DAO method opens its own short-lived connection. That is the simplest correct approach for a learning project; real applications use a connection pool.

### Input strategy (why prompts are never skipped)
There is one `Scanner`. Every answer is read with `nextLine()` (a whole line) and converted by our own code. We never call `nextInt()`/`nextDouble()`, because they leave the Enter key in the buffer and the next `nextLine()` would silently return an empty string. Every `readXxx` helper loops until the input is valid.

---

## 10. Sample console session

(Real output from running these exact sources against a MySQL 8.0 server. Dates are from September 2026; your menus are identical.)

```
=== Employee Attendance and Payroll Tracker ===
Connected to MySQL database successfully.

========== MAIN MENU ==========
 1. Add Employee
 ...
10. Exit
===============================
Enter your choice (1-10): 2

--- All Employees ---
ID    Name                   Department         Designation                  Salary
-----------------------------------------------------------------------------------
1     Aarav Sharma           Engineering        Software Engineer          60000.00
2     Priya Verma            Human Resources    HR Executive               45000.00
3     Rohan Mehta            Finance            Accountant                 52500.50
Total employees: 3

Enter your choice (1-10): 6
--- Mark Attendance ---
Employee ID: 1
Employee: Aarav Sharma
Date (YYYY-MM-DD, or type 'today'): 2026-09-03
Status (P = Present, A = Absent): A
Attendance saved: Aarav Sharma - 2026-09-03 - Absent

Enter your choice (1-10): 6
Employee ID: 1
Date (YYYY-MM-DD, or type 'today'): 2026-09-03
Status (P = Present, A = Absent): P
Attendance for this employee on 2026-09-03 is already recorded.

Enter your choice (1-10): 6
Employee ID: 1
Date (YYYY-MM-DD, or type 'today'): 2026-09-06
Sundays are not working days, so attendance cannot be marked on them.

Enter your choice (1-10): 8
--- Calculate Monthly Payroll ---
Employee ID: 1
Payroll month (YYYY-MM): 2026-09
Warning: 22 working day(s) have NO attendance record.
Only days marked Absent are deducted; unmarked days are NOT deducted.
Continue and generate payroll anyway? (y/n): y

----- Payroll: Aarav Sharma (ID 1) - 2026-09 -----
Monthly salary     : 60000.00
Total working days : 26
Present days       : 2
Absent days        : 2
Daily salary       : 2307.69
Deduction          : 4615.38
Net salary         : 55384.62
Payroll record saved.

Enter your choice (1-10): 8
Employee ID: 1
Payroll month (YYYY-MM): 2026-09
Payroll for Aarav Sharma for 2026-09 has already been generated. Use option 9 to view it.

Enter your choice (1-10): 9
--- Payroll History ---
1. All payroll records
2. Payroll history of one employee
Choose 1 or 2: 1
ID   EmpID Name             Month     Work  Pres   Abs      Salary   Deduction         Net
---------------------------------------------------------------------------------------------
1    1     Aarav Sharma     2026-09     26     2     2    60000.00     4615.38    55384.62
Records shown: 1

Enter your choice (1-10): 5
--- Delete Employee ---
Employee ID: 1
...
Cannot delete this employee. They still have 4 attendance record(s) and 1 payroll record(s).
Only employees with no attendance and no payroll records can be deleted.
```

Check of the numbers: 60000 × 2 / 26 = 4615.3846… → **4615.38**; 60000 − 4615.38 = **55384.62**.

---

## 11. Test every feature (checklist)

| # | Menu | Do this | Expected |
|---|---|---|---|
| 1 | 2 | View all | The 3 sample employees |
| 2 | 1 | Add an employee; try salary `abc`, `-5`, `1000.555`, then `40000.50` | Each bad value is rejected and re-asked; last one saved with a new ID |
| 3 | 3 | Search ID `1`, then `99` | Details; then "No employee found" |
| 4 | 4 | Update an employee, press Enter on every question except one | Only that field changes; all-blank = "Nothing was changed" |
| 5 | 6 | Mark Present for a past Monday | Saved |
| 6 | 6 | Same employee + same date again | "already recorded" |
| 7 | 6 | A Sunday / a future date / `2026-02-30` / employee `99` | Each refused with a clear message |
| 8 | 7 → 1 | All records | Table of every record |
| 9 | 7 → 2 | One employee + a month | That month's records + Present/Absent/Not marked counts |
| 10 | 8 | Payroll for an employee/month with attendance | Full breakdown, then "Payroll record saved." |
| 11 | 8 | Same employee/month again; a future month | "already generated"; "future month" refused |
| 12 | 9 | History for all, then one employee | The saved payroll rows |
| 13 | 5 | Delete employee who has attendance/payroll | Blocked with counts |
| 14 | 5 | Delete the employee you added in step 2 (no records) | Asks y/n, then deleted |
| 15 | 10 | Exit | "Goodbye!" |
| 16 | – | Type `abc` or `99` at the menu | "Invalid choice" and the menu again |

---

## 12. Common errors and fixes

| Message | Cause | Fix |
|---|---|---|
| `Database error: access denied...` (error 1045) | Wrong password or user | Fix `PASSWORD` in `DBConnection.java`; test the same password in Workbench |
| `cannot reach MySQL...` / "Communications link failure" | MySQL not running, or another port | Start `MySQL80` in `services.msc`; check port 3306 in Workbench |
| `'payroll_db' does not exist` (1049) or table missing (1146) | `database.sql` not run | Run it in Workbench |
| `java.sql.SQLException: No suitable driver found` or `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | Connector/J jar not on the build path | Redo section 4; the jar must appear under *Referenced Libraries* |
| `Public Key Retrieval is not allowed` | Missing URL option | Keep `allowPublicKeyRetrieval=true` in the URL |
| `UnsupportedClassVersionError` | Eclipse uses an older Java than the one that compiled | Window → Preferences → Java → Installed JREs → pick 17/21, then Project → Properties → Java Compiler |
| `Could not find or load main class payroll.Main` | Project has build errors or wrong source folder | Project → Clean...; confirm `src` is a source folder |
| Red marks on imports / `package payroll` | Files not inside `src/payroll` | Move them into package `payroll` |
| Console ignores typing | Cursor is not in the Console tab | Click in the Console, then type |
| Prompt accepts nothing / Ctrl+D | Input stream ended | Program exits cleanly; just run it again |
| Workbench error 1175 "safe update mode" | Workbench setting (not this program) | Not triggered by `database.sql`; if you run your own updates, disable it in Preferences → SQL Editor |

---

## 13. Upload to GitHub

1. Install **Git for Windows** and create a free account at https://github.com. On GitHub click **New repository**, name it `EmployeePayrollTracker`, leave "Add README" **unticked**, and create it.
2. **Remove your real password first.** Set `PASSWORD` back to `"YOUR_MYSQL_PASSWORD_HERE"` in `DBConnection.java` and save.
3. Open **Git Bash** (or Command Prompt) inside the project folder and run:

```bash
git init
git add .
git commit -m "Employee Attendance and Payroll Tracker: Core Java, JDBC, MySQL"
git branch -M main
git remote add origin https://github.com/<your-username>/EmployeePayrollTracker.git
git push -u origin main
```

4. Check `git status` shows nothing unexpected. `.gitignore` already skips `bin/` and `.class` files.
5. If you ever push a real password by accident, **change your MySQL password immediately**. Deleting the file later does not remove it from Git history.
6. Do not commit the Connector/J jar unless you want to; add it to `.gitignore` and tell readers (as this README does) to download it.
