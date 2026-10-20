#!/usr/bin/env python3
"""
add_places.py — script administrativo para adicionar destinos e/ou POIs ao
catalogo da Luvoy usando a infraestrutura de geracao via LLM (GPT-4o) que ja
existe no backend (endpoints /api/v1/admin/* em AdminController.java).

NAO FAZ chamadas diretas a OpenAI nem ao banco: ele so chama HTTP os
endpoints admin do backend Spring Boot. Por isso o backend precisa estar
rodando (local, por padrao http://localhost:8080) e configurado com
OPENAI_API_KEY e ADMIN_TOKEN validos no .env.

Como rodar
----------
1) Suba o backend local (NUNCA aponte isso pro banco de producao/Supabase
   remoto sem saber o que esta fazendo):

       cd backend/api/voyage
       JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw spring-boot:run

2) Monte um arquivo JSON com os lugares a adicionar (veja PLACES_FILE_FORMAT
   abaixo). Exemplo em scripts/places.example.json.

3) Rode o script (a partir de qualquer diretorio):

       python3 backend/api/voyage/scripts/add_places.py --input scripts/places.example.json

   Flags uteis:
     --backend-url   URL do backend (default: http://localhost:8080, ou a
                      env var LUVOY_BACKEND_URL)
     --env-file       Caminho do .env do backend (default: .env ao lado da
                      pasta scripts/, ou seja backend/api/voyage/.env)
     --dry-run        Monta os payloads e mostra o que seria enviado, sem
                      fazer nenhuma chamada HTTP (util pra validar o arquivo
                      de entrada e testar a sintaxe do script sem backend
                      no ar)
     --batch-missing  Alem de processar o arquivo de entrada, chama no final
                      POST /pois/generate-batch (gera POIs pra TODOS os
                      destinos que ainda tem zero POIs no banco)

O script le o ADMIN_TOKEN automaticamente do .env do backend (variavel
ADMIN_TOKEN) — nunca precisa ser colado na linha de comando nem hardcoded
aqui.

PLACES_FILE_FORMAT (JSON)
--------------------------
Uma lista de objetos. Cada objeto representa UMA acao:

    [
      {
        "action": "destination",
        "city": "Orlando",
        "country": "Estados Unidos",
        "slug": "orlando"
      },
      {
        "action": "destination_and_pois",
        "city": "Orlando",
        "country": "Estados Unidos",
        "slug": "orlando"
      },
      {
        "action": "pois",
        "slug": "paris",
        "focusCategory": "LANDMARK"
      }
    ]

Campos:
  action          "destination"          -> so cria o destino (POST
                                             /destinations/generate)
                  "pois"                 -> so gera POIs novos pra um
                                             destino JA EXISTENTE (POST
                                             /destinations/{slug}/pois/generate)
                  "destination_and_pois" -> cria o destino e, se der certo,
                                             já encadeia a geracao de POIs
                                             pra ele (bom caso de teste: um
                                             destino novo tipo Orlando com
                                             POI fullDayActivity=true, ex.
                                             Walt Disney World)
                  Se "action" for omitido, o script infere: se vier
                  "city"+"country" -> "destination"; senao -> "pois".
  city, country   obrigatorios para "destination"/"destination_and_pois".
  slug            sempre obrigatorio (desejado, no caso de destino novo; do
                  destino ja existente, no caso de so gerar POIs).
  focusCategory   opcional, so usado em "pois"/"destination_and_pois" pra
                  focar a geracao numa categoria especifica (ex: RESTAURANT,
                  LANDMARK, NIGHTLIFE — mesmos valores do enum PoiCategory
                  no backend). Se invalido, o backend responde 400 e o
                  script reporta o erro e segue pro proximo item.

Sobre o campo fullDayActivity (parques tematicos de dia inteiro)
-----------------------------------------------------------------
O prompt do GPT-4o em LlmService.generatePois ja instrui a marcar
fullDayActivity=true SOMENTE pra parques tematicos de dia inteiro de
verdade (Disneyland, Universal Studios, Europa-Park etc.), e
AdminService.mapToPoi ja mapeia esse campo pra entidade Poi ao salvar. Este
script NAO faz nenhum parsing manual da resposta do LLM nem monta POIs por
conta propria — ele so chama os endpoints admin, que ja preservam o campo
de ponta a ponta. Nada a fazer aqui alem de nao quebrar esse fluxo.

IMPORTANTE: os endpoints admin de geracao retornam apenas CONTADORES
(PoiTopUpReport.poisGenerated, BatchGenerationReport.totalPoisCreated) —
eles NAO retornam a lista dos POIs criados nem o valor de fullDayActivity
de cada um. O endpoint publico GET /api/v1/destinations/{slug} tambem nao
ajuda: o DTO que ele usa pra listar POIs (PoiSummaryDTO) nao inclui
fullDayActivity. Ou seja, via HTTP nao da pra saber quantos dos POIs
gerados vieram com fullDayActivity=true — isso so e visivel direto no
banco. O resumo final deste script documenta essa limitacao explicitamente
em vez de inventar um numero.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
import urllib.parse
from dataclasses import dataclass, field
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
DEFAULT_BACKEND_URL = os.environ.get("LUVOY_BACKEND_URL", "http://localhost:8080")
VALID_ACTIONS = {"destination", "pois", "destination_and_pois"}


# ---------------------------------------------------------------------------
# .env parsing (sem dependencia de python-dotenv, so o essencial: KEY=VALUE,
# comentarios com #, aspas simples/duplas opcionais ao redor do valor)
# ---------------------------------------------------------------------------

def load_env_file(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise FileNotFoundError(f".env nao encontrado em: {path}")

    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        values[key] = value
    return values


def load_admin_token(env_path: Path) -> str:
    env_values = load_env_file(env_path)
    token = env_values.get("ADMIN_TOKEN")
    if not token or token.strip() == "" or token == "<admin-token>":
        raise ValueError(
            f"ADMIN_TOKEN nao configurado (ou vazio) em {env_path}. "
            "Configure a variavel ADMIN_TOKEN no .env do backend."
        )
    return token


# ---------------------------------------------------------------------------
# Input file parsing
# ---------------------------------------------------------------------------

@dataclass
class PlaceItem:
    action: str
    city: str | None = None
    country: str | None = None
    slug: str | None = None
    focus_category: str | None = None
    raw: dict[str, Any] = field(default_factory=dict)


def infer_action(item: dict[str, Any]) -> str:
    if item.get("action"):
        return str(item["action"]).strip()
    if item.get("city") and item.get("country"):
        return "destination"
    return "pois"


def load_places(input_path: Path) -> list[PlaceItem]:
    if not input_path.is_file():
        raise FileNotFoundError(f"Arquivo de entrada nao encontrado: {input_path}")

    raw_text = input_path.read_text(encoding="utf-8")
    try:
        data = json.loads(raw_text)
    except json.JSONDecodeError as e:
        raise ValueError(f"JSON invalido em {input_path}: {e}") from e

    if not isinstance(data, list):
        raise ValueError("O arquivo de entrada deve conter uma lista JSON (ex: [ {...}, {...} ]).")

    items: list[PlaceItem] = []
    for idx, raw_item in enumerate(data):
        if not isinstance(raw_item, dict):
            raise ValueError(f"Item #{idx} do arquivo de entrada nao e um objeto JSON.")

        action = infer_action(raw_item)
        if action not in VALID_ACTIONS:
            raise ValueError(
                f"Item #{idx}: action '{action}' invalida. Use um de: {sorted(VALID_ACTIONS)}."
            )

        item = PlaceItem(
            action=action,
            city=raw_item.get("city"),
            country=raw_item.get("country"),
            slug=raw_item.get("slug"),
            focus_category=raw_item.get("focusCategory"),
            raw=raw_item,
        )

        if not item.slug:
            raise ValueError(f"Item #{idx}: campo 'slug' e obrigatorio em todas as acoes.")
        if action in ("destination", "destination_and_pois") and not (item.city and item.country):
            raise ValueError(
                f"Item #{idx} (action={action}): campos 'city' e 'country' sao obrigatorios."
            )

        items.append(item)

    return items


# ---------------------------------------------------------------------------
# Cliente HTTP para os endpoints admin
# ---------------------------------------------------------------------------

class AdminApiError(Exception):
    """Erro vindo da API (HTTP nao-2xx), com mensagem ja extraida do corpo."""

    def __init__(self, status_code: int, message: str):
        super().__init__(f"HTTP {status_code}: {message}")
        self.status_code = status_code
        self.message = message


class LuvoyAdminClient:
    def __init__(self, base_url: str, admin_token: str, timeout: int = 180):
        self.base_url = base_url.rstrip("/")
        self.admin_token = admin_token
        self.timeout = timeout
        self.session = requests.Session()

    def _headers(self) -> dict[str, str]:
        return {"X-Admin-Token": self.admin_token, "Content-Type": "application/json"}

    def _request(self, method: str, path: str, **kwargs) -> requests.Response:
        url = f"{self.base_url}{path}"
        try:
            resp = self.session.request(
                method, url, headers=self._headers(), timeout=self.timeout, **kwargs
            )
        except requests.exceptions.ConnectionError as e:
            raise AdminApiError(
                0, f"Nao foi possivel conectar em {url}. O backend esta rodando? ({e})"
            ) from e
        except requests.exceptions.Timeout as e:
            raise AdminApiError(0, f"Timeout chamando {url} (> {self.timeout}s): {e}") from e

        if resp.status_code >= 400:
            raise AdminApiError(resp.status_code, self._extract_error_message(resp))

        return resp

    @staticmethod
    def _extract_error_message(resp: requests.Response) -> str:
        # GlobalExceptionHandler responde com ErrorResponse(error, message, status).
        # Endpoints com @RequestHeader(required=false) + token errado/ausente
        # so retornam 403 sem corpo (ResponseEntity.build()), entao cobrimos
        # os dois casos.
        try:
            body = resp.json()
            if isinstance(body, dict) and "message" in body:
                return f"{body.get('error', 'ERROR')}: {body['message']}"
        except ValueError:
            pass
        if resp.status_code == 403:
            return "403 Forbidden — X-Admin-Token ausente ou incorreto (confira ADMIN_TOKEN no .env)."
        return resp.text.strip() or f"HTTP {resp.status_code} sem corpo de erro."

    def generate_destination(self, city: str, country: str, slug: str) -> dict[str, Any]:
        resp = self._request(
            "POST",
            "/api/v1/admin/destinations/generate",
            json={"city": city, "country": country, "slug": slug},
        )
        return resp.json()

    def generate_pois_for_destination(self, slug: str, focus_category: str | None) -> dict[str, Any]:
        path = f"/api/v1/admin/destinations/{urllib.parse.quote(slug)}/pois/generate"
        params = {"focusCategory": focus_category} if focus_category else None
        resp = self._request("POST", path, params=params)
        return resp.json()

    def generate_pois_batch_for_missing(self) -> dict[str, Any]:
        resp = self._request("POST", "/api/v1/admin/pois/generate-batch")
        return resp.json()


# ---------------------------------------------------------------------------
# Processamento item a item (nunca trava o lote inteiro por causa de 1 erro)
# ---------------------------------------------------------------------------

@dataclass
class ItemResult:
    item: PlaceItem
    ok: bool
    detail: str
    pois_generated: int = 0
    destination_created: bool = False


def process_item(client: LuvoyAdminClient, item: PlaceItem, dry_run: bool) -> ItemResult:
    label = f"{item.action} slug={item.slug}" + (f" city={item.city}" if item.city else "")

    if dry_run:
        payload_preview = {
            "action": item.action,
            "slug": item.slug,
            "city": item.city,
            "country": item.country,
            "focusCategory": item.focus_category,
        }
        print(f"[DRY-RUN] {label} -> payload: {json.dumps(payload_preview, ensure_ascii=False)}")
        return ItemResult(item, ok=True, detail="dry-run (nenhuma chamada HTTP feita)")

    destination_created = False
    pois_generated = 0

    try:
        if item.action in ("destination", "destination_and_pois"):
            result = client.generate_destination(item.city, item.country, item.slug)
            destination_created = True
            print(f"  ✓ destino criado: {result.get('slug')} ({result.get('name')})")

        if item.action in ("pois", "destination_and_pois"):
            result = client.generate_pois_for_destination(item.slug, item.focus_category)
            pois_generated = result.get("poisGenerated", 0)
            print(f"  ✓ POIs gerados para {item.slug}: {pois_generated}")

        return ItemResult(
            item, ok=True,
            detail="sucesso",
            pois_generated=pois_generated,
            destination_created=destination_created,
        )

    except AdminApiError as e:
        detail = e.message
        if destination_created:
            detail = f"destino criado com sucesso, mas geracao de POIs falhou: {e.message}"
        print(f"  ✗ falhou ({label}): {detail}", file=sys.stderr)
        return ItemResult(
            item, ok=False, detail=detail,
            pois_generated=pois_generated, destination_created=destination_created,
        )


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Adiciona destinos/POIs ao catalogo Luvoy via endpoints admin (geracao por LLM)."
    )
    parser.add_argument("--input", required=True, type=Path, help="Arquivo JSON com os lugares a adicionar.")
    parser.add_argument("--backend-url", default=DEFAULT_BACKEND_URL,
                         help=f"URL base do backend (default: {DEFAULT_BACKEND_URL}).")
    parser.add_argument("--env-file", default=DEFAULT_ENV_FILE, type=Path,
                         help=f"Caminho do .env do backend (default: {DEFAULT_ENV_FILE}).")
    parser.add_argument("--dry-run", action="store_true",
                         help="So monta os payloads e mostra o que seria enviado, sem chamar a API.")
    parser.add_argument("--batch-missing", action="store_true",
                         help="Ao final, tambem chama /pois/generate-batch (gera POIs pra destinos com zero POIs).")
    parser.add_argument("--timeout", type=int, default=180,
                         help="Timeout (segundos) por chamada HTTP — geracao via GPT-4o pode demorar.")
    args = parser.parse_args()

    try:
        items = load_places(args.input)
    except (FileNotFoundError, ValueError) as e:
        print(f"Erro lendo arquivo de entrada: {e}", file=sys.stderr)
        return 1

    if not items and not args.batch_missing:
        print("Arquivo de entrada nao tem nenhum item (e --batch-missing nao foi passado). Nada a fazer.")
        return 0

    admin_token = None
    if not args.dry_run:
        try:
            admin_token = load_admin_token(args.env_file)
        except (FileNotFoundError, ValueError) as e:
            print(f"Erro lendo ADMIN_TOKEN: {e}", file=sys.stderr)
            return 1

    client = LuvoyAdminClient(args.backend_url, admin_token or "dry-run-no-token", timeout=args.timeout)

    print(f"Backend: {args.backend_url}")
    print(f"Itens a processar: {len(items)}{' (dry-run)' if args.dry_run else ''}")
    print("-" * 60)

    results: list[ItemResult] = []
    for idx, item in enumerate(items, start=1):
        print(f"[{idx}/{len(items)}] {item.action} — slug={item.slug}")
        results.append(process_item(client, item, args.dry_run))

    batch_report = None
    if args.batch_missing and not args.dry_run:
        print("-" * 60)
        print("Rodando /pois/generate-batch para destinos com zero POIs...")
        try:
            batch_report = client.generate_pois_batch_for_missing()
            print(f"  ✓ batch: {batch_report.get('destinationsProcessed')} destinos processados, "
                  f"{batch_report.get('totalPoisCreated')} POIs criados "
                  f"(pulados: {batch_report.get('destinationsSkipped')})")
        except AdminApiError as e:
            print(f"  ✗ batch falhou: {e.message}", file=sys.stderr)

    # ---------------------------------------------------------------
    # Resumo final
    # ---------------------------------------------------------------
    print("=" * 60)
    print("RESUMO")
    print("=" * 60)

    # destination_created e verdadeiro assim que a chamada de criacao do
    # destino tem sucesso, mesmo que a geracao de POIs encadeada (no caso de
    # action=destination_and_pois) falhe depois — por isso NAO filtramos por
    # r.ok aqui (um item pode terminar "com falha" no geral mas ainda assim
    # ter criado o destino com sucesso).
    destinations_created = sum(1 for r in results if r.destination_created)
    destinations_attempted = sum(1 for r in results if r.item.action in ("destination", "destination_and_pois"))
    total_pois_generated = sum(r.pois_generated for r in results if r.ok)
    failed = [r for r in results if not r.ok]

    print(f"Destinos novos criados: {destinations_created}/{destinations_attempted}")
    print(f"Total de POIs gerados (soma de todos os itens com sucesso): {total_pois_generated}")
    if batch_report:
        print(f"POIs gerados via --batch-missing: {batch_report.get('totalPoisCreated', 0)}")
    print(f"Itens com falha: {len(failed)}/{len(results)}")
    for r in failed:
        print(f"  - {r.item.action} slug={r.item.slug}: {r.detail}")

    print()
    print("Nota sobre fullDayActivity: os endpoints admin de geracao retornam so")
    print("contadores (poisGenerated/totalPoisCreated), e o endpoint publico de")
    print("detalhe do destino (PoiSummaryDTO) tambem nao inclui esse campo na")
    print("resposta. Por isso NAO e possivel saber via HTTP quantos POIs vieram")
    print("marcados como fullDayActivity=true — o prompt do LlmService.generatePois")
    print("ja cuida de marcar isso corretamente nos parques tematicos de dia")
    print("inteiro, mas a confirmacao so e visivel direto no banco (coluna")
    print("full_day_activity da tabela pois).")

    return 1 if failed and not args.dry_run else 0


if __name__ == "__main__":
    sys.exit(main())
