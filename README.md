# Supply Chain Management System

A Java (Servlet/JSP) web application for managing supply chain operations —
role-based login for Admins, Procurement Managers, Warehouse Managers,
Suppliers, and Logistics Staff, plus Supplier Management, Procurement
Management, and full user administration.

## Folder structure

```
Supply_Chain_Management_System/
├── .vscode/settings.json     -> VS Code Java classpath config
├── src/com/scms/             -> Java source (dao, db, filter, model, servlet, util)
├── WebContent/                -> JSPs, CSS, WEB-INF (web.xml, lib, compiled classes)
├── sql/                        -> Database schema (Oracle and MySQL versions)
└── README.md
```

## Modules implemented

- **Module 1 — Authentication:** login, self-registration with email OTP
  verification, forgot-password with email OTP reset, change password,
  role-based dashboards, session-based access control (`AuthFilter`).
- **Manage Users (Admin):** create, update, activate/deactivate, and delete
  user accounts of any role. `manage_users.jsp` + `UserServlet` + `UserDAO`.
  Guardrails prevent an admin from deactivating/deleting their own account
  or the last remaining active administrator.
- **Module 2 — Supplier Management:** add / update / delete / search suppliers
  (Admin only). `suppliers.jsp` + `SupplierServlet` + `SupplierDAO`.
- **Module 3 — Procurement Management:** create, approve, cancel, and track
  purchase orders, plus a supplier performance table and summary report cards.
  Available to ADMIN and PROCUREMENT_MANAGER. `purchase_orders.jsp` +
  `PurchaseOrderServlet` + `PurchaseOrderDAO`.
- **Supplier Portal:** the self-service side of Module 3 for SUPPLIER-role
  logins -- view orders assigned to your company, mark approved orders as
  shipped, confirm delivery, and see your own performance history.
  `my_orders.jsp` + `SupplierPortalServlet`. Requires a SUPPLIER account to
  be linked to a `suppliers` record via `users.supplier_id` (see setup below).
- **Module 4 — Warehouse Management:** receive completed purchase orders into
  stock, view current inventory with low-stock alerts, dispatch goods, make
  manual stock adjustments, and see recent warehouse activity. Available to
  ADMIN and WAREHOUSE_MANAGER. `warehouse.jsp` + `WarehouseServlet` +
  `InventoryDAO`.
- **Module 5 — Logistics Management:** assign a carrier and destination to
  goods dispatched from the warehouse, track shipments through
  Assigned -> In Transit -> Delivered, and confirm final delivery. Available
  to ADMIN and LOGISTICS_STAFF. `logistics.jsp` + `LogisticsServlet` +
  `LogisticsDAO`. Every shipment is tied to one warehouse dispatch, so goods
  only enter the logistics pipeline once they've actually left the warehouse.
- **Reports (Admin):** a read-only cross-module report combining data already
  produced by every other module -- supplier performance, procurement summary,
  low-stock inventory alerts, and in-transit shipments. `reports.jsp`. No new
  tables; it just reads existing DAOs.
- **Monitor System (Admin):** live database health check, active/inactive
  account counts, and a login audit trail (every login attempt, successful
  or not, with username/IP/timestamp). `monitor.jsp` + `AuditDAO` +
  `login_audit` table. `LoginServlet` now logs every attempt.

### Email OTP verification (new)

Two flows now send a 6-digit one-time code by email:

- **Register** (`register.jsp` -> `RegisterServlet` -> `verify_otp.jsp`):
  the account isn't created in the database until the code is verified.
  Nothing about the pending signup is persisted anywhere except the
  browser's server-side session, so an abandoned registration leaves no
  trace.
- **Forgot Password** (`forgot_password.jsp` -> `ForgotPasswordServlet` ->
  `reset_password_otp.jsp`): looks the account up by username or email,
  emails a code to the address on file, and lets you set a new password
  once it's verified.

Both flows share `OtpUtil` (code generation), `OtpChallenge` (5-minute
expiry, 5-attempt lockout, session-scoped), and `EmailUtil` (the actual
mailer). See **"Configuring email (OTP)"** below -- **the project works
immediately without any setup**, because `EmailUtil` prints the OTP to the
Tomcat console until you configure a real mailbox.

## One-time setup

1. **Create the database.** Run `sql/schema_oracle.sql` (Oracle) or `sql/schema.sql`
   (MySQL) against your database. Both include the `users`, `suppliers`,
   `purchase_orders`, `inventory`, and `inventory_transactions` tables with
   sample data, plus the `users.supplier_id` link that connects a SUPPLIER
   login to its supplier company record.

   **Already have these tables from an earlier setup?** Don't re-run the whole
   script -- read the "MIGRATING AN EXISTING DATABASE" comments near the bottom
   of the relevant SQL file and run just those statements instead.

