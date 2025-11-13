FROM alpine:3.19

# Install vsftpd and shadow (for chpasswd)
RUN apk add --no-cache vsftpd shadow

# Environment variables (FTP_USER and FTP_PASS must be provided at runtime)
# ENV FTP_USER and FTP_PASS intentionally not set to avoid embedding secrets in the image
# Optional: PASV_ADDRESS can be provided at runtime to set vsftpd pasv_address
ENV FTP_BASE_DIR=/rangi_windows

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
set -e\n\
\n\
# Validate required environment variables\n\
if [ -z "${FTP_USER:-}" ] || [ -z "${FTP_PASS:-}" ]; then\n\
    echo "ERROR: FTP_USER and FTP_PASS must be set via environment variables." >&2\n\
    exit 1\n\
fi\n\
\n\
# Optionally set pasv_address if PASV_ADDRESS is provided (useful on macOS/NAT)\n\
if [ -n "${PASV_ADDRESS:-}" ]; then\n\
    if grep -q "^pasv_address=" /etc/vsftpd/vsftpd.conf; then\n\
        sed -i "s/^pasv_address=.*/pasv_address=$PASV_ADDRESS/" /etc/vsftpd/vsftpd.conf\n\
    else\n\
        echo "pasv_address=$PASV_ADDRESS" >> /etc/vsftpd/vsftpd.conf\n\
    fi\n\
fi\n\
\n\
# Create FTP user if not exists\n\
if ! id "$FTP_USER" >/dev/null 2>&1; then\n\
    adduser -D -h "$FTP_BASE_DIR" -s /bin/sh "$FTP_USER"\n\
fi\n\
\n\
# Set password\n\
printf "%s:%s\n" "$FTP_USER" "$FTP_PASS" | chpasswd\n\
\n\
# Create FTP directory\n\
mkdir -p "$FTP_BASE_DIR"\n\
chown -R "$FTP_USER:$FTP_USER" "$FTP_BASE_DIR"\n\
\n\
# Add user to allowed list\n\
echo "$FTP_USER" > /etc/vsftpd.userlist\n\
\n\
# Start vsftpd in foreground\n\
exec /usr/sbin/vsftpd /etc/vsftpd/vsftpd.conf\n' > /entrypoint.sh && \
    chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]