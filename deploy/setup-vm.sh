#!/usr/bin/env bash
set -euo pipefail

echo "=================================================="
echo " Starting Oracle Cloud Ampere A1 VM Setup"
echo " Target OS: Ubuntu 22.04 LTS+"
echo "=================================================="

# 1. Update and upgrade package index
echo "==> Updating package repository and upgrading installed packages..."
sudo apt-get update
sudo apt-get upgrade -y

# 2. Install prerequisites
echo "==> Installing basic prerequisites..."
sudo apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    software-properties-common \
    iptables \
    iptables-persistent

# 3. Install Docker Engine and Docker Compose Plugin
echo "==> Setting up Docker repository..."
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg --yes
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

echo "==> Installing Docker Engine and Compose plugin..."
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "==> Enabling and starting Docker service..."
sudo systemctl enable --now docker

# Add current user to docker group if not already added
if ! groups "$USER" | grep -q '\bdocker\b'; then
    echo "==> Adding user $USER to docker group..."
    sudo usermod -aG docker "$USER"
fi

# 4. Install Nginx, Certbot, and Python Certbot Nginx plugin
echo "==> Installing Nginx, Certbot, and python3-certbot-nginx..."
sudo apt-get install -y nginx certbot python3-certbot-nginx
sudo systemctl enable --now nginx

# 5. Setup application directory /opt/stormapi
echo "==> Setting up /opt/stormapi directory..."
sudo mkdir -p /opt/stormapi
sudo chown -R "$USER:$USER" /opt/stormapi
sudo chmod 755 /opt/stormapi

# 6. Configure iptables firewall rules
echo "==> Configuring iptables firewall..."
# Allow established connections and loopback
sudo iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
sudo iptables -A INPUT -i lo -j ACCEPT

# Allow ports 22 (SSH), 80 (HTTP), 443 (HTTPS)
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# Explicitly block direct access to 8080 (backend) and 5432 (PostgreSQL)
sudo iptables -A INPUT -p tcp --dport 8080 -j DROP
sudo iptables -A INPUT -p tcp --dport 5432 -j DROP

# Save iptables rules to persist across reboots
if command -v netfilter-persistent &> /dev/null; then
    sudo netfilter-persistent save
elif [ -d /etc/iptables ]; then
    sudo iptables-save | sudo tee /etc/iptables/rules.v4 > /dev/null
fi

echo "=================================================="
echo " VM Setup Complete!"
echo "=================================================="
echo "To issue an SSL certificate using Certbot with Nginx, run:"
echo "  sudo certbot --nginx -d your-domain.com"
echo "=================================================="
