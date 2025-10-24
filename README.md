# ATM Banking Simulation System

## Requirements
- Java 17+ (JDK)
- Command line (Terminal / CMD / PowerShell)

## Compile
### macOS/Linux
```bash
mkdir -p out
javac -d out $(find src -name "*.java")
javac -d out -cp out test/CustomAccountTests.java
```

### Windows (PowerShell)
```powershell
mkdir out
Get-ChildItem -Recurse -Filter *.java src | % FullName | % { $_ } | javac -d out @-
javac -d out -cp out test/CustomAccountTests.java
```
*(If `@-` doesn’t work in your shell, use: `dir /s /b src\*.java > sources.txt` then `javac -d out @sources.txt`.)*

## Run
```bash
java -cp out atm.Main
```

## Test (no external libraries)
Enable assertions `-ea`:
```bash
java -ea -cp out CustomAccountTests
```

## Default log file
- `logs/atm.log`

## Sample logins
- User: `100001 / 1111`
- User: `100002 / 2222`
- User: `100003 / 3333`
- Admin: `999999 / 1234`

## Notes
- Data persists in `data/accounts.csv` and `data/transactions.csv`.
- Admin can create/deactivate accounts from the menu.
