# Static game binary assets (.bin) directory

Place all server-facing .bin files here so they are bundled and found at runtime.

Default resolution order:

-   Java code reads assets via Util.assetFile("relative/path.bin"), which resolves to `${ECHO_ASSETS_DIR}` if set, else `-Decho.assets.dir`, else this `assets/` folder.

Expected files (examples):

-   player-overview.bin
-   gamelist.bin
-   gameinfo.bin
-   hostconnection.bin
-   test/4b71.bin
-   test/4b72.bin

You can organize subfolders (e.g. `test/`). Ensure docker-compose mounts `./assets:/app/assets:ro`.
