FROM alpine:3.19

# Install vsftpd and shadow (for user management) and openssl (for password hashing)
RUN apk add --no-cache vsftpd shadow openssl

# Configure vsftpd
RUN echo "listen=YES" > /etc/vsftpd/vsftpd.conf && \
    echo "listen_ipv6=NO" >> /etc/vsftpd/vsftpd.conf && \
    echo "anonymous_enable=NO" >> /etc/vsftpd/vsftpd.conf && \
    echo "local_enable=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "write_enable=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "local_umask=022" >> /etc/vsftpd/vsftpd.conf && \
    echo "chroot_local_user=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "allow_writeable_chroot=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "pasv_enable=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "pasv_min_port=21100" >> /etc/vsftpd/vsftpd.conf && \
    echo "pasv_max_port=21110" >> /etc/vsftpd/vsftpd.conf && \
    echo "dirmessage_enable=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "use_localtime=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "xferlog_enable=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "connect_from_port_20=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "seccomp_sandbox=NO" >> /etc/vsftpd/vsftpd.conf && \
    echo "userlist_enable=YES" >> /etc/vsftpd/vsftpd.conf && \
    echo "userlist_file=/etc/vsftpd.userlist" >> /etc/vsftpd/vsftpd.conf && \
    echo "userlist_deny=NO" >> /etc/vsftpd/vsftpd.conf

# Expose FTP ports
EXPOSE 21 21100-21110

# Create entrypoint script
RUN printf '#!/bin/sh\n\
set -euo pipefail\n\
\n\
# Validate required environment variables\n\
if [ -z "${FTP_USER:-}" ] || [ -z "${FTP_PASS:-}" ]; then\n\
  echo "ERROR: FTP_USER and FTP_PASS must be set via environment variables." >&2\n\
  exit 1\n\
fi\n\
\
# Username sanity check (allow a-zA-Z0-9._-)\n\
case "$FTP_USER" in\n\
  *[!a-zA-Z0-9._-]* ) echo "ERROR: Invalid FTP_USER '\''$FTP_USER'\''. Allowed chars: a-z A-Z 0-9 . _ -" >&2; exit 1 ;;\n\
  "" ) echo "ERROR: Empty FTP_USER." >&2; exit 1 ;;\n\
  * ) ;;\n\
esac\n\
\
HOME_DIR="/home/$FTP_USER"\n\
# Optionally set pasv_address if PASV_ADDRESS is provided (useful on macOS/NAT)\n\
if [ -n "${PASV_ADDRESS:-}" ]; then\n\
  if grep -q "^pasv_address=" /etc/vsftpd/vsftpd.conf; then\n\
    sed -i "s/^pasv_address=.*/pasv_address=$PASV_ADDRESS/" /etc/vsftpd/vsftpd.conf\n\
  else\n\
    echo "pasv_address=$PASV_ADDRESS" >> /etc/vsftpd/vsftpd.conf\n\
  fi\n\
fi\n\
\
# Create FTP user if not exists (shadow suite)\n\
if ! id "$FTP_USER" >/dev/null 2>&1; then\n\
  useradd -m -d "$HOME_DIR" -s /bin/sh "$FTP_USER" || { echo "ERROR: Failed to add user." >&2; exit 1; }\n\
fi\n\
\
# Ensure home directory exists and ownership is correct\n\
mkdir -p "$HOME_DIR"\n\
chown "$FTP_USER:$FTP_USER" "$HOME_DIR"\n\
chmod 755 "$HOME_DIR"\n\
\
# Set password (SHA-512) using usermod with hashed value to avoid chpasswd issues\n\
HASH=$(openssl passwd -6 "$FTP_PASS") || { echo "ERROR: Failed to hash password." >&2; exit 1; }\n\
if ! usermod -p "$HASH" "$FTP_USER"; then\n\
  echo "ERROR: Failed to set password for user '\''$FTP_USER'\''." >&2\n\
  exit 1\n\
fi\n\
\
# Add user to allowed list (overwrite for single user)\n\
echo "$FTP_USER" > /etc/vsftpd.userlist\n\
\
# Log final state\n\
echo "FTP server starting for user '\''$FTP_USER'\'' (home: $HOME_DIR)"\n\
ls -ld "$HOME_DIR" || true\n\
\
# Start vsftpd in foreground\n\
exec /usr/sbin/vsftpd /etc/vsftpd/vsftpd.conf\n' > /entrypoint.sh && \
    chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]