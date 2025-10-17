⸻

Troubleshooting Report: SSH Connection Refused from LAN Client

Scenario
	•	Device A: kason-pi (Debian/Ubuntu, SSH server)
	•	Device B: Rock Pi (Ubuntu, SSH client)
	•	Both devices on same internal network.
	•	Mac can SSH to kason-pi, Rock Pi cannot (Connection refused).
	•	Ping works between Rock Pi and kason-pi.

⸻

Step 1 — Verify SSH Server Status

Question: Is sshd running and listening on the correct port?

Commands on kason-pi:

sudo ss -tulpn | grep 22
sudo systemctl status ssh

Expected Outcome:
	•	sshd should be LISTEN on 0.0.0.0:22 (all interfaces).
	•	No errors in service status.

Observation in case study:
	•	sshd listening on 0.0.0.0:22 (IPv4) and [::]:22 (IPv6).
	•	Service active ✅

⸻

Step 2 — Attempt SSH from Rock Pi with Verbose Mode

Question: What error does SSH report?

Command on Rock Pi:

ssh -v kason@192.168.1.72

Observation:
	•	Connection refused immediately.
	•	No logs appear on kason-pi in journalctl or auth.log.

Interpretation:
	•	TCP connection is rejected before reaching sshd.
	•	Likely network-level issue (firewall, isolation, routing).

⸻

Step 3 — Check SSH Logs on Server

Question: Does sshd log the incoming connection attempt?

Command on kason-pi:

sudo journalctl -u ssh -f
sudo tail -f /var/log/auth.log

Observation:
	•	Connections from Mac appear.
	•	Connections from Rock Pi do not appear.

Interpretation:
	•	Packets from Rock Pi are never reaching sshd.

⸻

Step 4 — Capture Network Traffic

Question: Are TCP SYN packets arriving at kason-pi?

Command on kason-pi:

sudo tcpdump -n -i any 'tcp port 22 and host <Rock-Pi-IP>'

Observation:
	•	No traffic captured when Rock Pi attempts SSH.
	•	Ping works → ICMP reaches server.

Interpretation:
	•	TCP traffic blocked at network layer, before sshd.

⸻

Step 5 — Check Firewall Rules (Optional)

Question: Is a firewall dropping connections?

Commands on kason-pi:

sudo ufw status
sudo iptables -L -n
sudo iptables -t raw -L -n
sudo nft list ruleset

Observation in case study:
	•	ufw inactive.
	•	iptables / nftables rules present only for docker; nothing blocking wlan0.

Interpretation:
	•	Local firewall not the cause.

⸻

Step 6 — Check IP Addresses and Interfaces

Question: Are both devices on the same subnet?

Commands:

# kason-pi
hostname -I

# Rock Pi
ip a | grep inet

Observation:
	•	kason-pi: 192.168.1.191 (wlan0)
	•	Rock Pi: 192.168.1.120 (wlan0)
	•	Both devices on same subnet.

⸻

Step 7 — Check for Wi-Fi / AP Isolation

Question: Is the access point blocking client-to-client traffic?

Observation:
	•	Mac can connect → possibly bypassed isolation using mDNS/IPv6.
	•	Rock Pi cannot connect via IPv4 TCP → likely Wi-Fi AP isolation.

Action:
	•	Check router/AP for settings:
	•	“AP Isolation” / “Client Isolation” / “Guest Network Isolation”
	•	Disable isolation on the SSID used by both devices.

⸻

Step 8 — ARP Verification

Question: Can Rock Pi resolve kason-pi’s MAC?

Commands on Rock Pi:

arp -n | grep 192.168.1.191

Observation:
	•	No ARP entry → confirms packets not reaching server.

⸻

Step 9 — Test Alternative Network Path

Question: Does SSH work on a direct connection / hotspot?

Action:
	•	Connect Rock Pi to Mac hotspot.
	•	Assign IPs in same subnet.
	•	Attempt SSH:

ssh kason@192.168.1.191

Observation:
	•	SSH works → confirms Wi-Fi client isolation is the root cause.

⸻

Step 10 — Summary / Root Cause

Symptom	Observation	Conclusion
Mac can SSH, Rock Pi cannot	SSH works from Mac, fails from Rock Pi	Not sshd
Ping works	ICMP succeeds	Network reachable
No SSH logs from Rock Pi	tcpdump shows no TCP SYN	TCP blocked before sshd
Firewall inactive / Docker rules present	Firewall not blocking wlan0	Not firewall
IPs on same subnet	192.168.1.x /24	Same subnet
Wi-Fi interface used	wlan0	AP isolation likely
Direct connection works	SSH works when bypassing AP isolation	Confirmed root cause

Root Cause:

Wi-Fi client isolation (AP isolation) blocked TCP traffic between Rock Pi and kason-pi.

Resolution:
	•	Disable AP/client isolation on router for the SSID.
	•	Alternatively, connect both devices to the same network segment without isolation (Ethernet, hotspot, or adjusted Wi-Fi SSID).

⸻

✅ Key Lessons / Checklist for Future
	1.	Always confirm sshd is listening and service active.
	2.	Use verbose SSH to capture client-side errors.
	3.	Check server logs — absence of logs can indicate network drop.
	4.	Use tcpdump to see if packets reach server.
	5.	Verify firewall rules (ufw, iptables, nftables).
	6.	Confirm both devices are on the same subnet and interface.
	7.	Check ARP table — missing entry = packet blocked.
	8.	Consider AP isolation / guest network isolation on Wi-Fi routers.
	9.	Test with direct connection / hotspot to isolate router restrictions.

⸻
