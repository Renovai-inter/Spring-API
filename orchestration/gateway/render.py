"""Renderiza os manifests do gateway sem acessar AWS."""
import argparse
import hashlib
import ipaddress
from pathlib import Path
import yaml

BASE = Path(__file__).resolve().parent


def render(dns, certificate=""):
    ipaddress.ip_address(dns)
    if certificate and (not certificate.startswith("arn:aws:acm:") or ":certificate/" not in certificate):
        raise ValueError("NLB_CERTIFICATE_ARN deve ser um ARN de certificado ACM")
    config = (BASE / "nginx.conf").read_text(encoding="utf-8")
    config = config.replace("${DNS_RESOLVER}", dns).replace("${PUBLIC_SCHEME}", "https" if certificate else "http").replace("${PUBLIC_PORT}", "443" if certificate else "80")
    namespace, deployment, service = list(yaml.safe_load_all((BASE / "gateway.yaml").read_text(encoding="utf-8")))
    deployment["spec"]["template"]["metadata"]["annotations"] = {"checksum/config": hashlib.sha256(config.encode()).hexdigest()}
    if certificate:
        service["metadata"]["annotations"].update({
            "service.beta.kubernetes.io/aws-load-balancer-ssl-cert": certificate,
            "service.beta.kubernetes.io/aws-load-balancer-ssl-ports": "443",
            "service.beta.kubernetes.io/aws-load-balancer-backend-protocol": "tcp",
        })
        service["spec"]["ports"] = [{"name": "https", "port": 443, "targetPort": "http"}]
    configmap = {"apiVersion": "v1", "kind": "ConfigMap", "metadata": {"name": "renovai-gateway-config", "namespace": "renovai-api"}, "data": {"nginx.conf": config}}
    return [namespace, configmap, deployment, service]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--dns", required=True)
    parser.add_argument("--certificate", default="")
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    Path(args.output).write_text(yaml.safe_dump_all(render(args.dns, args.certificate), sort_keys=False), encoding="utf-8")
