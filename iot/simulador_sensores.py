#!/usr/bin/env python3
"""Simulador dos sensores IoT do OngSave.

Faz o papel de dois dispositivos físicos e envia as leituras em JSON para a API (POST /api/iot/leituras):

  1. Termómetro da câmara fria da ONG (Casa Esperança): mede a temperatura a cada ciclo.
     A meio da simulação, a porta "fica aberta" e a temperatura sobe acima de 8 °C -> alerta para a ONG.
  2. Balança da doca de carga da empresa (Supermercado Bom Preço): pesa o lote que está a ser preparado
     para a retirada. Uma das pesagens vem 20 % abaixo do peso declarado -> alerta para a empresa e a ONG.

Só usa a biblioteca padrão do Python 3. Exemplos:
  python3 iot/simulador_sensores.py
  python3 iot/simulador_sensores.py --leituras 60 --intervalo 2
  python3 iot/simulador_sensores.py --sem-alertas
"""
import argparse
import json
import random
import sys
import time
import urllib.error
import urllib.request


def pedido(base, chave, metodo, caminho, corpo=None):
    dados = None if corpo is None else json.dumps(corpo).encode("utf-8")
    req = urllib.request.Request(base + caminho, data=dados, method=metodo,
                                 headers={"X-Sensor-Key": chave, "Content-Type": "application/json", "Accept": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except ValueError:
            return e.code, {"erro": e.reason}


def main():
    ap = argparse.ArgumentParser(description="Simula os sensores IoT do OngSave.")
    ap.add_argument("--url", default="http://localhost:8080/ongsave", help="endereço do site (padrão: %(default)s)")
    ap.add_argument("--chave", default="ongsave-iot-demo", help="valor de iot.chave (padrão da demonstração)")
    ap.add_argument("--leituras", type=int, default=30, help="ciclos de leitura (padrão: %(default)s)")
    ap.add_argument("--intervalo", type=float, default=3, help="segundos entre ciclos (padrão: %(default)s)")
    ap.add_argument("--sem-alertas", action="store_true", help="só leituras normais, sem provocar alertas")
    a = ap.parse_args()
    base = a.url.rstrip("/") + "/api/iot"

    try:
        st, sensores = pedido(base, a.chave, "GET", "/sensores")
    except urllib.error.URLError as e:
        sys.exit(f"O site não respondeu em {a.url} ({e.reason}). O Tomcat está ligado?")
    if st != 200:
        sys.exit(f"Erro {st} ao listar os sensores: {sensores.get('erro')}")

    termo = next((s for s in sensores if s["tipo"] == "temperatura"), None)
    balanca = next((s for s in sensores if s["tipo"] == "peso"), None)
    print("Sensores encontrados:")
    for s in sensores:
        extra = f" · lote em pesagem #{s['loteId']} ({s['loteKg']} kg declarados)" if s.get("loteId") else ""
        print(f"  - {s['id']} ({s['tipo']}, {s['unidade']}){extra}")
    if not termo and not balanca:
        sys.exit("Nenhum sensor cadastrado. Reinicie o Tomcat para aplicar a migração V005.")
    print()

    temp = 4.5
    pico_ini = a.leituras // 3                 # a porta abre a um terço da simulação...
    pico_fim = pico_ini + 6                    # ...e fecha seis ciclos depois
    divergente = (2 * a.leituras) // 3         # pesagem com divergência a dois terços

    for i in range(1, a.leituras + 1):
        if termo:
            if not a.sem_alertas and pico_ini <= i < pico_fim:
                temp += random.uniform(0.9, 1.4)                 # porta aberta: aquece rápido
            else:
                temp += (4.5 - temp) * 0.35 + random.uniform(-0.25, 0.25)   # compressor puxa para ~4,5 °C
            st, r = pedido(base, a.chave, "POST", "/leituras", {"sensor": termo["id"], "valor": round(temp, 1)})
            mostrar(i, termo, st, r)

        if balanca and balanca.get("loteId") and i % 5 == 0:
            kg = float(balanca["loteKg"])
            fator = 0.80 if (not a.sem_alertas and i >= divergente and i - 5 < divergente) else random.uniform(0.98, 1.02)
            corpo = {"sensor": balanca["id"], "valor": round(kg * fator, 1), "lote": balanca["loteId"]}
            st, r = pedido(base, a.chave, "POST", "/leituras", corpo)
            mostrar(i, balanca, st, r)

        time.sleep(a.intervalo)

    print("\nSimulação terminada. Veja os alertas no sino de notificações dos painéis da ONG e da empresa.")


def mostrar(i, sensor, st, r):
    if st != 200:
        print(f"[{i:3}] {sensor['id']}: erro {st} - {r.get('erro')}")
        return
    sinal = "⚠ ALERTA" if r["alerta"] else "ok"
    aviso = " (notificação enviada)" if r.get("notificado") else ""
    lote = f" lote #{r['lote']}" if r.get("lote") else ""
    print(f"[{i:3}] {sensor['id']}: {r['valor']} {sensor['unidade']}{lote} -> {sinal}{aviso}")
    if r["alerta"] and r.get("motivo"):
        print(f"        {r['motivo']}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrompido.")