2. **Update DB credentials if needed** in
   `src/com/scms/db/DBConnection.java` (`DB_URL`, `DB_USER`, `DB_PASSWORD`).

3. **Point VS Code's Java extension at your Tomcat jars.** Open
   `.vscode/settings.json` and update the two `C:/.../lib/servlet-api.jar` and
   `.../jsp-api.jar` paths to match wherever Tomcat is installed on your
   machine -- or just keep using the bundled copies in `ide-libs/` (already
   referenced by `settings.json`; no changes needed for those to work).

4. **Point Tomcat at this folder.** Create (or update)
   `<tomcat>/conf/Catalina/localhost/SCMS_LoginModule.xml` with:
   ```xml
   <Context docBase="FULL_PATH_TO_THIS_FOLDER\WebContent" reloadable="true" />
   ```
   Replace `FULL_PATH_TO_THIS_FOLDER` with wherever you extracted this project.

5. **(Optional) Configure email (OTP).** See below. The app runs fine without
   this step in dev mode -- OTP codes just print to the Tomcat console instead
   of being emailed.

## Configuring email (OTP)

Open `src/com/scms/util/EmailUtil.java` and edit the four constants near the
top of the file:

```java
private static final String SMTP_HOST = "smtp.gmail.com";
private static final int SMTP_PORT = 587;
private static final String SMTP_USERNAME = "your-email@gmail.com";
private static final String SMTP_PASSWORD = "your-16-char-app-password";
```

**For Gmail:**
1. Turn on 2-Step Verification on the Google account you'll send from.
2. Go to Google Account -> Security -> 2-Step Verification -> **App Passwords**.
3. Generate a 16-character app password and paste it into `SMTP_PASSWORD`
   (your normal Gmail password will *not* work here).
4. Put the full Gmail address in `SMTP_USERNAME`.

**For Outlook/Office365:** use `smtp.office365.com`, port `587`, and your
normal account credentials (or an app password if MFA is enabled).

Until both `SMTP_USERNAME` and `SMTP_PASSWORD` are changed from their
placeholder `"your-..."` values, `EmailUtil` stays in **dev mode**: no real
email is sent, and every OTP code is printed to the Tomcat console (and
shown as a small on-screen note during registration/reset) so you can keep
testing without setting up a mailbox at all.

No external mail jar is needed -- `EmailUtil` talks SMTP directly over a
plain `Socket`/`SSLSocketFactory` using only the standard JDK.

## Compiling after Java changes

JSP edits reload automatically. After changing any `.java` file, recompile
from this folder (`Supply_Chain_Management_System`):

**Windows (PowerShell):**
```powershell
javac -encoding UTF-8 -cp "WebContent\WEB-INF\lib\ojdbc11.jar;ide-libs\servlet-api.jar;ide-libs\jsp-api.jar" `
      -d WebContent\WEB-INF\classes `
      (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

**macOS/Linux:**
```bash
javac -encoding UTF-8 -cp "WebContent/WEB-INF/lib/ojdbc11.jar:ide-libs/servlet-api.jar:ide-libs/jsp-api.jar" \
      -d WebContent/WEB-INF/classes \
      $(find src -name "*.java")
```

Then restart Tomcat. **Always include `-encoding UTF-8`** -- without it, the
compiler falls back to your OS's default codepage (Windows often uses
`Cp1252`), which can turn a harmless em-dash in a comment into a hard
compile error. The `.vscode/settings.json` in this project also forces
`files.encoding: utf8` so VS Code's Java extension won't hit the same issue.

## If VS Code still shows Problems after opening this project

This most commonly means VS Code doesn't have this exact folder open as the
workspace root, or the Java language server needs a refresh. Try these in
order:

1. **File -> Open Folder...** and select `Supply_Chain_Management_System`
   itself (not its parent folder, and not the extracted zip's top level).
   `.vscode/settings.json` only takes effect when this folder is the
   workspace root.
2. Make sure the **"Extension Pack for Java"** (`vscjava.vscode-java-pack`)
   is installed.
3. Run **Command Palette -> "Java: Clean Java Language Server Workspace"**,
   then reload when prompted. This clears any stale project index left over
   from before these fixes.
4. Check the **Problems panel** again -- this project now compiles with
   **zero errors or warnings** (verified with `javac -encoding UTF-8`), so
   anything still showing after a clean+reload is worth double-checking
   against the exact file/line it points to.

## Sample login (all roles, password: `Passw0rd!`)

| Username     | Role                |
|--------------|---------------------|
| admin1       | ADMIN               |
| proc_mgr1    | PROCUREMENT_MANAGER |
| wh_mgr1      | WAREHOUSE_MANAGER   |
| supplier1    | SUPPLIER            |
| logistics1   | LOGISTICS_STAFF     |

New accounts created via `register.jsp` or the admin's "Manage Users" panel
work the same way once created/verified.
