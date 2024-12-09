#!/bin/sh

# This script installs all the JS dependencies and builds the JavaScript and CSS bundles.
# It also starts a watcher which triggers a rebuild of these bundles whenever JS or CSS code changes.
#
# It has to be run once before the UI server is started.
# You need to rerun it if you update your JS dependencies (package.json).
# Please see the Scout documentation for details about the available run scripts: https://eclipsescout.github.io/scout-docs/24.1/technical-guide/user-interface/build-stack.html#command-line-interface-cli
#
# To make this script work you need a current version of Node.js (>=22.12.0), npm (>=10.9.0) and pnpm (>=9.15.0).
# Node.js (incl. npm) is available here: https://nodejs.org/.
# pnpm is available here: https://pnpm.io/

# Abort the script if any command fails
set -e

# Specify the path to the node and npm binaries
PATH=$PATH:/usr/local/bin

# Check if node is available
command -v node >/dev/null 2>&1 || { echo >&2 "node cannot be found. Make sure Node.js is installed and the PATH variable correctly set. See the content of this script for details."; exit 1; }

# Check if pnpm is available
command -v pnpm >/dev/null 2>&1 || { echo >&2 "pnpm cannot be found. Make sure pnpm is installed. See the content of this script for details."; exit 1; }

# Install all JavaScript dependencies => creates the node_modules folders
cd ..
echo "Running 'pnpm install' in ${symbol_dollar}{PWD}"
pnpm install --ignore-scripts
echo "pnpm install finished successfully!\n"

# Build the JavaScript and CSS bundles and start the watcher => creates the dist folder
cd ${rootArtifactId}.app
echo "Running 'pnpm build:dev:watch'"
pnpm run build:dev:watch
