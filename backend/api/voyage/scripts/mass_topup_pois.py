#!/usr/bin/env python3
"""
mass_topup_pois.py — reforça TODOS os destinos do catalogo (ou uma lista
especifica, via --only) até atingirem um numero-alvo de POIs, chamando
repetidamente o mesmo endpoint admin de top-up já usado por add_places.py
(POST /api/v1/admin/destinations/{slug}/pois/generate, sem focusCategory).

Cada chamada gera ~20 POIs novos (prompt fixo em LlmService.generatePois) e
NUNCA repete titulos ja cadastrados (AdminService.generatePoisForExistingDestination
busca os titulos atuais e manda pro LLM como lista de exclusao — ver fix
aplicado em 2026-10-17 que corrigiu o caso sem focusCategory, que antes
nao mandava essa lista e gerava duplicatas).

NAO FAZ chamadas diretas a OpenAI nem ao banco: so chama HTTP os endpoints
admin (escrita) e publicos (leitura de contagem) do backend Spring Boot já
no ar. Por isso o backend precisa estar rodando (local, por padrao
http://localhost:8080) e configurado com OPENAI_API_KEY e ADMIN_TOKEN
validos no .env — mesma exigencia de add_places.py.

Como rodar
----------
    python3 backend/api/voyage/scripts/mass_topup_pois.py --target 60

Flags uteis:
  --target N        Numero-alvo de POIs por destino (default: 60, o maior
                     valor já atingido no catalogo hoje — Paris).
  --only slug1,slug2  So processa esses slugs (default: todos os destinos
                     ativos retornados por GET /api/v1/destinations).
  --max-rounds N    Trava de seguranca: no maximo N chamadas de top-up por
                     destino, mesmo que ainda esteja abaixo do alvo depois
                     disso (default: 5 — suficiente pra ir de 0 a 100 POIs).
  --dry-run         So mostra quantas rodadas cada destino precisaria, sem
                     chamar a API de escrita.
  --backend-url, --env-file, --timeout  Mesmo significado de add_places.py.

O script NUNCA diminui a contagem de ninguem (o endpoint so adiciona POIs,
nunca remove) — destinos que já estao no alvo ou acima (ex: Paris, hoje em
60) sao pulados.
"""

from __future__ import annotations

import argparse
import sys
import time
import urllib.parse
from pathlib import Path
from typing import Any

try:
    import requests
except ImportError:
    print("Erro: biblioteca 'requests' nao encontrada. Instale com: pip3 install requests",
          file=sys.stderr)
    sys.exit(1)


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_ENV_FILE = SCRIPT_DIR.parent / ".env"
DEFAULT_BACKEND_URL = "http://localhost:8080"
DEFAULT_TARGET = 60
DEFAULT_MAX_ROUNDS = 5


def load_admin_token(env_path: Path) -> str:
    if not env_path.is_file():
        raise FileNotFoundError(f".env nao encontrado em: {env_path}")
    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        if key == "ADMIN_TOKEN":
            if not value or value == "<admin-token>":
                break
            return value
    raise ValueError(f"ADMIN_TOKEN nao configurado (ou vazio) em {env_path}.")


class AdminApiError(Exception):
    def __init__(self, status_code: int, message: str):
        super().__init__(f"HTTP {status_code}: {message}")
        self.status_code = status_code
        self.message = message


