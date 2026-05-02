# Code Snippet Manager

Desktop app for storing, organizing, searching, and exporting reusable code snippets.

- Java Swing frontend
- Repository-backed persistence with automatic fallback
- Syntax-highlighted code editor
- Search, filter, save, delete, and export snippets

## Overview

This project is a lightweight code snippet manager built for local development and daily reuse.
It keeps snippets in a structured library and provides a faster way to browse code by title, language, and tags.

## Features

- Save snippets with title, language, tags, and code
- Search by title, language, tag, or code content
- Filter the snippet library from the left panel
- Edit code in a dark syntax-highlighted editor
- Export the selected snippet or the filtered list
- Works with Railway MySQL, Neon/Postgres, or local file storage

## Architecture

The app is organized into a simple layered flow:

| Layer | Responsibility |
|---|---|
| Entry | `Main` starts the application and hands control to `SnippetApplication` |
| Application | `SnippetApplication` selects the repository implementation and launches the Swing UI |
| Persistence | `SnippetRepository`, `RailwayMySqlSnippetRepository`, and `NeonSnippetRepository` handle snippet storage |
| Service | `SnippetService` contains the business operations used by the UI |
| UI | `SnippetDashboardFrame`, `SnippetManagerFrame`, and `SyntaxHighlightTextPane` render the desktop experience |

Runtime flow:

```text
Main -> SnippetApplication -> Repository selection -> SnippetService -> Swing dashboard
```

Repository priority:

1. Railway MySQL
2. Neon/Postgres
3. Local file storage

## Flow Diagram

<img src="docs/flow-diagram.svg" alt="Animated flow diagram showing Main -> SnippetApplication -> repository selection -> SnippetService -> UI" />


How it works:

- `Main` starts the app.
- `SnippetApplication` decides which repository to use.
- The app prefers Railway MySQL, then Neon/Postgres, then local storage.
- `SnippetService` performs the snippet operations used by the Swing UI.
- `SnippetDashboardFrame` is the main desktop workspace where you search and edit snippets.



## Requirements

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |

## Project Structure

```text
src/main/java/com/snippetmanager/
|- Main.java
|- app/SnippetApplication.java
|- model/Snippet.java
|- persistence/
|  |- SnippetRepository.java
|  |- DataSourceProvider.java
|  |- RailwayMySqlSnippetRepository.java
|  `- NeonSnippetRepository.java
|- service/SnippetService.java
`- ui/
	|- SnippetManagerFrame.java
	|- SyntaxHighlightTextPane.java
	|- NeonPanel.java
	`- dashboard/SnippetDashboardFrame.java
```

## Running the App

Build the executable jar:

```bash
mvn -q -DskipTests package
java -jar target/snipet-1.0.0.jar
```

### Database Configuration

The app looks for a database in this order:

1. Railway MySQL
2. Neon/Postgres
3. Local file storage

### Railway MySQL

Set one of these environment variables before launching:

- `RAILWAY_MYSQL_URL`
- `MYSQL_URL`
- `MYSQL_PUBLIC_URL`
- `MYSQL_CONNECTION_URL`
- `DATABASE_URL`

The app accepts Railway-style URLs such as:

```text
mysql://root:password@host:port/railway
```

### Neon/Postgres Fallback

If you are using Neon or another Postgres provider, set one of:

- `NEON_JDBC_URL`
- `RAILWAY_DATABASE_URL`
- `POSTGRES_URL`
- `PGURL`

Optional user and password variables are also supported for both paths.

## What the App Stores

- Snippet id
- Title
- Language
- Tags
- Code body
- Created and updated timestamps

## UI Layout

- Left side: snippet library, search, and filters
- Right side: snippet editor and action buttons
- Dark code editor with syntax highlighting and placeholder text

## Notes

- The app seeds sample snippets when the database is empty.
- If no database is configured, it falls back to local storage.
- The executable entry point is `com.snippetmanager.Main`.
