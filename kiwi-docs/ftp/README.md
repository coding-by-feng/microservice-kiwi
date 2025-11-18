# Kiwi FTP Service

Single-user vsftpd container with user chrooted to their home directory (/home/$FTP_USER). Host directory is mounted directly to that home path. Passive ports 21100-21110 exposed; optional PASV_ADDRESS for NAT/macOS.

## Why the change
Original image used FTP_BASE_DIR=/rangi_windows and attempted to set the password even when FTP_USER was empty or the user had not yet been created, producing repeated `chpasswd: pam_chauthtok() failed` errors. Root directory requirement (home) was unmet. This refactor:
- Validates FTP_USER / FTP_PASS are non-empty and username matches [a-zA-Z0-9._-]
- Creates user first under /home/$FTP_USER then sets password (single attempt)
- Chroots via vsftpd default and maps host storage into that home directory
- Adds clearer logging and optional PASV_ADDRESS injection

## Build & Run (interactive)
```sh
cd kiwi-deploy/ftp
./init_ftp.sh
# Follow prompts: username, password, host directory, (PASV_ADDRESS on macOS)
```
Image tag default: kiwi-ftp:1.1
Container name default: kiwi-ftp

## Non-interactive quick start
```sh
export FTP_USER=demo
export FTP_PASS='ChangeMe123'
export FTP_HOST_DIR=$PWD/ftp-data
mkdir -p "$FTP_HOST_DIR"
cd kiwi-deploy/ftp
IMAGE_TAG=kiwi-ftp:1.1 CONTAINER_NAME=kiwi-ftp ./init_ftp.sh <<EOF


EOF
```
(Empty heredoc just skips interactive prompts if env vars provided.)

On macOS add `PASV_ADDRESS=$(ipconfig getifaddr en0)` if needed.

## Direct docker run (already built)
```sh
docker build -t kiwi-ftp:1.1 kiwi-deploy/ftp
mkdir -p ftp-data
docker run -d --name kiwi-ftp \
  -e FTP_USER=demo -e FTP_PASS='ChangeMe123' \
  -v "$PWD/ftp-data":/home/demo \
  -p 21:21 -p 21100-21110:21100-21110 \
  kiwi-ftp:1.1
```
Add `-e PASV_ADDRESS=<public_ip>` when behind NAT.

## Verify container state
```sh
docker exec -it kiwi-ftp grep demo /etc/passwd
docker exec -it kiwi-ftp ls -ld /home/demo
docker logs kiwi-ftp | tail -n 20
```
You should see home `/home/demo`, correct ownership, and startup log without chpasswd errors.

## Test FTP login (lftp example)
```sh
lftp -u demo,ChangeMe123 localhost
lftp> pwd
lftp> put somefile.txt
```
Passive mode issues: ensure ports 21100-21110 open; set PASV_ADDRESS to external IP/hostname when connecting from outside host.

## Troubleshooting
- chpasswd error: Check FTP_USER not empty; inspect `docker logs`. Recreate container after setting env vars.
- Permission denied uploading: Host directory ownership inside container should be your FTP user; run `docker exec chown demo:demo /home/demo` if needed.
- Cannot list directory (chroot): Ensure `allow_writeable_chroot=YES` present (already configured).
- Passive mode stalls: Provide `-e PASV_ADDRESS=<external_ip>` and open the passive port range in firewall/security groups.

## Security Notes
- Password transmitted in cleartext unless using FTPS (not configured yet). Avoid using sensitive credentials.
- Consider enabling TLS (future enhancement): add certificate, set `ssl_enable=YES` and related directives.
- Limit exposure: on Linux prefer `--network host` only if appropriate; otherwise map ports explicitly similar to macOS style.

## Future Enhancements
- Multi-user support via mounting a provisioning file and iterating user creation
- Optional TLS/FTPS
- Healthcheck script

## Clean Up
```sh
docker rm -f kiwi-ftp
docker rmi kiwi-ftp:1.1
```

## Differences from previous version
Old: ENV FTP_BASE_DIR, password set regardless of user creation, potential empty username.
New: Home-based chroot, deterministic mount, validation & logging.

---
Last updated: 2025-11-19