class LuvoyClient:
    def __init__(self, base_url: str, admin_token: str, timeout: int):
        self.base_url = base_url.rstrip("/")
        self.admin_token = admin_token
        self.timeout = timeout
        self.session = requests.Session()

    def _get(self, path: str) -> Any:
        resp = self.session.get(f"{self.base_url}{path}", timeout=self.timeout)
        resp.raise_for_status()
        return resp.json()

    def list_destinations(self) -> list[dict]:
        data = self._get("/api/v1/destinations")
        return data if isinstance(data, list) else data.get("content", data)

    def poi_count(self, slug: str) -> int:
        data = self._get(f"/api/v1/destinations/{urllib.parse.quote(slug)}")
        pois = data.get("pois", data.get("poiList", []))
        return len(pois)

    def topup_pois(self, slug: str) -> int:
        url = f"{self.base_url}/api/v1/admin/destinations/{urllib.parse.quote(slug)}/pois/generate"
        headers = {"X-Admin-Token": self.admin_token, "Content-Type": "application/json"}
        try:
            resp = self.session.post(url, headers=headers, timeout=self.timeout)
        except requests.exceptions.ConnectionError as e:
            raise AdminApiError(0, f"Nao foi possivel conectar em {url} ({e})") from e
        except requests.exceptions.Timeout as e:
            raise AdminApiError(0, f"Timeout chamando {url} (> {self.timeout}s): {e}") from e
        if resp.status_code >= 400:
            try:
                body = resp.json()
                msg = f"{body.get('error', 'ERROR')}: {body.get('message')}" if isinstance(body, dict) else resp.text
            except ValueError:
                msg = resp.text.strip() or f"HTTP {resp.status_code} sem corpo de erro."
            raise AdminApiError(resp.status_code, msg)
        return resp.json().get("poisGenerated", 0)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--target", type=int, default=DEFAULT_TARGET)
    parser.add_argument("--only", type=str, default=None, help="Lista de slugs separados por virgula.")
    parser.add_argument("--max-rounds", type=int, default=DEFAULT_MAX_ROUNDS)
    parser.add_argument("--backend-url", default=DEFAULT_BACKEND_URL)
    parser.add_argument("--env-file", type=Path, default=DEFAULT_ENV_FILE)
    parser.add_argument("--timeout", type=int, default=180)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    admin_token = "dry-run-no-token"
    if not args.dry_run:
        try:
            admin_token = load_admin_token(args.env_file)
        except (FileNotFoundError, ValueError) as e:
            print(f"Erro lendo ADMIN_TOKEN: {e}", file=sys.stderr)
            return 1

    client = LuvoyClient(args.backend_url, admin_token, args.timeout)

    try:
        destinations = client.list_destinations()
    except requests.exceptions.RequestException as e:
        print(f"Erro ao listar destinos em {args.backend_url}: {e}", file=sys.stderr)
        return 1

    slugs = [d["slug"] for d in destinations]
    if args.only:
        wanted = {s.strip() for s in args.only.split(",") if s.strip()}
        slugs = [s for s in slugs if s in wanted]

    print(f"Backend: {args.backend_url}")
    print(f"Alvo: {args.target} POIs por destino | Destinos a avaliar: {len(slugs)}"
          f"{' (dry-run)' if args.dry_run else ''}")
    print("-" * 60)

    total_rounds_run = 0
    total_pois_generated = 0
    skipped_already_ok = 0
    failures: list[tuple[str, str]] = []

    for idx, slug in enumerate(slugs, start=1):
        try:
            current = client.poi_count(slug)
        except requests.exceptions.RequestException as e:
            print(f"[{idx}/{len(slugs)}] {slug}: erro ao consultar contagem atual ({e}) — pulando")
            failures.append((slug, f"erro ao consultar contagem: {e}"))
            continue

        if current >= args.target:
            skipped_already_ok += 1
            print(f"[{idx}/{len(slugs)}] {slug}: já tem {current} POIs (>= alvo {args.target}) — pulando")
            continue

        rounds_needed = -(-(args.target - current) // 20)  # ceil division, ~20 POIs por rodada
        rounds_needed = min(rounds_needed, args.max_rounds)
        print(f"[{idx}/{len(slugs)}] {slug}: {current} POIs atuais, faltam ~{args.target - current} "
              f"-> {rounds_needed} rodada(s)" + (" [DRY-RUN]" if args.dry_run else ""))

        if args.dry_run:
            total_rounds_run += rounds_needed
            continue

        for round_num in range(1, rounds_needed + 1):
            try:
                generated = client.topup_pois(slug)
                total_pois_generated += generated
                total_rounds_run += 1
                print(f"    rodada {round_num}/{rounds_needed}: +{generated} POIs")
            except AdminApiError as e:
                print(f"    rodada {round_num}/{rounds_needed} falhou: {e.message}", file=sys.stderr)
                failures.append((slug, e.message))
                break
            time.sleep(1)  # pequena pausa entre chamadas para nao martelar o rate limit da OpenAI

    print("=" * 60)
    print("RESUMO")
    print("=" * 60)
    print(f"Destinos avaliados: {len(slugs)}")
    print(f"Ja estavam no alvo (pulados): {skipped_already_ok}")
    print(f"Rodadas de geracao executadas: {total_rounds_run}")
    print(f"Total de POIs gerados: {total_pois_generated}")
    print(f"Falhas: {len(failures)}")
    for slug, msg in failures:
        print(f"  - {slug}: {msg}")

    return 1 if failures and not args.dry_run else 0


if __name__ == "__main__":
    sys.exit(main())
