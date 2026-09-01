package com.transcriptor.hid.data.db

/**
 * Provider for seeding default developer categories, production snippets, and macros.
 */
object DefaultToolPackProvider {

    suspend fun seedDefaultDatabase(db: AppDatabase) {
        val categoryDao = db.categoryDao()
        val snippetDao = db.snippetDao()
        val macroDao = db.macroDao()

        // 1. Insert Default Categories
        val gitCatId = categoryDao.insertCategory(
            CategoryEntity(
                name = "Git & VCS",
                iconName = "Commit",
                colorHex = "#F44336",
                displayOrder = 0,
                isDefault = true
            )
        )
        val dockerCatId = categoryDao.insertCategory(
            CategoryEntity(
                name = "Containers & Cloud",
                iconName = "Cloud",
                colorHex = "#2196F3",
                displayOrder = 1,
                isDefault = true
            )
        )
        val devtoolsCatId = categoryDao.insertCategory(
            CategoryEntity(
                name = "Languages & Runtimes",
                iconName = "Code",
                colorHex = "#FF9800",
                displayOrder = 2,
                isDefault = true
            )
        )
        val terminalCatId = categoryDao.insertCategory(
            CategoryEntity(
                name = "Terminal & Navigation",
                iconName = "Terminal",
                colorHex = "#4CAF50",
                displayOrder = 3,
                isDefault = true
            )
        )
        val aiCatId = categoryDao.insertCategory(
            CategoryEntity(
                name = "AI Prompting",
                iconName = "Psychology",
                colorHex = "#9C27B0",
                displayOrder = 4,
                isDefault = true
            )
        )

        // 2. Insert Default Production Snippets
        val snippets = listOf(
            // Git & VCS Category
            SnippetEntity(
                title = "Git Status",
                content = "git status\n",
                categoryId = gitCatId,
                syntaxType = SyntaxType.GIT,
                isFavorite = true,
                orderIndex = 0,
                tags = listOf("git", "status", "vcs")
            ),
            SnippetEntity(
                title = "Git Commit with Message",
                content = "git commit -m \"{{prompt:Commit Message}}\"\n",
                categoryId = gitCatId,
                syntaxType = SyntaxType.GIT,
                isFavorite = true,
                orderIndex = 1,
                tags = listOf("git", "commit")
            ),
            SnippetEntity(
                title = "Git New Branch & Switch",
                content = "git checkout -b {{prompt:Branch Name}}\n",
                categoryId = gitCatId,
                syntaxType = SyntaxType.GIT,
                isFavorite = false,
                orderIndex = 2,
                tags = listOf("git", "branch", "checkout")
            ),
            SnippetEntity(
                title = "Git Rebase Pull",
                content = "git pull --rebase origin $(git branch --show-current)\n",
                categoryId = gitCatId,
                syntaxType = SyntaxType.GIT,
                isFavorite = false,
                orderIndex = 3,
                tags = listOf("git", "rebase", "pull")
            ),
            SnippetEntity(
                title = "Git Stash & Sync",
                content = "git stash && git pull && git stash pop\n",
                categoryId = gitCatId,
                syntaxType = SyntaxType.GIT,
                isFavorite = false,
                orderIndex = 4,
                tags = listOf("git", "stash", "sync")
            ),
            SnippetEntity(
                title = "Git Log OneLine",
                content = "git log --oneline -n 10\n",
                categoryId = gitCatId,
                syntaxType = SyntaxType.GIT,
                isFavorite = false,
                orderIndex = 5,
                tags = listOf("git", "log")
            ),

            // Containers & Cloud Category
            SnippetEntity(
                title = "Docker Compose Up",
                content = "docker compose up -d --build\n",
                categoryId = dockerCatId,
                syntaxType = SyntaxType.DOCKER,
                isFavorite = true,
                orderIndex = 0,
                tags = listOf("docker", "compose", "up")
            ),
            SnippetEntity(
                title = "Docker Compose Down",
                content = "docker compose down --remove-orphans\n",
                categoryId = dockerCatId,
                syntaxType = SyntaxType.DOCKER,
                isFavorite = false,
                orderIndex = 1,
                tags = listOf("docker", "compose", "down")
            ),
            SnippetEntity(
                title = "Docker Container List",
                content = "docker ps -a --format \"table {{.ID}}\\t{{.Names}}\\t{{.Status}}\\t{{.Ports}}\"\n",
                categoryId = dockerCatId,
                syntaxType = SyntaxType.DOCKER,
                isFavorite = false,
                orderIndex = 2,
                tags = listOf("docker", "ps")
            ),
            SnippetEntity(
                title = "K8s Get All Pods",
                content = "kubectl get pods -A -o wide\n",
                categoryId = dockerCatId,
                syntaxType = SyntaxType.KUBERNETES,
                isFavorite = true,
                orderIndex = 3,
                tags = listOf("k8s", "kubectl", "pods")
            ),
            SnippetEntity(
                title = "K8s Follow Pod Logs",
                content = "kubectl logs -f --tail=100 {{prompt:Pod Name}}\n",
                categoryId = dockerCatId,
                syntaxType = SyntaxType.KUBERNETES,
                isFavorite = false,
                orderIndex = 4,
                tags = listOf("k8s", "logs")
            ),
            SnippetEntity(
                title = "K8s Pod Shell Exec",
                content = "kubectl exec -it {{prompt:Pod Name}} -- /bin/sh\n",
                categoryId = dockerCatId,
                syntaxType = SyntaxType.KUBERNETES,
                isFavorite = false,
                orderIndex = 5,
                tags = listOf("k8s", "exec", "shell")
            ),

            // Languages & Runtimes Category
            SnippetEntity(
                title = "Cargo Release Build",
                content = "cargo build --release\n",
                categoryId = devtoolsCatId,
                syntaxType = SyntaxType.RUST,
                isFavorite = true,
                orderIndex = 0,
                tags = listOf("rust", "cargo", "build")
            ),
            SnippetEntity(
                title = "Cargo Test (No Capture)",
                content = "cargo test --all -- --nocapture\n",
                categoryId = devtoolsCatId,
                syntaxType = SyntaxType.RUST,
                isFavorite = false,
                orderIndex = 1,
                tags = listOf("rust", "cargo", "test")
            ),
            SnippetEntity(
                title = "Cargo Clippy Strict",
                content = "cargo clippy --all-targets -- -D warnings\n",
                categoryId = devtoolsCatId,
                syntaxType = SyntaxType.RUST,
                isFavorite = false,
                orderIndex = 2,
                tags = listOf("rust", "clippy", "lint")
            ),
            SnippetEntity(
                title = "Pytest Verbose Short",
                content = "pytest -v --tb=short\n",
                categoryId = devtoolsCatId,
                syntaxType = SyntaxType.PYTHON,
                isFavorite = true,
                orderIndex = 3,
                tags = listOf("python", "pytest", "test")
            ),
            SnippetEntity(
                title = "Python Venv Activate",
                content = "python3 -m venv .venv && source .venv/bin/activate\n",
                categoryId = devtoolsCatId,
                syntaxType = SyntaxType.PYTHON,
                isFavorite = false,
                orderIndex = 4,
                tags = listOf("python", "venv")
            ),
            SnippetEntity(
                title = "Go Run Main",
                content = "go run main.go\n",
                categoryId = devtoolsCatId,
                syntaxType = SyntaxType.PLAIN_TEXT,
                isFavorite = false,
                orderIndex = 5,
                tags = listOf("go", "run")
            ),

            // Terminal & Navigation Category
            SnippetEntity(
                title = "Tmux New Named Session",
                content = "tmux new -s {{prompt:Session Name|dev}}\n",
                categoryId = terminalCatId,
                syntaxType = SyntaxType.SHELL,
                isFavorite = true,
                orderIndex = 0,
                tags = listOf("tmux", "session")
            ),
            SnippetEntity(
                title = "Tmux Attach Session",
                content = "tmux attach -t {{prompt:Session Name|dev}}\n",
                categoryId = terminalCatId,
                syntaxType = SyntaxType.SHELL,
                isFavorite = false,
                orderIndex = 1,
                tags = listOf("tmux", "attach")
            ),
            SnippetEntity(
                title = "SSH Login Host",
                content = "ssh {{prompt:User|root}}@{{prompt:Host IP or Domain}}\n",
                categoryId = terminalCatId,
                syntaxType = SyntaxType.SHELL,
                isFavorite = true,
                orderIndex = 2,
                tags = listOf("ssh", "remote")
            ),
            SnippetEntity(
                title = "System Resource Monitor (htop)",
                content = "htop\n",
                categoryId = terminalCatId,
                syntaxType = SyntaxType.SHELL,
                isFavorite = false,
                orderIndex = 3,
                tags = listOf("system", "htop", "process")
            ),
            SnippetEntity(
                title = "Disk Usage Human",
                content = "df -h\n",
                categoryId = terminalCatId,
                syntaxType = SyntaxType.SHELL,
                isFavorite = false,
                orderIndex = 4,
                tags = listOf("disk", "df")
            ),

            // AI Prompting Templates
            SnippetEntity(
                title = "AI Prompt: Fix Compiler Errors",
                content = "Fix the following compiler error and explain the root cause concisely:\n{{clipboard}}\n",
                categoryId = aiCatId,
                syntaxType = SyntaxType.PROMPT,
                isFavorite = true,
                orderIndex = 0,
                tags = listOf("ai", "debug", "compiler")
            ),
            SnippetEntity(
                title = "AI Prompt: Generate Unit Tests",
                content = "Write comprehensive unit tests covering edge cases, happy paths, and error scenarios for this code:\n{{clipboard}}\n",
                categoryId = aiCatId,
                syntaxType = SyntaxType.PROMPT,
                isFavorite = false,
                orderIndex = 1,
                tags = listOf("ai", "testing", "unit")
            ),
            SnippetEntity(
                title = "AI Prompt: Code Review",
                content = "Review this code for security vulnerabilities, edge cases, and performance bottlenecks:\n{{clipboard}}\n",
                categoryId = aiCatId,
                syntaxType = SyntaxType.PROMPT,
                isFavorite = true,
                orderIndex = 2,
                tags = listOf("ai", "review", "security")
            )
        )

        snippetDao.insertAll(snippets)

        // 3. Insert Default Macros
        val vsCodeMacro = MacroEntity(
            title = "VS Code: Save & Run Test",
            description = "Saves all files, opens terminal panel, and runs pytest",
            iconName = "PlayArrow",
            categoryId = devtoolsCatId,
            orderIndex = 0,
            stepsJson = """
                [
                    {"type":"key_combo","modifiers":1,"usageId":22,"holdMs":20},
                    {"type":"delay","durationMs":100},
                    {"type":"key_combo","modifiers":1,"usageId":53,"holdMs":20},
                    {"type":"delay","durationMs":150},
                    {"type":"type_string","text":"pytest -v\n","delayMs":8}
                ]
            """.trimIndent()
        )
        macroDao.insertMacro(vsCodeMacro)

        val terminalClearMacro = MacroEntity(
            title = "Terminal: Clear & Re-run",
            description = "Clears terminal screen and re-runs last command with Enter",
            iconName = "Refresh",
            categoryId = terminalCatId,
            orderIndex = 0,
            stepsJson = """
                [
                    {"type":"key_combo","modifiers":1,"usageId":15,"holdMs":20},
                    {"type":"delay","durationMs":100},
                    {"type":"key_combo","modifiers":0,"usageId":82,"holdMs":20},
                    {"type":"delay","durationMs":50},
                    {"type":"key_combo","modifiers":0,"usageId":40,"holdMs":20}
                ]
            """.trimIndent()
        )
        macroDao.insertMacro(terminalClearMacro)
    }
}
