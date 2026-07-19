# Player Rollback

A powerful administration utility for Bukkit/Paper servers that takes automated snapshots of player inventories, equipment, location, and status metadata upon joining and dying. Admins can easily restore inventory profiles via an interactive GUI.

## Features

- **Automated Snapshots:** Automatically saves complete inventory states when a player joins the server or dies.
- **Interactive Admin GUI:** Opens a menu to preview snapshots and restore specific items or entire profiles.
- **Lightweight Storage:** Uses standard Yaml files to save snapshots locally.

## Commands & Permissions

- `/rollback <player>` — Opens the rollback GUI menu for the specified player. Requires `playerrollback.admin`.
