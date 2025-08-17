# Contributing Guidelines

You're welcomed to make contributions to this project!

## Project structure

```
opanel
├── frontend/
│   ├── app
│   ├── components
│   └── ...
├── core
├── fabric-...
├── spigot-...
├── paper-...
└── ...
```

- Folder `frontend` stores the sourcecode of the frontend app. The frontend is developed with [Next.js](https://nextjs.org) and [Shadcn UI](https://ui.shadcn.com).
- Folder `core` is the core of the whole project, containing a bunch of core logics and features, such as web server and backend API.
- Folders that starts with `fabric-` are implementations of Fabric mods for different Minecraft versions.
- Folders that starts with `spigot-` are implementations of Spigot plugins for different Minecraft versions.
- Folders that starts with `paper-` are implementations of Paper plugins for different Minecraft versions.

As soon as the initialization of the plugin / mod, the program will start a web server on the specified port (default `3000`), which contains the frontend app and the backend API.

Under the development environment, the development frontend app is served at port `3001`, and it will connect to the backend API with a hard-coded url `http://localhost:3000` (See `frontend/lib/api.ts`). So when developing the frontend app, your backend server should always be on the port `3000`.

## Requirements

Before starting, you need to have [VSCode](https://code.visualstudio.com) and [Intellij IDEA](https://jetbrains.com/idea) installed. The following guidelines assumes you are using the two code editor.

Then, you need to install [Node.js](https://nodejs.org) and jdk 21.

## Install dependencies

After cloning the repo, you're supposed to install all dependencies to run the app.

### Gradle dependencies

1. Open the project with Intellij IDEA, and the IDEA will automatically start a task to download dependencies.

2. After the task ends, click on the gradle icon in the right sidebar. If you can find the task `fabric/runServer` in the menu, the Gradle dependencies are correctly installed.

### Node.js dependencies

1. Enter the folder `frontend`.

```cmd
cd frontend
```

2. Run the command to install dependencies.

```cmd
npm install
```

## Development

### Test Server / Backend API

#### Fabric

Run the task `fabric/runServer` in the gradle menu. After the server fully starts, the backend API and production frontend app are served at port `3000`. Open `http://localhost:3000` with your browser.

**Note: Usually, we don't use the production version to develop the frontend app.**

#### Spigot / Paper

Setup a spigot / paper test server in advance. Follow the following guides to build the jar files, and then copy the corresponding jar file into your plugins folder. After executing command `/reload`, the latest changes will be applied to your test server.

### Frontend

In the folder `frontend`, run the following command:

```cmd
npm run dev
```

And the development frontend app is served at port `3001`. Open `http://localhost:3001` with your browser.

## Build from source

1. Build the frontend app

Inside the `frontend` folder, execute

```cmd
npm run build
```

This step will automatically bundle the frontend and copy the bundled files into `core/src/main/resources/web`.

2. Build the server plugin

Inside the project root folder, execute

```cmd
.\gradlew clean
.\gradlew build
```

And the built jar files will be at the `build/libs` folder of each modules. For example, the built jar file of `fabric-1.21.5` is `fabric-1.21.5/build/libs/opanel-fabric-1.21.5-build-<version>.jar`.
