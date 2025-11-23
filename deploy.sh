docker build -t poddeck-agent:latest .
docker save -o poddeck-agent.tar poddeck-agent:latest
sudo k3s ctr images import poddeck-agent.tar
kubectl rollout restart deployment/poddeck-agent