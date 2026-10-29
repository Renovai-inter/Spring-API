import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import time
import unittest
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
import uuid

BASE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("gateway_render", BASE / "render.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class GatewayTest(unittest.TestCase):
    def test_single_loadbalancer_and_tls(self):
        for cert in ("", "arn:aws:acm:us-east-1:123456789012:certificate/example"):
            with self.subTest(cert=cert):
                docs = module.render("10.100.0.10", cert)
                services = [d for d in docs if d["kind"] == "Service"]
                self.assertEqual(len(services), 1)
                service = services[0]
                self.assertNotIn("loadBalancerClass", service["spec"])
                annotations = service["metadata"]["annotations"]
                self.assertEqual(annotations["service.beta.kubernetes.io/aws-load-balancer-type"], "nlb")
                self.assertEqual(annotations["service.beta.kubernetes.io/aws-load-balancer-internal"], "false")
                self.assertNotIn("service.beta.kubernetes.io/aws-load-balancer-nlb-target-type", annotations)
                self.assertEqual(service["spec"].get("externalTrafficPolicy", "Cluster"), "Cluster")
                self.assertEqual(service["spec"]["ports"][0]["port"], 443 if cert else 80)
                config = docs[1]["data"]["nginx.conf"]
                self.assertNotIn("${", config)
                self.assertIn("$request_uri", config)
                self.assertIn("proxy_next_upstream off;", config)
                self.assertIn("proxy_buffering off;", config)
                self.assertEqual(service["spec"]["selector"], docs[2]["spec"]["template"]["metadata"]["labels"])
        self.assertNotEqual(module.render("10.100.0.10")[2]["spec"]["template"]["metadata"]["annotations"], module.render("10.100.0.11")[2]["spec"]["template"]["metadata"]["annotations"])
        with self.assertRaises(ValueError):
            module.render("127.0.0.1; injected")
        with self.assertRaises(ValueError):
            module.render("127.0.0.1", "invalid")

    def test_route_boundaries(self):
        config = module.render("127.0.0.11")[1]["data"]["nginx.conf"]
        rules = re.findall(r"~(\S+) (\S+);", config)
        default = re.search(r"default (\S+);", config)[1]
        cases = {"/api": "springboot", "/api/auth/login": "springboot", "/rotas": "redis-api", "/rotas/123": "redis-api", "/rotas/123/paradas": "redis-api", "/rotas/otimizar": "ia-api", "/rotas/retreinar": "ia-api", "/rotas/otimizarXYZ": "redis-api", "/rotas/otimizar/123": "redis-api", "/apiXYZ": "ia-api", "/rotasXYZ": "ia-api", "/chat": "ia-api", "/monitor.html": "ia-api", "/assets/renovai.png": "ia-api", "/docs": "ia-api"}
        for path, expected in cases.items():
            target = next((host for pattern, host in rules if re.search(pattern, path)), default)
            self.assertTrue(target.startswith(expected), (path, target))


@unittest.skipUnless(os.environ.get("DOCKER_GATEWAY_TEST") == "1", "requires Docker daemon")
class ProxyRuntimeTest(unittest.TestCase):
    def test_real_nginx_preserves_requests_and_isolates_failure(self):
        network = "gateway-test-" + uuid.uuid4().hex[:10]
        containers = []
        def docker(*args):
            return subprocess.check_output(["docker", *args], text=True).strip()
        docker("network", "create", network)
        try:
            mock = """import json,os
from http.server import BaseHTTPRequestHandler,HTTPServer
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  self.send_response(200);self.end_headers();self.wfile.write(json.dumps({'backend':os.environ['BACKEND'],'path':self.path,'authorization':self.headers.get('Authorization'),'proto':self.headers.get('X-Forwarded-Proto')}).encode())
 do_POST=do_GET
HTTPServer(('0.0.0.0',80),Handler).serve_forever()
"""
            for backend in ("springboot-service", "redis-api-service", "ia-api-service"):
                containers.append(docker("run", "-d", "--rm", "--network", network, "--network-alias", backend + ".renovai-api.svc.cluster.local", "-e", "BACKEND=" + backend, "python:3.12-alpine", "python", "-u", "-c", mock))
            with tempfile.TemporaryDirectory() as directory:
                config = Path(directory) / "nginx.conf"
                config.write_text(module.render("127.0.0.11")[1]["data"]["nginx.conf"], encoding="utf-8")
                containers.append(docker("run", "-d", "--rm", "--network", network, "-p", "127.0.0.1::8080", "--user", "101:101", "--read-only", "--tmpfs", "/tmp:uid=101,gid=101", "-v", str(config) + ":/etc/nginx/nginx.conf:ro", "nginx:stable-alpine", "nginx", "-g", "daemon off;"))
                port = docker("port", containers[-1], "8080/tcp").rsplit(":", 1)[1]
                base = "http://127.0.0.1:" + port
                for attempt in range(50):
                    try:
                        with urlopen(base + "/gateway-health", timeout=2) as response:
                            self.assertEqual(response.status, 200)
                        break
                    except (URLError, ConnectionError):
                        if attempt == 49:
                            raise
                        time.sleep(0.2)
                for path, target in (("/api/auth/login?next=%2Fhome", "springboot-service"), ("/rotas/123?x=1", "redis-api-service"), ("/chat", "ia-api-service"), ("/apiXYZ", "ia-api-service"), ("/rotasXYZ", "ia-api-service")):
                    request = Request(base + path, data=b'{}', headers={"Authorization": "Bearer test", "Content-Type": "application/json"})
                    with urlopen(request, timeout=5) as response:
                        data = json.load(response)
                    self.assertEqual(data["backend"], target)
                    self.assertEqual(data["path"], path)
                    self.assertEqual(data["authorization"], "Bearer test")
                    self.assertEqual(data["proto"], "http")
                docker("rm", "-f", containers[2])
                with self.assertRaises(HTTPError) as error:
                    urlopen(base + "/chat", timeout=10)
                # Backend removido pode recusar conexão (502) ou expirar no IP em cache (504).
                self.assertIn(error.exception.code, (502, 504))
                with urlopen(base + "/api/actuator/health", timeout=5) as response:
                    self.assertEqual(json.load(response)["backend"], "springboot-service")
        finally:
            for container in reversed(containers):
                subprocess.run(["docker", "rm", "-f", container], capture_output=True)
            subprocess.run(["docker", "network", "rm", network], capture_output=True)


if __name__ == "__main__":
    unittest.main()
