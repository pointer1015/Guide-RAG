# ==========================================
# GuideRAG Root .gitignore
# ==========================================

# --- Java / Maven ---
target/
*.class
*.jar
*.war
*.ear
*.log
.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

# --- Spring Boot ---
**/src/main/resources/application-dev.yml
**/src/main/resources/application-prod.yml
**/src/main/resources/*.jks
*.p12
*.pfx

# --- Node / Frontend ---
node_modules/
dist/
stats.json
npm-debug.log*
yarn-debug.log*
yarn-error.log*
.eslintcache
.stylelintcache
*.local
.env
.env.production
.env.development

# --- IDE (IntelliJ / VS Code) ---
.idea/
*.iws
*.iml
*.ipr
.vscode/
.project
.classpath
.settings/
.factorypath

# --- OS Specific ---
.DS_Store
Thumbs.db
desktop.ini

# --- Logs & Temp ---
logs/
*.tmp
*.bak
dump.rdb
