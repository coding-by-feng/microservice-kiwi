# kason-ftp

A lightweight vsftpd FTP server container.

- No credentials baked into the image.
- Provide FTP_USER and FTP_PASS via environment variables at runtime.
- Optional PASV_ADDRESS support for NAT/macOS environments.

## Initialize (interactive)
If you prefer an interactive setup that prompts for FTP_USER and FTP_PASS (and re-prompts if left empty):

```sh
cd kason-deploy/ftp
chmod +x ./init_ftp.sh
./init_ftp.sh
```

- On macOS, the script maps ports 21 and 21100-21110 and will suggest a PASV_ADDRESS to advertise to clients.
- On Linux, it uses `--network host` by default.

## Manual steps
Follow these steps to build and run manually with your own credentials.

```sh
# Create a directory for the Dockerfile
mkdir -p ~/kason-ftp
cd ~/kason-ftp

# Save the Dockerfile (copy the content from the artifact above)
nano Dockerfile

# Build the image
sudo docker build -t kason-ftp:1.0 .

# Run the container with custom credentials
sudo docker run -d \
  --name kason-ftp \
  --network host \
  --restart unless-stopped \
  -e FTP_USER=xxx \
  -e FTP_PASS=xxx \
  -e FTP_BASE_DIR=/rangi_windows \
  -v /rangi_windows:/rangi_windows \
  kason-ftp:1.0
```

Note for macOS/NAT:
- Docker Desktop does not support `--network host`. Use port mappings and set PASV_ADDRESS to your host IP.

```sh
HOST_IP=192.168.1.100  # replace with your LAN/public IP
sudo docker run -d \
  --name kason-ftp \
  --restart unless-stopped \
  -p 21:21 \
  -p 21100-21110:21100-21110 \
  -e FTP_USER=xxx \
  -e FTP_PASS=xxx \
  -e FTP_BASE_DIR=/rangi_windows \
  -e PASV_ADDRESS=$HOST_IP \
  -v /rangi_windows:/rangi_windows \
  kason-ftp:1.0
```

## Build

```sh
# From the repo root or this directory
docker build -t kason-ftp:1.0 kason-deploy/ftp
```

## Run

Bind-mount the directory you want to expose and set FTP_BASE_DIR to match the mount location inside the container.

### Linux (host networking)
If your Docker host supports host networking (Linux), you can run:

```sh
docker run -d \
  --name kason-ftp \
  --network host \
  --restart unless-stopped \
  -e FTP_USER=<your-user> \
  -e FTP_PASS=<your-pass> \
  -e FTP_BASE_DIR=/rangi_windows \
  -v /rangi_windows:/rangi_windows \
  kason-ftp:1.0
```

### macOS / NAT environments
Docker Desktop on macOS does not support `--network host`. Expose the FTP control and passive ports and set `PASV_ADDRESS` to your host's reachable IP (LAN or public IP):

```sh
# Replace 192.168.1.100 with your Mac's LAN IP or your public IP if clients connect over the internet
HOST_IP=192.168.1.100

docker run -d \
  --name kason-ftp \
  --restart unless-stopped \
  -p 21:21 \
  -p 21100-21110:21100-21110 \
  -e FTP_USER=<your-user> \
  -e FTP_PASS=<your-pass> \
  -e FTP_BASE_DIR=/rangi_windows \
  -e PASV_ADDRESS=$HOST_IP \
  -v /rangi_windows:/rangi_windows \
  kason-ftp:1.0
```

Notes:
- Ensure your firewall allows inbound TCP on port 21 and 21100-21110.
- `PASV_ADDRESS` is optional on Linux/host networking, but typically required behind NAT or on macOS.
- The container creates the user if it does not exist, sets the password, and chroots the user into `FTP_BASE_DIR`.

## Environment variables
- `FTP_USER` (required): Login username.
- `FTP_PASS` (required): Login password.
- `FTP_BASE_DIR` (optional, default `/rangi_windows`): The FTP root directory and the user's home inside the container. Should match the mount path used with `-v`.
- `PASV_ADDRESS` (optional): External IP/hostname to advertise for passive connections (useful on macOS or behind NAT).

## Ports
- Control: 21/tcp
- Passive data: 21100-21110/tcp

## Troubleshooting
- If login works but directory listing fails, verify that passive ports are open and `PASV_ADDRESS` is set to an IP reachable by the client.
- If you see `ERROR: FTP_USER and FTP_PASS must be set via environment variables.`, ensure you provided `-e FTP_USER=... -e FTP_PASS=...` at `docker run`.
