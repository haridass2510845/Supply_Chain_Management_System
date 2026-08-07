# Supply Chain Management System

A Java (Servlet/JSP) web application for managing supply chain operations —
role-based login for Admins, Procurement Managers, Warehouse Managers,
Suppliers, and Logistics Staff, plus Supplier Management (Module 2).

## Folder structure

```
Supply_Chain_Management_System/
├── .vscode/settings.json     -> VS Code Java classpath config
├── src/com/scms/             -> Java source (dao, db, filter, model, servlet, util)
├── WebContent/                -> JSPs, CSS, WEB-INF (web.xml, lib, compiled classes)
├── sql/                        -> Database schema (Oracle and MySQL versions)
└── README.md
```

## One-time setup

1. **Create the database.** Run `sql/schema_oracle.sql` (Oracle) or `sql/schema.sql`
   (MySQL) against your database. Both now include the `users` table AND the
   `suppliers` table with sample data.

2. **Update DB credentials if needed** in
   `src/com/scms/db/DBConnection.java` (`DB_URL`, `DB_USER`, `DB_PASSWORD`).

3. **Point VS Code's Java extension at your Tomcat jars.** Open
   `.vscode/settings.json` and update the two `C:/.../lib/servlet-api.jar` and
   `.../jsp-api.jar` paths to match wherever Tomcat is installed on your machine.

4. **Point Tomcat at this folder.** Create (or update)
   `<tomcat>/conf/Catalina/localhost/SCMS_LoginModule.xml` with:
   ```xml
   <Context docBase="FULL_PATH_TO_THIS_FOLDER\WebContent" reloadable="true" />
   ```
   Replace `FULL_PATH_TO_THIS_FOLDER` with wherever you extracted this project.

## Compiling after Java changes

JSP edits reload automatically. After changing any `.java` file, recompile from
this folder (`Supply_Chain_Management_System`) in PowerShell:

```powershell
javac -cp "WebContent\WEB-INF\lib\ojdbc11.jar;C:\path\to\tomcat\lib\servlet-api.jar" `
      -d WebContent\WEB-INF\classes `
      (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

Then restart Tomcat.

## Sample login (all roles, password: `Passw0rd!`)

| Username     | Role                |
|--------------|---------------------|
| admin1       | ADMIN               |
| proc_mgr1    | PROCUREMENT_MANAGER |
| wh_mgr1      | WAREHOUSE_MANAGER   |
| supplier1    | SUPPLIER            |
| logistics1   | LOGISTICS_STAFF     |
